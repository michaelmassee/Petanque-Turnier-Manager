package de.petanqueturniermanager.addins;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.CadrageRechner;
import de.petanqueturniermanager.algorithmen.Direktvergleich;
import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEventListener;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.addin.XGlobal;

public final class GlobalImpl extends AbstractAddInImpl implements XGlobal, ITurnierEventListener {
	static final Logger logger = LogManager.getLogger(GlobalImpl.class);

	private final XComponentContext xContext;
	private static final String implName = GlobalImpl.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.addin.GlobalAddIn";
	// WICHTIG: Beide Services müssen registriert sein!
	private static final String[] serviceNames = {
		SERVICE_NAME,
		"com.sun.star.sheet.AddIn"  // Standard AddIn Service
	};

	private static AtomicBoolean isDirty = new AtomicBoolean(false);

	// Instanz-Cache pro Dokument – vermeidet wiederholte UNO-Bulk-Reads bei Formel-Auswertungen.
	// Wird invalidiert wenn Properties geändert werden (via ITurnierEventListener)
	// oder wenn das Dokument wechselt (anderer doc-Hash).
	private volatile int cachedDocHash = 0;
	private volatile DocumentPropertiesHelper cachedHelper = null;

	// DisplayNames aus GlobalAddIn.xcu - diese werden für Formeln in Calc verwendet
	// =PTM.ALG.INTPROPERTY("propertyname")
	// Hinweis: Das "=" wird automatisch von setFormulaInCell() hinzugefügt
	public static final String PTM_INT_PROPERTY = "PTM.ALG.INTPROPERTY";
	public static final String PTM_STRING_PROPERTY = "PTM.ALG.STRINGPROPERTY";
	public static final String PTM_DIREKTVERGLEICH = "PTM.ALG.DIREKTVERGLEICH";
	public static final String PTM_TURNIERSYSTEM = "PTM.ALG.TURNIERSYSTEM";

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

	public static final String FORMAT_PTM_INT_PROPERTY(String propName) {
		return PTM_INT_PROPERTY + "(\"" + propName + "\")";
	}

	public static final String FORMAT_PTM_STRING_PROPERTY(String propName) {
		return PTM_STRING_PROPERTY + "(\"" + propName + "\")";
	}

	// wird nur einmal aufgerufen für alle sheets
	public GlobalImpl(XComponentContext xContext) {
		this.xContext = xContext;
		GlobalImpl.isDirty = new AtomicBoolean(false);
		PetanqueTurnierMngrSingleton.init(xContext);
		PetanqueTurnierMngrSingleton.addTurnierEventListener(this);
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
			XSpreadsheetDocument doc = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
			if (doc != null) {
				int docHash = doc.hashCode();
				if (cachedHelper != null && cachedDocHash == docHash) {
					return cachedHelper; // Cache-Hit: gleiches Dokument, Properties unverändert
				}
				DocumentPropertiesHelper hlpr = new DocumentPropertiesHelper(doc);
				if (hlpr.hasRequiredProperties()) {
					cachedHelper = hlpr;
					cachedDocHash = docHash;
					return hlpr;
				}
				invalidateCache();
			}
			// Kein PTM-Dokument erkannt oder Dokument nicht gefunden
			GlobalImpl.isDirty.set(true);
			logger.debug("Kein PTM-Dokument erkannt");
		} catch (Exception e) {
			logger.error("getDocumentPropertiesHelper", e);
			invalidateCache();
			GlobalImpl.isDirty.set(true);
		}
		return null;
	}

	private void invalidateCache() {
		cachedHelper = null;
		cachedDocHash = 0;
	}

	@Override
	public void onPropertiesChanged(ITurnierEvent eventObj) {
		invalidateCache(); // Properties geändert → beim nächsten Formel-Call neu laden
	}

	@Override
	public int ptmintproperty(String propname) {
		DocumentPropertiesHelper hlpr = getDocumentPropertiesHelper();
		if (hlpr != null) {
			TurnierSystem turnierSystemAusDocument = hlpr.getTurnierSystemAusDocument();

			if (!StringUtils.isAllBlank(propname) && turnierSystemAusDocument != TurnierSystem.KEIN) {
				Integer propVal = hlpr.getIntProperty(propname, 0);
				logger.debug("return:" + propname + "=" + propVal);
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
				logger.debug("return:" + propname + "=" + propVal);
				return propVal;
			}
		}
		return "fehler";
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
	public int ptmcadrageanzfreilose(int anzTeams) {
		return berechneCadrage(anzTeams, CadrageRechner::anzFreilose);
	}

}
