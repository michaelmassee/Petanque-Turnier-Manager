package de.petanqueturniermanager.addins;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XCellRangeData;
import com.sun.star.sheet.XCellRangeFormula;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.Any;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addin.XGlobal;
import de.petanqueturniermanager.algorithmen.common.CadrageRechner;
import de.petanqueturniermanager.algorithmen.liga.Direktvergleich;
import de.petanqueturniermanager.algorithmen.poule.PouleGruppenRechner;
import de.petanqueturniermanager.basesheet.meldeliste.NrZellFormel;
import de.petanqueturniermanager.basesheet.meldeliste.TeamAnzeigeFormatierer;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.DokumentKontext;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.planungsrechner.PlanungsrechnerRechner;
import de.petanqueturniermanager.planungsrechner.PlanungsrechnerRechner.ZeitplanEintrag;
import de.petanqueturniermanager.spielerdb.SpielerAddInCache;
import de.petanqueturniermanager.spielerdb.SpielerDbConnection;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

public final class GlobalImpl extends AbstractAddInImpl implements XGlobal {
	static final Logger logger = LogManager.getLogger(GlobalImpl.class);

	private final XComponentContext xContext;
	private static final String implName = GlobalImpl.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.addin.GlobalAddIn";
	// WICHTIG: Beide Services müssen registriert sein!
	private static final String[] serviceNames = { 
		SERVICE_NAME,
		"com.sun.star.sheet.AddIn"  // Standard AddIn Service
	};

	private static final AtomicBoolean isDirty = new AtomicBoolean(false);

	// DisplayNames aus GlobalAddIn.xcu - diese werden für Formeln in Calc verwendet
	// =PTM.ALG.INTPROPERTY("propertyname")
	// Hinweis: Das "=" wird automatisch von setFormulaInCell() hinzugefügt
	public static final String PTM_INT_PROPERTY = "PTM.ALG.INTPROPERTY";
	public static final String PTM_STRING_PROPERTY = "PTM.ALG.STRINGPROPERTY";
	public static final String PTM_BOOLEAN_PROPERTY = "PTM.ALG.BOOLEANPROPERTY";
	public static final String PTM_DIREKTVERGLEICH = "PTM.ALG.DIRECTCOMPARISON";
	public static final String PTM_TURNIERSYSTEM = "PTM.ALG.TURNIERSYSTEM";
	public static final String PTM_TEAM_ANZEIGE = "PTM.ALG.TEAMANZEIGE";
	public static final String PTM_UUID_TEAM_ANZEIGE = "PTM.ALG.UUID.TEAMANZEIGE";
	public static final String PTM_TNR_UUID = "PTM.ALG.TNRUUID";
	public static final String PTM_UUID_ZEILE = "PTM.ALG.UUIDZEILE";
	public static final String PTM_UUID_TEAMNR = "PTM.ALG.UUIDTEAMNR";
	public static final String PTM_SPLR_UUID = "PTM.ALG.SPLRUUID";

	public static final String PTM_SUPERMELEE_TRIPL_ANZ_DOUBLETTE = "PTM.SUPERMELEE.TRIPL_ANZ_DOUBLETTE";
	public static final String PTM_SUPERMELEE_TRIPL_ANZ_TRIPLETTE = "PTM.SUPERMELEE.TRIPL_ANZ_TRIPLETTE";
	public static final String PTM_SUPERMELEE_TRIPL_NUR_DOUBLETTE = "PTM.SUPERMELEE.TRIPL_NUR_DOUBLETTE";
	public static final String PTM_SUPERMELEE_DOUBL_ANZ_DOUBLETTE = "PTM.SUPERMELEE.DOUBL_ANZ_DOUBLETTE";
	public static final String PTM_SUPERMELEE_DOUBL_ANZ_TRIPLETTE = "PTM.SUPERMELEE.DOUBL_ANZ_TRIPLETTE";
	public static final String PTM_SUPERMELEE_DOUBL_NUR_TRIPLETTE = "PTM.SUPERMELEE.DOUBL_NUR_TRIPLETTE";
	public static final String PTM_SUPERMELEE_TRIPL_ANZ_PAARUNGEN = "PTM.SUPERMELEE.TRIPL_ANZ_PAARUNGEN";
	public static final String PTM_SUPERMELEE_TRIPL_ANZ_BAHNEN = "PTM.SUPERMELEE.TRIPL_ANZ_BAHNEN";
	public static final String PTM_SUPERMELEE_DOUBL_ANZ_PAARUNGEN = "PTM.SUPERMELEE.DOUBL_ANZ_PAARUNGEN";
	public static final String PTM_SUPERMELEE_DOUBL_ANZ_BAHNEN = "PTM.SUPERMELEE.DOUBL_ANZ_BAHNEN";
	public static final String PTM_SUPERMELEE_VALIDE_ANZ_SPIELER = "PTM.SUPERMELEE.VALIDE_ANZ_SPIELER";
	public static final String PTM_SUPERMELEE_ANZ_TRIPLETTE_WENN_NUR_TRIPLETTE = "PTM.SUPERMELEE.ANZ_TRIPLETTE_WENN_NUR_TRIPLETTE";
	public static final String PTM_SUPERMELEE_ANZ_DOUBLETTE_WENN_NUR_DOUBLETTE = "PTM.SUPERMELEE.ANZ_DOUBLETTE_WENN_NUR_DOUBLETTE";

	public static final String PTM_POULE_ANZ_GRUPPEN        = "PTM.POULE.ANZ_GRUPPEN";
	public static final String PTM_POULE_ANZ_VIERER_GRUPPEN = "PTM.POULE.ANZ_VIERER_GRUPPEN";
	public static final String PTM_POULE_ANZ_DREIER_GRUPPEN = "PTM.POULE.ANZ_DREIER_GRUPPEN";

	public static final String PTM_DB_SPIELER_ANZAHL = "PTM.DB.SPIELER.ANZAHL";
	public static final String PTM_DB_SPIELER_SUCHE  = "PTM.DB.SPIELER.SUCHE";

	public static final String PTM_PLANUNG_DURCHGAENGE_PRO_RUNDE = "PTM.PLANUNG.DURCHGAENGE_PRO_RUNDE";
	public static final String PTM_PLANUNG_ZEITLIMIT             = "PTM.PLANUNG.ZEITLIMIT";
	public static final String PTM_PLANUNG_TURNIER_ENDE          = "PTM.PLANUNG.TURNIER_ENDE";
	public static final String PTM_PLANUNG_ZEITPLAN              = "PTM.PLANUNG.ZEITPLAN";

	private static final int SUCHE_LIMIT = 200;
	private static final int SUCHE_MAX_LEN = 100;
	private static final String[][] LEERES_SUCH_ERGEBNIS = new String[0][0];
	private static final DateTimeFormatter PLANUNG_HH_MM = DateTimeFormatter.ofPattern("HH:mm");

	private final SpielerAddInCache spielerCache = new SpielerAddInCache();

	public static final String FORMAT_PTM_INT_PROPERTY(String propName) {
		return PTM_INT_PROPERTY + "(\"" + propName + "\")";
	}

	public static final String FORMAT_PTM_STRING_PROPERTY(String propName) {
		return PTM_STRING_PROPERTY + "(\"" + propName + "\")";
	}

	public static String FORMAT_PTM_BOOLEAN_PROPERTY(String propName) {
		return PTM_BOOLEAN_PROPERTY + "(\"" + propName + "\")";
	}

	public static String FORMAT_PTM_TEAM_ANZEIGE(boolean teamnameAnzeigen, int anzSpieler,
			boolean vereinsnameAnzeigen, String meldelistenZeileFormel) {
		return PTM_TEAM_ANZEIGE + "(" + (teamnameAnzeigen ? "1" : "0") + ";" + anzSpieler
				+ ";" + (vereinsnameAnzeigen ? "1" : "0") + ";" + meldelistenZeileFormel + ")";
	}

	public static String FORMAT_PTM_UUID_TEAM_ANZEIGE(boolean teamnameAnzeigen, int anzSpieler,
			boolean vereinsnameAnzeigen, String uuidAusdruck, String bereich, boolean teamnameSpalteUeberspringen) {
		return PTM_UUID_TEAM_ANZEIGE + "(" + (teamnameAnzeigen ? "1" : "0") + ";" + anzSpieler
				+ ";" + (vereinsnameAnzeigen ? "1" : "0") + ";" + uuidAusdruck + ";" + bereich
				+ (teamnameSpalteUeberspringen ? ";1" : "") + ")";
	}

	/** Nr-Zelle der Meldeliste: zeigt {@code nr} an und trägt die UUID des Teams als Literal. */
	public static String FORMAT_PTM_TNR_UUID(int nr, String uuid) {
		return NrZellFormel.team(uuid).formel(nr);
	}

	/** Zeile der UUID im Bereich, Ersatz für {@code MATCH(nr;bereich;0)}. */
	public static String FORMAT_PTM_UUID_ZEILE(String uuidAusdruck, String bereich) {
		return PTM_UUID_ZEILE + "(" + uuidAusdruck + ";" + bereich + ")";
	}

	/** Aktuelle Teamnummer zur UUID, Ersatz für {@code INDEX(bereich;UUIDZEILE(uuid;bereich))}. */
	public static String FORMAT_PTM_UUID_TEAMNR(String uuidAusdruck, String bereich) {
		return PTM_UUID_TEAMNR + "(" + uuidAusdruck + ";" + bereich + ")";
	}

	public static String FORMAT_PTM_PLANUNG_DURCHGAENGE_PRO_RUNDE(String teamsAdresse, String bahnenAdresse) {
		return PTM_PLANUNG_DURCHGAENGE_PRO_RUNDE + "(" + teamsAdresse + ";" + bahnenAdresse + ")";
	}

	public static String FORMAT_PTM_PLANUNG_ZEITLIMIT(String startAdresse, String endeAdresse, String teamsAdresse,
			String bahnenAdresse, String rundenAdresse, String durchgangPauseAdresse, String rundenPauseAdresse) {
		return PTM_PLANUNG_ZEITLIMIT + "(" + startAdresse + ";" + endeAdresse + ";" + teamsAdresse + ";"
				+ bahnenAdresse + ";" + rundenAdresse + ";" + durchgangPauseAdresse + ";" + rundenPauseAdresse + ")";
	}

	public static String FORMAT_PTM_PLANUNG_TURNIER_ENDE(String startAdresse, String teamsAdresse,
			String bahnenAdresse, String rundenAdresse, String durchgangPauseAdresse, String rundenPauseAdresse,
			String zeitlimitAdresse) {
		return PTM_PLANUNG_TURNIER_ENDE + "(" + startAdresse + ";" + teamsAdresse + ";" + bahnenAdresse + ";"
				+ rundenAdresse + ";" + durchgangPauseAdresse + ";" + rundenPauseAdresse + ";" + zeitlimitAdresse
				+ ")";
	}

	public static String FORMAT_PTM_PLANUNG_ZEITPLAN(String startAdresse, String teamsAdresse, String bahnenAdresse,
			String rundenAdresse, String durchgangPauseAdresse, String rundenPauseAdresse, String zeitlimitAdresse) {
		return PTM_PLANUNG_ZEITPLAN + "(" + startAdresse + ";" + teamsAdresse + ";" + bahnenAdresse + ";"
				+ rundenAdresse + ";" + durchgangPauseAdresse + ";" + rundenPauseAdresse + ";" + zeitlimitAdresse
				+ ")";
	}

	// wird nur einmal aufgerufen für alle sheets
	public GlobalImpl(XComponentContext xContext) {
		this.xContext = xContext;
		GlobalImpl.isDirty.set(false);
		PetanqueTurnierMngrSingleton.init(xContext);
	}

	public static final XSingleComponentFactory __getComponentFactory(String name) {
		XSingleComponentFactory xFactory = null;
		if (name.equals(implName)) {
			xFactory = Factory.createComponentFactory(GlobalImpl.class, serviceNames);
		}
		return xFactory;
	}

	public static final boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
		return Factory.writeRegistryServiceInfo(implName, serviceNames, regKey);
	}

	// ------------------- XGlobal function(s) -----------------

	/**
	 * bei jeden call das aktive Dokument ermitteln
	 * 
	 * @return
	 */

	private DocumentPropertiesHelper getDocumentPropertiesHelper() {
		try {
			XSpreadsheetDocument doc = DokumentKontext.get();
			if (doc == null) {
				doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
			}
			if (doc != null) {
				DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(doc);
				if (hlpr.isEmpty() && hlpr.isFirstLoad()) {
					// ist dann der fall wenn das Turnier dokument als erstes neu aus dem Menue geladen wird,
					// oder das Dokument hat keine properties aber PTM Funktionen.
					if (!GlobalImpl.isDirty.getAndSet(true)) {
						logger.debug("properties isFirstLoad and isEmpty=true");
					}
					return null;
				}
				return hlpr;
			}
			// das hat nicht funktioniert
			// Beim Doc-Dispose ruft LO alle PTM-Formel-Zellen einzeln auf -> nur 1x pro Dirty-Übergang loggen
			if (!GlobalImpl.isDirty.getAndSet(true)) {
				logger.debug("XSpreadsheetDocument = null");
			}
		} catch (Exception e) {
			if (!GlobalImpl.isDirty.getAndSet(true)) {
				logger.error("getDocumentPropertiesHelper", e);
			}
		}
		return null;
	}

	@Override
	public int ptmintproperty(String propname) {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();

			if (!StringUtils.isAllBlank(propname) && turnierSystemAusDocument != TurnierSystem.KEIN) {
				Integer propVal = hlpr.getIntProperty(propname, 0);
				logger.debug("IntProperty [{}] = {}", propname, propVal);
				return propVal;
			}
		}
		return 0;
	}

	@Override
	public String ptmstringproperty(String propname) {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();

			if (!StringUtils.isAllBlank(propname) && turnierSystemAusDocument != TurnierSystem.KEIN) {
				String propVal = hlpr.getStringProperty(propname, "Property '" + propname + "' fehlt.");
				logger.debug("return:{} = {}", propname, propVal);
				return propVal;
			}
		}
		return "fehler";
	}

	@Override
	public int ptmbooleanproperty(String propname) {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();

			if (!StringUtils.isAllBlank(propname) && turnierSystemAusDocument != TurnierSystem.KEIN) {
				boolean propVal = hlpr.getBooleanProperty(propname, false);
				logger.trace("BooleanProperty [{}] = {}", propname, propVal);
				return propVal ? 1 : 0;
			}
		}
		logger.trace("BooleanProperty fallback used (TurnierSystem noch nicht gesetzt): {}", propname);
		return 0;
	}

	@Override
	public String ptmturniersystem() {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();
			return turnierSystemAusDocument.getBezeichnung();
		}
		return TurnierSystem.KEIN.getBezeichnung();
	}

	static boolean getAndSetDirty(boolean newval) {
		return GlobalImpl.isDirty.getAndSet(newval);
	}

	@Override
	public int ptmdirektvergleich(int teamA, int teamB, int[][] begegnungen, int[][] siege, int[][] spielpunkte) {
		Direktvergleich dvrgl = new Direktvergleich(teamA, teamB, begegnungen, siege, spielpunkte);
		return dvrgl.calc().getCode();
	}

	@Override
	public int ptmaktuellerunde() {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			return hlpr.getIntProperty("Spielrunde", 0);
		}
		return 0;
	}

	@Override
	public int ptmaktuellerspieltag() {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			return hlpr.getIntProperty("Spieltag", 0);
		}
		return 0;
	}

	@Override
	public int ptmoperationaktiv() {
		return SheetRunner.isRunning() ? 1 : 0;
	}

	@Override
	public String ptmteamanzeige(int teamnameAnzeigen, int anzSpieler, int vereinsnameAnzeigen,
			String[][] meldelistenZeile) {
		String[] zeile = meldelistenZeile == null || meldelistenZeile.length == 0 ? null : meldelistenZeile[0];
		return TeamAnzeigeFormatierer.formatiere(teamnameAnzeigen != 0, anzSpieler, vereinsnameAnzeigen != 0, zeile);
	}

	/** Sucht nur in der ersten Spalte, liest danach ausschließlich die gefundene Zeile. Kein Fokuszugriff. */
	@Override
	public Object ptmuuidteamanzeige(int teamnameAnzeigen, int anzSpieler, int vereinsnameAnzeigen,
			String uuid, XCellRange bereich, Object spielerSpaltenVersatz) {
		if (!TeamUuidFormel.istUuid(uuid)) return Any.VOID;
		int versatz = 0;
		if (spielerSpaltenVersatz != null && !com.sun.star.uno.AnyConverter.isVoid(spielerSpaltenVersatz)) {
			if (!(spielerSpaltenVersatz instanceof Number zahl)
					|| (zahl.doubleValue() != 0 && zahl.doubleValue() != 1)) return Any.VOID;
			versatz = zahl.intValue();
		}
		try {
			XCellRangeAddressable adressierbar = Lo.qi(XCellRangeAddressable.class, bereich);
			if (adressierbar == null) return Any.VOID;
			var adresse = adressierbar.getRangeAddress();
			int letzteZeile = adresse.EndRow - adresse.StartRow;
			int letzteSpalte = adresse.EndColumn - adresse.StartColumn;
			if (versatz > letzteSpalte) return Any.VOID;
			XCellRangeFormula nrSpalte = Lo.qi(XCellRangeFormula.class,
					bereich.getCellRangeByPosition(0, 0, 0, letzteZeile));
			if (nrSpalte == null) return Any.VOID;
			OptionalInt gefunden = TeamUuidFormel.zeileImBereich(uuid, nrSpalte.getFormulaArray());
			if (gefunden.isEmpty()) return Any.VOID;
			int zeile = gefunden.getAsInt() - 1;
			XCellRange daten = bereich.getCellRangeByPosition(versatz, zeile, letzteSpalte, zeile);
			long benoetigt = teamnameAnzeigen != 0 ? 2 : 1L + Math.max(0, anzSpieler) * (vereinsnameAnzeigen != 0 ? 3L : 2L);
			String[] namen = new String[(int) Math.min(benoetigt, letzteSpalte - versatz + 1L)];
			namen[0] = ""; // Nr bzw. übersprungener Teamname wird vom Formatierer nicht verwendet.
			for (int spalte = 1; spalte < namen.length; spalte++) {
				var text = Lo.qi(com.sun.star.text.XTextRange.class, daten.getCellByPosition(spalte, 0));
				namen[spalte] = text == null ? "" : text.getString();
			}
			return TeamAnzeigeFormatierer.formatiere(teamnameAnzeigen != 0, anzSpieler, vereinsnameAnzeigen != 0, namen);
		} catch (com.sun.star.lang.IndexOutOfBoundsException | RuntimeException e) {
			logger.error("PTM.ALG.UUID.TEAMANZEIGE fehlgeschlagen", e);
			return Any.VOID;
		}
	}

	@Override
	public int ptmtnruuid(int nr, String uuid, Object onlineId) {
		return nr;
	}

	@Override
	public int ptmsplruuid(int nr, String uuid, Object onlineId) {
		return nr;
	}

	/**
	 * Der Bereich kommt als {@link XCellRange} aus dem aufrufenden Dokument (LO erzeugt ihn dort), daher ist kein
	 * fokus-basierter Dokumentzugriff nötig; Calc führt ihn zugleich als Abhängigkeit der Formel.
	 */
	@Override
	public Object ptmuuidzeile(String uuid, XCellRange bereich) {
		try {
			XCellRangeFormula formelBereich = Lo.qi(XCellRangeFormula.class, bereich);
			if (formelBereich == null) {
				return Any.VOID;
			}
			OptionalInt zeile = TeamUuidFormel.zeileImBereich(uuid, formelBereich.getFormulaArray());
			return zeile.isPresent() ? Integer.valueOf(zeile.getAsInt()) : Any.VOID;
		} catch (RuntimeException e) {
			logger.error("PTM.ALG.UUIDZEILE fehlgeschlagen", e);
			return Any.VOID;
		}
	}

	/** Wie {@link #ptmuuidzeile}, liefert aber direkt die angezeigte Nr der gefundenen Zeile. */
	@Override
	public Object ptmuuidteamnr(String uuid, XCellRange bereich) {
		try {
			XCellRangeFormula formelBereich = Lo.qi(XCellRangeFormula.class, bereich);
			XCellRangeData datenBereich = Lo.qi(XCellRangeData.class, bereich);
			if (formelBereich == null || datenBereich == null) {
				return Any.VOID;
			}
			OptionalInt nr = TeamUuidFormel.nrImBereich(uuid, formelBereich.getFormulaArray(),
					datenBereich.getDataArray());
			return nr.isPresent() ? Integer.valueOf(nr.getAsInt()) : Any.VOID;
		} catch (RuntimeException e) {
			logger.error("PTM.ALG.UUIDTEAMNR fehlgeschlagen", e);
			return Any.VOID;
		}
	}

	@Override
	String getImplName() {
		return implName;
	}

	@Override
	String[] getServiceNames() {
		return serviceNames;
	}

	// ------------------- PTM.SUPERMELEE.* Formeln -----------------

	private int berechneSupermeleeTriplette(int anzSpieler,
			java.util.function.Function<SuperMeleeTeamRechner, Integer> fn) {
		if (anzSpieler < 4) return 0;
		var rechner = new SuperMeleeTeamRechner(anzSpieler, SuperMeleeMode.Triplette);
		if (!rechner.valideAnzahlSpieler()) return 0;
		return fn.apply(rechner);
	}

	private int berechneSupermeleeDoublette(int anzSpieler,
			java.util.function.Function<SuperMeleeTeamRechner, Integer> fn) {
		if (anzSpieler < 4) return 0;
		var rechner = new SuperMeleeTeamRechner(anzSpieler, SuperMeleeMode.Doublette);
		if (!rechner.valideAnzahlSpieler()) return 0;
		return fn.apply(rechner);
	}

	@Override
	public int ptmsmtriplanzdoublette(int anzSpieler) {
		return berechneSupermeleeTriplette(anzSpieler, SuperMeleeTeamRechner::getAnzDoublette);
	}

	@Override
	public int ptmsmtriplanztriplette(int anzSpieler) {
		return berechneSupermeleeTriplette(anzSpieler, SuperMeleeTeamRechner::getAnzTriplette);
	}

	@Override
	public int ptmsmnurdoublette(int anzSpieler) {
		return berechneSupermeleeTriplette(anzSpieler, r -> r.isNurDoubletteMoeglich() ? 1 : 0);
	}

	@Override
	public int ptmsmdouplanzdoublette(int anzSpieler) {
		return berechneSupermeleeDoublette(anzSpieler, SuperMeleeTeamRechner::getAnzDoublette);
	}

	@Override
	public int ptgsmdouplanztriplette(int anzSpieler) {
		return berechneSupermeleeDoublette(anzSpieler, SuperMeleeTeamRechner::getAnzTriplette);
	}

	@Override
	public int ptgsmnurtriplette(int anzSpieler) {
		return berechneSupermeleeDoublette(anzSpieler, r -> r.isNurTripletteMoeglich() ? 1 : 0);
	}

	@Override
	public int ptmsmtriplanzpaarungen(int anzSpieler) {
		return berechneSupermeleeTriplette(anzSpieler, SuperMeleeTeamRechner::getAnzPaarungen);
	}

	@Override
	public int ptmsmtriplanzbahnen(int anzSpieler) {
		return berechneSupermeleeTriplette(anzSpieler, SuperMeleeTeamRechner::getAnzBahnen);
	}

	@Override
	public int ptmsmdouplanzpaarungen(int anzSpieler) {
		return berechneSupermeleeDoublette(anzSpieler, SuperMeleeTeamRechner::getAnzPaarungen);
	}

	@Override
	public int ptmsmdoupanzbahnen(int anzSpieler) {
		return berechneSupermeleeDoublette(anzSpieler, SuperMeleeTeamRechner::getAnzBahnen);
	}

	@Override
	public int ptmsmvalide(int anzSpieler) {
		if (anzSpieler < 1) return 0;
		return new SuperMeleeTeamRechner(anzSpieler).valideAnzahlSpieler() ? 1 : 0;
	}

	@Override
	public int ptmsmanztriplwennnurtriplette(int anzSpieler) {
		if (anzSpieler < 1) return 0;
		return new SuperMeleeTeamRechner(anzSpieler).getAnzahlTripletteWennNurTriplette();
	}

	@Override
	public int ptmsmanzdoublwennnurdoublette(int anzSpieler) {
		if (anzSpieler < 1) return 0;
		return new SuperMeleeTeamRechner(anzSpieler).getAnzahlDoubletteWennNurDoublette();
	}

	// ------------------- PTM.CADRAGE.* Formeln -----------------

	private int berechneCadrage(int anzTeams,
			java.util.function.Function<CadrageRechner, Integer> fn) {
		if (anzTeams <= 2) return 0;
		return fn.apply(new CadrageRechner(anzTeams));
	}

	@Override
	public int ptmcadrageanzteams(int anzTeams) {
		return berechneCadrage(anzTeams, CadrageRechner::anzTeams);
	}

	@Override
	public int ptmcadragezielanz(int anzTeams) {
		return berechneCadrage(anzTeams, CadrageRechner::zielAnzahlTeams);
	}

	@Override
	public int ptmcadrageanzohnecadrage(int anzTeams) {
		return berechneCadrage(anzTeams, CadrageRechner::anzOhneCadrage);
	}

	// ------------------- PTM.POULE.* Formeln -----------------

	private int berechnePoule(int anzTeams, java.util.function.IntSupplier fn) {
		if (anzTeams < 3) return 0;
		try {
			return fn.getAsInt();
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	@Override
	public int ptmpouleanzgruppen(int anzTeams) {
		return berechnePoule(anzTeams, () -> PouleGruppenRechner.anzGruppen(anzTeams));
	}

	@Override
	public int ptmpouleanzvierergruppen(int anzTeams) {
		return berechnePoule(anzTeams, () -> PouleGruppenRechner.anzVierTeamGruppen(anzTeams));
	}

	@Override
	public int ptmpouleanzdreiergruppen(int anzTeams) {
		return berechnePoule(anzTeams, () -> PouleGruppenRechner.anzDreiTeamGruppen(anzTeams));
	}

	// ------------------- PTM.DB.SPIELER.* Formeln -----------------
	// Synchronisiert über spielerCache als Monitor: HSQLDB-Connection ist nicht
	// thread-safe für parallele Statements, Calc-Recalcs sind heute sequenziell
	// pro Sheet, aber das ist nicht garantiert.

	@Override
	public int ptmdbspieleranzahl() {
		try {
			synchronized (spielerCache) {
				return spielerCache.holeOderBerechne("anzahl", () -> {
					try {
						return spielerRepository().anzahl();
					} catch (SpielerDbException e) {
						logger.error("PTM.DB.SPIELER.ANZAHL fehlgeschlagen", e);
						return 0;
					}
				});
			}
		} catch (RuntimeException | LinkageError e) {
			logger.error("PTM.DB.SPIELER.ANZAHL: harter Init-Fehler", e);
			return 0;
		}
	}

	@Override
	public String[][] ptmdbspielersuche(String suche, boolean wildcard) {
		String normalized = (suche == null ? "" : suche).trim().toLowerCase(Locale.ROOT);
		if (normalized.length() > SUCHE_MAX_LEN) {
			normalized = normalized.substring(0, SUCHE_MAX_LEN);
		}
		String key = "suche:" + wildcard + ":" + normalized;
		String suchTerm = normalized;
		try {
			synchronized (spielerCache) {
				return spielerCache.holeOderBerechne(key, () -> {
					try {
						List<SpielerMitVerein> treffer = spielerRepository()
								.findeMitWildcard(suchTerm, wildcard, SUCHE_LIMIT);
						String[][] tabelle = new String[treffer.size()][3];
						for (int i = 0; i < treffer.size(); i++) {
							SpielerMitVerein s = treffer.get(i);
							tabelle[i][0] = s.vorname();
							tabelle[i][1] = s.nachname();
							tabelle[i][2] = s.vereinName() == null ? "" : s.vereinName();
						}
						return tabelle;
					} catch (SpielerDbException e) {
						logger.error("PTM.DB.SPIELER.SUCHE fehlgeschlagen: '{}'", suchTerm, e);
						return LEERES_SUCH_ERGEBNIS;
					}
				});
			}
		} catch (RuntimeException | LinkageError e) {
			logger.error("PTM.DB.SPIELER.SUCHE: harter Init-Fehler", e);
			return LEERES_SUCH_ERGEBNIS;
		}
	}

	private SpielerRepository spielerRepository() {
		try {
			return new SpielerRepository(SpielerDbConnection.getInstance());
		} catch (SpielerDbException e) {
			throw new IllegalStateException("Spieler-DB nicht erreichbar", e);
		}
	}

	// ------------------- PTM.PLANUNG.* Formeln -----------------
	// Eigenständiger, vom Turniersystem unabhängiger Planungsrechner (siehe PlanungsrechnerSheet).
	// Uhrzeiten kommen als echte Calc-Zeitwerte (Bruchteil des Tages, 09:00 = 0,375) direkt aus der
	// mit UserNumberFormat.TIME formatierten Eingabezelle, nicht als String. Wie alle PTM-Add-In-
	// Funktionen: bei ungültigen Eingaben nie eine Exception werfen, sondern einen neutralen
	// Fallback liefern (0 / "" / leere Tabelle) statt die Zelle mit #WERT! abzureißen.

	@Override
	public int ptmplanungdurchgaengeprorunde(int anzTeams, int anzBahnen) {
		try {
			return PlanungsrechnerRechner.durchgaengeProRunde(anzTeams, anzBahnen);
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	@Override
	public int ptmplanungzeitlimit(double start, double ende, int anzTeams, int anzBahnen, int anzRunden,
			int durchgangPauseMin, int rundenPauseMin) {
		try {
			var ergebnis = PlanungsrechnerRechner.berechneZeitlimit(planungZeit(start), planungZeit(ende),
					anzTeams, anzBahnen, anzRunden, durchgangPauseMin, rundenPauseMin);
			return ergebnis.zeitlimitProDurchgangMinuten();
		} catch (IllegalArgumentException e) {
			return 0;
		}
	}

	@Override
	public String ptmplanungturnierende(double start, int anzTeams, int anzBahnen, int anzRunden,
			int durchgangPauseMin, int rundenPauseMin, int zeitlimitMin) {
		try {
			var ergebnis = PlanungsrechnerRechner.berechneZeitplan(planungZeit(start), anzTeams, anzBahnen,
					anzRunden, durchgangPauseMin, rundenPauseMin, zeitlimitMin);
			return ergebnis.turnierEnde().format(PLANUNG_HH_MM);
		} catch (IllegalArgumentException e) {
			return "";
		}
	}

	@Override
	public String[][] ptmplanungzeitplan(double start, int anzTeams, int anzBahnen, int anzRunden,
			int durchgangPauseMin, int rundenPauseMin, int zeitlimitMin) {
		String[][] tabelle = leeresPlanungZeitplanErgebnis();
		try {
			var ergebnis = PlanungsrechnerRechner.berechneZeitplan(planungZeit(start), anzTeams, anzBahnen,
					anzRunden, durchgangPauseMin, rundenPauseMin, zeitlimitMin);
			List<ZeitplanEintrag> eintraege = ergebnis.eintraege();
			int anzZeilen = Math.min(eintraege.size(), PlanungsrechnerRechner.MAX_ZEITPLAN_ZEILEN);
			for (int i = 0; i < anzZeilen; i++) {
				ZeitplanEintrag eintrag = eintraege.get(i);
				tabelle[i][0] = String.valueOf(eintrag.rundeNr());
				tabelle[i][1] = String.valueOf(eintrag.durchgangNr());
				tabelle[i][2] = eintrag.start().format(PLANUNG_HH_MM);
				tabelle[i][3] = eintrag.ende().format(PLANUNG_HH_MM);
			}
		} catch (IllegalArgumentException e) {
			logger.debug("PTM.PLANUNG.ZEITPLAN: ungueltige Eingabe, leere Tabelle geliefert", e);
		}
		return tabelle;
	}

	/**
	 * Wandelt einen Calc-Zeitwert (Bruchteil eines 24h-Tages, wie ihn eine mit
	 * {@code UserNumberFormat.TIME} formatierte Zelle liefert) in eine {@link LocalTime} um.
	 */
	private static LocalTime planungZeit(double bruchteilDesTages) {
		if (Double.isNaN(bruchteilDesTages) || Double.isInfinite(bruchteilDesTages) || bruchteilDesTages < 0) {
			throw new IllegalArgumentException("Ungueltiger Zeitwert: " + bruchteilDesTages);
		}
		long sekundenDesTages = Math.round(bruchteilDesTages * 86400.0) % 86400;
		return LocalTime.ofSecondOfDay(sekundenDesTages);
	}

	private static String[][] leeresPlanungZeitplanErgebnis() {
		String[][] tabelle = new String[PlanungsrechnerRechner.MAX_ZEITPLAN_ZEILEN][4];
		for (String[] zeile : tabelle) {
			Arrays.fill(zeile, "");
		}
		return tabelle;
	}

}
