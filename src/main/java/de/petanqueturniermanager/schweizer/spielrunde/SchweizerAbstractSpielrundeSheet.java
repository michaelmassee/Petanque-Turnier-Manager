package de.petanqueturniermanager.schweizer.spielrunde;

import static com.google.common.base.Preconditions.checkNotNull;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_HEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.CHAR_WEIGHT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.HORI_JUSTIFY;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.SHRINK_TO_FIT;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.TABLE_BORDER2;
import static de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties.VERT_JUSTIFY;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.awt.FontWeight;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;
import com.sun.star.table.TableBorder2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.common.DurchgangAufteilungRechner;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerSystem;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeFooterHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeHelper;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.sheet.ConditionalFormatHelper;
import de.petanqueturniermanager.helper.sheet.EditierbaresZelleFormatHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.RangeProperties;
import de.petanqueturniermanager.helper.sheet.numberformat.UserNumberFormat;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetFreeze;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 27.03.2024 / Michael Massee
 */

public abstract class SchweizerAbstractSpielrundeSheet extends SheetRunner implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SchweizerAbstractSpielrundeSheet.class);

	public static final int SHEET_COLOR = SheetTabFarben.SPIELRUNDE;
	public static final String SHEET_NAMEN = "Spielrunde";

	public static final int ERSTE_HEADER_ZEILE = 0;
	public static final int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;
	public static final int ERSTE_DATEN_ZEILE = ZWEITE_HEADER_ZEILE + 1;

	public static final int NR_CHARHEIGHT = 18;
	public static final int BAHN_NR_SPALTE = 0;
	public static final int TEAM_A_SPALTE = BAHN_NR_SPALTE + 1;
	public static final int TEAM_B_SPALTE = TEAM_A_SPALTE + 1;
	public static final int ERG_TEAM_A_SPALTE = TEAM_B_SPALTE + 1;
	public static final int ERG_TEAM_B_SPALTE = ERG_TEAM_A_SPALTE + 1;
	/**
	 * Nur befuellt bei aktiver Rundenzeitplanung (isDurchgangAufteilungWirksam), sonst leer.
	 * Zeigt die Startzeit eines Durchgangs in dessen erster Datenzeile und die Endzeit in dessen
	 * letzter Datenzeile (bei einzeiligen Durchgaengen ueberschreibt die Endzeit die Startzeit in
	 * derselben Zelle — die Endzeit ist die fuer die Weiterverkettung massgebliche Zelle, siehe
	 * {@link #durchgangInfoSpaltenSchreiben}). Durchgaenge werden NICHT per Text-Label markiert,
	 * sondern durch eine doppelte horizontale Linie am oberen Rand der ersten Zeile getrennt,
	 * siehe {@link #durchgangTrennlinienSetzen}.
	 */
	public static final int ZEIT_SPALTE = ERG_TEAM_B_SPALTE + 1;
	public static final int FEHLER_SPALTE = ZEIT_SPALTE + 1;

	private final SchweizerKonfigurationSheet konfigurationSheet;
	private final SchweizerMeldeListeSheetUpdate meldeListe;
	private final SpielrundeHelper spielrundeHelper;
	private final String sheetBaseName;
	private SpielRundeNr spielRundeNrInSheet = null;
	private volatile boolean forceOk = false; // wird fuer Test verwendet

	//	private SpielRundeNr sheetSpielRundeNr = null; // muss nicht der Aktive sein

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, TurnierSystem.SCHWEIZER, SHEET_NAMEN);
	}

	protected SchweizerAbstractSpielrundeSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem ts,
			String sheetBaseName) {
		super(workingSpreadsheet, ts, sheetBaseName);
		this.sheetBaseName = sheetBaseName;
		konfigurationSheet = initKonfigurationSheet(workingSpreadsheet);
		meldeListe = initMeldeListeSheet(workingSpreadsheet);
		spielrundeHelper = new SpielrundeHelper(this, NR_CHARHEIGHT, NR_CHARHEIGHT, true,
				konfigurationSheet.getSpielRundeHintergrundFarbeGeradeStyle(),
				konfigurationSheet.getSpielRundeHintergrundFarbeUnGeradeStyle());
	}

	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	public SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	protected final boolean canStart(TeamMeldungen meldungen) throws GenerateException {
		if (getSpielRundeNr().getNr() < 1) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(I18n.get("schweizer.spielrunde.fehler.ungueltige.spielrunde", getSpielRundeNr().getNr()))
					.show();
			return false;
		}

		if (meldungen.size() < 6) {
			getSheetHelper().setActiveSheet(getMeldeListe().getXSpreadSheet());
			MessageBox.from(getxContext(), MessageBoxTypeEnum.ERROR_OK)
					.caption(I18n.get("msg.caption.aktuelle.spielrunde.fehler"))
					.message(I18n.get("schweizer.spielrunde.fehler.zu.wenige.meldungen", meldungen.size()))
					.show();
			return false;
		}
		return true;
	}

	@VisibleForTesting
	protected SchweizerMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerMeldeListeSheetUpdate(workingSpreadsheet);
	}

	/**
	 * Liefert den Metadaten-Schlüssel für die Spielrunde mit der angegebenen Nummer.
	 * Subklassen (z.B. Maastrichter) können dies überschreiben um einen anderen Schlüssel zu verwenden.
	 */
	protected String getSpielrundeSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselSchweizerSpielrunde(rundeNr);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		var rundeNr = getSpielRundeNr();
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getSpielrundeSchluessel(rundeNr.getNr()), getLegacySheetName(rundeNr));
	}

	/**
	 * Gibt den lokalisierten Tabellennamen für eine Spielrunde zurück (für die Sheet-Erstellung).
	 * Kann von Subklassen überschrieben werden (z.B. für Maastrichter Vorrunden).
	 */
	protected String getSheetName(SpielRundeNr nr) {
		return SheetNamen.spielrunde(nr.getNr());
	}

	/**
	 * Gibt den unveränderlichen deutschen Legacy-Namen zurück (für {@code findeSheetUndHeile}-Fallback
	 * bei älteren Dokumenten ohne Metadaten).
	 */
	protected final String getLegacySheetName(SpielRundeNr nr) {
		return nr.getNr() + ". " + sheetBaseName;
	}

	@Override
	public final TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	public final SpielRundeNr getSpielRundeNr() throws GenerateException {
		return getKonfigurationSheet().getAktiveSpielRunde();
	}

	public SpielRundeNr getSpielRundeNrInSheet() {
		return spielRundeNrInSheet;
	}

	public void setSpielRundeNrInSheet(SpielRundeNr spielRundeNrInSheet) {
		this.spielRundeNrInSheet = spielRundeNrInSheet;
	}

	public SchweizerMeldeListeSheetUpdate getMeldeListe() {
		return meldeListe;
	}

	/**
	 * Liest alle gespielten Runden ein und befüllt:
	 * <ul>
	 *   <li>Team-Gegner-Beziehungen (für Auslosung der nächsten Runde)</li>
	 *   <li>Freilos-Flags</li>
	 * </ul>
	 *
	 * @param aktiveMeldungen aktive Teams
	 * @param abSpielrunde    erste einzulesende Runde (inkl.)
	 * @param bisSpielrunde   letzte einzulesende Runde (inkl.)
	 * @return Auswertungsdaten je Team (Siege, Punktedifferenz, Gegnerliste)
	 */
	protected List<SchweizerTeamErgebnis> gespieltenRundenEinlesen(TeamMeldungen aktiveMeldungen, int abSpielrunde,
			int bisSpielrunde) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punktediff, [2]=punkte+
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		if (bisSpielrunde >= abSpielrunde && bisSpielrunde >= 1) {
			int spielrunde = (abSpielrunde > 1) ? abSpielrunde : 1;
			processBoxinfo("processbox.gespielte.runden.einlesen", spielrunde, bisSpielrunde);
			var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();

			for (; spielrunde <= bisSpielrunde; spielrunde++) {
				SheetRunner.testDoCancelTask();
				// Iterations-Lookup: Metadaten-first (überlebt Umbenennung), Fallback auf Namen
				XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
						getSpielrundeSchluessel(spielrunde), getSheetName(SpielRundeNr.from(spielrunde)));
				if (sheet == null) {
					continue;
				}
				leseRundeEin(sheet, aktiveMeldungen, statsMap, gegnerMap);
			}
		}

		List<SchweizerTeamErgebnis> ergebnisse = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] stats = statsMap.getOrDefault(team.getNr(), new int[3]);
			List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
			ergebnisse.add(new SchweizerTeamErgebnis(team.getNr(), stats[0], stats[1], stats[2], gegnerNrn));
		}
		return ergebnisse;
	}

	private void leseRundeEin(XSpreadsheet sheet, TeamMeldungen aktiveMeldungen, Map<Integer, int[]> statsMap,
			Map<Integer, List<Integer>> gegnerMap) throws GenerateException {
		RangePosition readRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE,
				ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange).getDataFromRange();

		for (RowData row : rowsData) {
			if (row.size() < 2) {
				break;
			}
			int nrA = resolveTeamNr(row.get(0)); // TEAM_A_SPALTE (relativ: 0)
			if (nrA <= 0) {
				break; // Ende der Daten
			}
			Team teamA = aktiveMeldungen.getTeam(nrA);
			if (teamA == null) {
				continue; // Team inaktiv, überspringen
			}

			int nrB = resolveTeamNr(row.get(1)); // TEAM_B_SPALTE (relativ: 1)
			if (nrB <= 0) {
				// Freilos für Team A
				teamA.setHatteFreilos(true);
				statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				continue;
			}
			Team teamB = aktiveMeldungen.getTeam(nrB);
			if (teamB == null) {
				continue;
			}

			teamA.addGegner(teamB); // registriert gegenseitig

			gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
			gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0; // ERG_TEAM_A_SPALTE (relativ: 2)
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0; // ERG_TEAM_B_SPALTE (relativ: 3)

			// Punkte+ für beide Teams
			statsMap.computeIfAbsent(nrA, k -> new int[3])[2] += ergA;
			statsMap.computeIfAbsent(nrB, k -> new int[3])[2] += ergB;

			if (ergA > ergB) {
				statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] += ergA - ergB;
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] -= ergA - ergB;
			} else if (ergB > ergA) {
				statsMap.computeIfAbsent(nrB, k -> new int[3])[0]++;
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] += ergB - ergA;
				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] -= ergB - ergA;
			}
		}
	}

	/**
	 * Löst eine Team-Nr aus einer Zelle auf.
	 * Versucht zunächst den Integer-Wert, dann Name-Lookup über die Meldeliste.
	 */
	private int resolveTeamNr(CellData cell) throws GenerateException {
		int nr = cell.getIntVal(0);
		if (nr > 0) {
			return nr;
		}
		String name = cell.getStringVal();
		if (name != null && !name.isEmpty()) {
			return getMeldeListe().getTeamNrByTeamname(name);
		}
		return 0;
	}

	/**
	 * Sortiert die aktiven Teams nach Ranglisten-Kriterien (Schweizer System)
	 * und gibt eine neue TeamMeldungen-Liste in dieser Reihenfolge zurück.
	 */
	protected TeamMeldungen sortierteTeamMeldungen(TeamMeldungen aktiveMeldungen,
			List<SchweizerTeamErgebnis> ergebnisse) throws GenerateException {
		SchweizerRankingModus modus = getKonfigurationSheet().getRankingModus();
		SchweizerSystem sortierer = new SchweizerSystem();
		List<SchweizerTeamErgebnis> sortiert = sortierer.sortiereNachAuswertungskriterien(ergebnisse, modus);

		TeamMeldungen sortierteMeldungen = new TeamMeldungen();
		for (SchweizerTeamErgebnis erg : sortiert) {
			Team team = aktiveMeldungen.getTeam(erg.teamNr());
			if (team != null) {
				sortierteMeldungen.addTeamWennNichtVorhanden(team);
			}
		}
		return sortierteMeldungen;
	}

	/**
	 * enweder einfach ein laufende nummer, oder jenachdem was in der konfig steht die Spielbahnnummer<br>
	 * property getSpielrundeSpielbahn<br>
	 * X = nur ein laufende paarungen nummer<br>
	 * L = Spielbahn -> leere Spalte<br>
	 * N = Spielbahn -> durchnumeriert<br>
	 * R = Spielbahn -> random<br>
	 *
	 * @throws GenerateException
	 */
	private void datenErsteSpalte() throws GenerateException {
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
		Integer letzteZeile = letztePositionRechtsUnten().getZeile();
		SpielrundeSpielbahn spielrundeSpielbahn = getKonfigurationSheet().getSpielrundeSpielbahn();

		spielrundeHelper.datenErsteSpalte(spielrundeSpielbahn, ERSTE_DATEN_ZEILE, letzteZeile, BAHN_NR_SPALTE,
				ERSTE_HEADER_ZEILE, ZWEITE_HEADER_ZEILE, headerColor);
	}

	private void header() throws GenerateException {
		processBoxinfo("processbox.formatiere.header");
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();
		boolean nameMode = getKonfigurationSheet().getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME;

		Position headerStart = Position.from(TEAM_A_SPALTE, ERSTE_HEADER_ZEILE);

		StringCellValue headerValue = StringCellValue.from(getXSpreadSheet(), headerStart)
				.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true).setEndPosMergeSpaltePlus(3)
				.setValue(I18n.get("schweizer.spielrunde.header.spielrunde", getSpielRundeNr().getNr()));
		getSheetHelper().setStringValueInCell(headerValue);

		int zeile2CharHeight = nameMode ? 12 : NR_CHARHEIGHT;
		String labelA = nameMode ? I18n.get("schweizer.spielrunde.spalte.mannschaft.a") : "A";
		String labelB = nameMode ? I18n.get("schweizer.spielrunde.spalte.mannschaft.b") : "B";

		StringCellValue headerValueZeile2 = StringCellValue
				.from(getXSpreadSheet(), headerStart.zeile(ZWEITE_HEADER_ZEILE)).setVertJustify(CellVertJustify2.CENTER)
				.setHoriJustify(CellHoriJustify.CENTER)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder()).setCellBackColor(headerColor)
				.setCharHeight(zeile2CharHeight).setShrinkToFit(true);

		headerValueZeile2.setValue(labelA);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue(labelB).spaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		headerValueZeile2.setValue(I18n.get("schweizer.spielrunde.spalte.ergebnis")).spaltePlus(1).setEndPosMergeSpaltePlus(1);
		getSheetHelper().setStringValueInCell(headerValueZeile2);

		rundenStartzeitFeld();
	}

	/**
	 * Schreibt (nur wenn {@code isZeitplanAktiv()}) das einzige haendisch editierbare
	 * Zeit-Eingabefeld dieses Features in die freien Header-Zellen von {@link #FEHLER_SPALTE}
	 * (dort steht sonst nichts, siehe {@link #fehlerSpalteFormatieren()}). Runde 1 nutzt die
	 * zentrale Turnier-Startzeit als Default, alle Folgerunden verketten sich per
	 * Cross-Sheet-Zellbezug auf die Startzeit der Vorrunde plus deren Gesamtdauer plus
	 * Rundenpause. Alle Werte werden live per {@code PTM.ALG.INTPROPERTY}/{@code STRINGPROPERTY}
	 * referenziert, damit Aenderungen an der Konfiguration ohne Sheet-Neuaufbau wirken.
	 */
	private void rundenStartzeitFeld() throws GenerateException {
		if (!getKonfigurationSheet().isZeitplanAktiv()) {
			return;
		}
		zeitplanPropertiesPersistieren();
		Integer headerColor = getKonfigurationSheet().getSpielRundeHeaderFarbe();

		StringCellValue labelValue = StringCellValue.from(getXSpreadSheet(), Position.from(FEHLER_SPALTE, ERSTE_HEADER_ZEILE))
				.setVertJustify(CellVertJustify2.BOTTOM).setHoriJustify(CellHoriJustify.CENTER)
				.setCellBackColor(headerColor).setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true)
				.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
				.setValue(I18n.get("schweizer.spielrunde.start.label"));
		getSheetHelper().setStringValueInCell(labelValue);

		Position startzeitPos = Position.from(FEHLER_SPALTE, ZWEITE_HEADER_ZEILE);
		if (getSpielRundeNr().getNr() <= 1) {
			// Runde 1: einmaliger literaler Default aus der Turnier-Startzeit (kein Formelbezug) —
			// konsistent mit der Persistenz-Regel (das Sheet-Feld selbst ist die fuehrende Quelle,
			// keine retroaktive Verschiebung wenn die zentrale Turnier-Startzeit spaeter geaendert wird).
			double bruchteilDesTages = zeitStringZuTagesBruchteil(getKonfigurationSheet().getZeitplanTurnierStartzeit());
			NumberCellValue startzeitValue = NumberCellValue.from(getXSpreadSheet(), startzeitPos)
					.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
					.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setValue(bruchteilDesTages);
			getSheetHelper().setNumberValueInCell(startzeitValue);
		} else {
			StringCellValue startzeitValue = StringCellValue.from(getXSpreadSheet(), startzeitPos, rundenStartzeitFormel())
					.setVertJustify(CellVertJustify2.CENTER).setHoriJustify(CellHoriJustify.CENTER)
					.setCharHeight(NR_CHARHEIGHT).setShrinkToFit(true)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder());
			getSheetHelper().setFormulaInCell(startzeitValue);
		}

		RangePosition startzeitRange = RangePosition.from(startzeitPos, startzeitPos);
		RangeHelper.from(this, startzeitRange).setRangeProperties(RangeProperties.from().numberFormat(UserNumberFormat.TIME));
		EditierbaresZelleFormatHelper.anwenden(this, startzeitRange);
	}

	/**
	 * Ruft die Getter der Zeitplan-Zeitwerte einmal auf, damit deren Konfig-Default via
	 * {@code readIntProperty} in die UserDefinedProperties des Dokuments persistiert wird, falls
	 * dort noch kein Wert existiert (z.B. weil der Zeitplan-Dialog noch nie geoeffnet wurde). Ohne
	 * das liefert das weiter unten live referenzierte {@code PTM.ALG.INTPROPERTY(...)} in den
	 * gebauten Formeln 0 statt des echten Defaults — sichtbarer Bug: Endzeit == Startzeit.
	 */
	private void zeitplanPropertiesPersistieren() {
		var konfig = getKonfigurationSheet();
		konfig.getZeitplanZeitlimitMinuten();
		konfig.getZeitplanDurchgangPauseMinuten();
		konfig.getZeitplanRundenPauseMinuten();
	}

	/** "09:00" -&gt; 0.375 (Bruchteil des Tages, Calc-interne Zeit-Repraesentation). Ungueltige Eingabe -&gt; 0.0 (Mitternacht). */
	private static double zeitStringZuTagesBruchteil(String hhMm) {
		try {
			String[] teile = hhMm.split(":");
			int stunden = Integer.parseInt(teile[0].trim());
			int minuten = teile.length > 1 ? Integer.parseInt(teile[1].trim()) : 0;
			return (stunden * 60 + minuten) / 1440.0;
		} catch (RuntimeException e) {
			return 0.0;
		}
	}

	/**
	 * Nur fuer Runde N&gt;1: Cross-Sheet-Bezug auf das tatsaechliche Ende des letzten Durchgangs der
	 * Vorrunde (siehe {@link #ermittleLetzteZeitZeile}) plus Rundenpause — dieselbe
	 * Verkettungslogik wie zwischen zwei Durchgaengen innerhalb einer Runde, nur rundenuebergreifend.
	 * War die Vorrunde nicht aufgeteilt (kein Eintrag in {@link #ZEIT_SPALTE}), wird ersatzweise die
	 * Rundenstartzeit der Vorrunde plus ein Zeitlimit (Dauer der einzigen, ungeteilten Runde) plus
	 * Rundenpause verwendet.
	 * <p>
	 * Der Sheet-Name im Formel-Bezug wird ueber {@link SheetMetadataHelper#findeSheetUndHeile}
	 * aufgeloest (Metadaten-first, ueberlebt Umbenennung), nicht ueber den lokalisierten
	 * Default-Namen {@link #getSheetName} — sonst entsteht ein {@code #REF!}, sobald die Vorrunde
	 * umbenannt wurde, obwohl die Metadaten-Suche das richtige Sheet findet.
	 */
	private String rundenStartzeitFormel() throws GenerateException {
		SpielRundeNr aktuelleRunde = getSpielRundeNr();
		SpielRundeNr vorherigeRunde = SpielRundeNr.from(aktuelleRunde.getNr() - 1);
		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		XSpreadsheet vorSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
				getSpielrundeSchluessel(vorherigeRunde.getNr()), getSheetName(vorherigeRunde));
		// Vorrunde muesste eigentlich existieren (Runde N>1 setzt Runde N-1 voraus); defensiv
		// trotzdem auf den Default-Namen zurueckfallen statt eine NPE zu riskieren.
		String vorSheetName = vorSheet != null ? Lo.qi(XNamed.class, vorSheet).getName() : getSheetName(vorherigeRunde);
		String rundenPause = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_RUNDEN_PAUSE_MINUTEN);

		int letzteZeitZeile = vorSheet != null ? ermittleLetzteZeitZeile(vorSheet, xDoc) : -1;
		if (letzteZeitZeile >= 0) {
			String letzteEndeZelle = "$'" + vorSheetName + "'." + Position.from(ZEIT_SPALTE, letzteZeitZeile).getAddressWith$();
			return letzteEndeZelle + "+TIME(0;" + rundenPause + ";0)";
		}

		String vorZelle = "$'" + vorSheetName + "'." + Position.from(FEHLER_SPALTE, ZWEITE_HEADER_ZEILE).getAddressWith$();
		String zeitlimit = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
		return vorZelle + "+TIME(0;" + zeitlimit + "+" + rundenPause + ";0)";
	}

	/**
	 * Liest die Zeile der letzten befuellten {@link #ZEIT_SPALTE}-Zelle der Vorrunde aus (= Ende des
	 * letzten Durchgangs, da Durchgang-Bloecke lueckenlos aufeinanderfolgen und der letzte Block
	 * immer bei der letzten Datenzeile endet). -1, wenn die Vorrunde nicht aufgeteilt war.
	 */
	private int ermittleLetzteZeitZeile(XSpreadsheet vorSheet, XSpreadsheetDocument xDoc) throws GenerateException {
		RangePosition zeitRange = RangePosition.from(ZEIT_SPALTE, ERSTE_DATEN_ZEILE, ZEIT_SPALTE, ERSTE_DATEN_ZEILE + 999);
		RangeData zeitDaten = RangeHelper.from(vorSheet, xDoc, zeitRange).getDataFromRange();
		int letzteNichtLeere = -1;
		for (int i = 0; i < zeitDaten.size(); i++) {
			String val = zeitDaten.get(i).get(0).getStringVal();
			if (val != null && !val.isEmpty()) {
				letzteNichtLeere = i;
			}
		}
		return letzteNichtLeere >= 0 ? ERSTE_DATEN_ZEILE + letzteNichtLeere : -1;
	}

	/**
	 * spalten Teampaarungen + Ergebnis
	 * 
	 * @throws GenerateException
	 */

	private void datenformatieren() throws GenerateException {
		processBoxinfo("processbox.formatiere.daten");

		XSpreadsheet sheet = getXSpreadSheet();
		Position datenStart = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		Position datenEnd = letztePositionRechtsUnten();

		// komplett mit normal gitter
		RangePosition datenRangeInclErg = RangePosition.from(datenStart, datenEnd);
		TableBorder2 border = BorderFactory.from().allThin().toBorder();
		getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, TABLE_BORDER2, border);

		SpielrundeHintergrundFarbeGeradeStyle geradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeGeradeStyle();
		SpielrundeHintergrundFarbeUnGeradeStyle unGeradeColor = getKonfigurationSheet()
				.getSpielRundeHintergrundFarbeUnGeradeStyle();

		// Zebra-Farbe: Team-A- und Team-B-Spalten
		RangePosition datenRangeSpielpaarungen = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
		spielrundeHelper.formatiereGeradeUngradeSpielpaarungen(this, datenRangeSpielpaarungen, geradeColor,
				unGeradeColor);

		// Alle Spalten (A, B, Erg) zentrieren
		getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, HORI_JUSTIFY, CellHoriJustify.CENTER);
		getSheetHelper().setPropertyInRange(sheet, datenRangeInclErg, VERT_JUSTIFY, CellVertJustify2.CENTER);

		RangePosition abSpalten = RangePosition.from(datenRangeInclErg).endeSpalte(TEAM_B_SPALTE);
		if (getKonfigurationSheet().getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME) {
			// Teamname-Modus: Font 12, an Zellgröße anpassen, Spaltenbreite 6 cm
			getSheetHelper().setPropertyInRange(sheet, abSpalten, CHAR_HEIGHT, 12);
			getSheetHelper().setPropertyInRange(sheet, abSpalten, SHRINK_TO_FIT, Boolean.TRUE);
			getSheetHelper().setColumnWidth(sheet, TEAM_A_SPALTE, 6000); // 6 cm = 6000 (1/100 mm)
			getSheetHelper().setColumnWidth(sheet, TEAM_B_SPALTE, 6000);
		} else {
			// Teamnummer-Modus: große Schrift für A/B-Spalten
			getSheetHelper().setPropertyInRange(sheet, abSpalten, CHAR_HEIGHT, 32);
			getSheetHelper().setPropertyInRange(sheet, abSpalten, CHAR_WEIGHT, FontWeight.BOLD);
		}

		// Ergebnis-Spalten: Zebra + Validierung 0–13 (wie Supermelee)
		datenEnd = letztePositionRechtsUnten(); // neu einlesen
		RangePosition ergebnisRange = RangePosition.from(
				Position.from(ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE), Position.from(datenEnd));

		getSheetHelper().setPropertyInRange(sheet, ergebnisRange, CHAR_HEIGHT, 32);
		getSheetHelper().setPropertyInRange(sheet, ergebnisRange, CHAR_WEIGHT, FontWeight.BOLD);

		spielrundeHelper.formatiereErgebnissRange(this, ergebnisRange, ERG_TEAM_A_SPALTE);

		// Editierbare Felder hervorheben: Ergebnis-Spalten A und B
		if (datenEnd != null) {
			RangePosition ergebnisEditierbarRange = RangePosition.from(
					ERG_TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, datenEnd.getZeile());
			EditierbaresZelleFormatHelper.anwenden(this, ergebnisEditierbarRange);
		}
	}

	/**
	 * Setzt den Druckbereich über die gesamte Tabelle
	 * (von BAHN_NR_SPALTE/ERSTE_HEADER_ZEILE bis ERG_TEAM_B_SPALTE/letzte Datenzeile, ohne Fehler-Spalte).
	 */
	private void druckBereichSetzen() throws GenerateException {
		processBoxinfo("processbox.print.bereich");
		Position letztePos = letztePositionRechtsUnten();
		if (letztePos == null) {
			return;
		}
		// Bei aktiver Zeitplanung wird der Druckbereich bis FEHLER_SPALTE (letzte Spalte) erweitert.
		int letzteDruckSpalte = getKonfigurationSheet().isZeitplanAktiv() ? FEHLER_SPALTE : ERG_TEAM_B_SPALTE;
		RangePosition druckBereich = RangePosition.from(BAHN_NR_SPALTE, ERSTE_HEADER_ZEILE,
				Position.from(letzteDruckSpalte, letztePos.getZeile()));
		PrintArea.from(getXSpreadSheet(), getWorkingSpreadsheet()).setPrintArea(druckBereich);
		SpielrundeFooterHelper.schreibeFooterUndErweitereDruckbereich(this, getXSpreadSheet(), getWorkingSpreadsheet());
	}

	/**
	 * Setzt Trennlinien:
	 * <ul>
	 *   <li>Doppelte Linie rechts von BAHN_NR_SPALTE (erste Spalte)</li>
	 *   <li>Doppelte Linie rechts von TEAM_B_SPALTE (Trennlinie B/Ergebnis)</li>
	 * </ul>
	 * Muss nach allen anderen Formatierungen aufgerufen werden.
	 */
	private void trennlinienSetzen() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		Position letztePos = letztePositionRechtsUnten();
		if (letztePos == null) {
			return;
		}
		int letzteZeile = letztePos.getZeile();

		// Doppelte Linie rechts von BAHN_NR_SPALTE
		RangePosition bahnNrRange = RangePosition.from(BAHN_NR_SPALTE, ERSTE_HEADER_ZEILE,
				BAHN_NR_SPALTE, letzteZeile);
		getSheetHelper().setPropertyInRange(sheet, bahnNrRange, TABLE_BORDER2,
				BorderFactory.from().allThin().doubleLn().forRight().toBorder());

		// Doppelte Trennlinie rechts von TEAM_B_SPALTE (zwischen B und Ergebnis)
		RangePosition teamBRange = RangePosition.from(TEAM_B_SPALTE, ERSTE_HEADER_ZEILE,
				TEAM_B_SPALTE, letzteZeile);
		getSheetHelper().setPropertyInRange(sheet, teamBRange, TABLE_BORDER2,
				BorderFactory.from().allThin().doubleLn().forRight().toBorder());

		// Dicke untere Linie für die gesamte zweite Header-Zeile (ohne Fehler-Spalte)
		// Nur BottomLine setzen (IsBottomLineValid=true), andere Borders bleiben erhalten
		RangePosition headerUntenRange = RangePosition.from(BAHN_NR_SPALTE, ZWEITE_HEADER_ZEILE,
				ERG_TEAM_B_SPALTE, ZWEITE_HEADER_ZEILE);
		getSheetHelper().setPropertyInRange(sheet, headerUntenRange, TABLE_BORDER2,
				BorderFactory.from().boldLn().forBottom().toBorder());
	}

	/**
	 * Fügt rechts neben den Ergebnisspalten eine Fehler-Spalte ein.
	 * <p>
	 * Pro Datenzeile wird eine Formel eingetragen, die „FEHLER" anzeigt, wenn:
	 * <ul>
	 *   <li>ein Ergebnis außerhalb 0–13 liegt</li>
	 *   <li>beide Ergebnisse gleich sind (Unentschieden nicht erlaubt)</li>
	 *   <li>genau eine der beiden Zellen leer ist</li>
	 * </ul>
	 */
	private void fehlerSpalteFormatieren() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		Position letztePos = letztePositionRechtsUnten();
		if (letztePos == null) {
			return;
		}
		int letzteZeile = letztePos.getZeile();

		getSheetHelper().setColumnWidth(sheet, Position.from(FEHLER_SPALTE, ERSTE_HEADER_ZEILE), 1800);

		for (int zeile = ERSTE_DATEN_ZEILE; zeile <= letzteZeile; zeile++) {
			String ergA = Position.from(ERG_TEAM_A_SPALTE, zeile).getAddress();
			String ergB = Position.from(ERG_TEAM_B_SPALTE, zeile).getAddress();

			// @formatter:off
			String formel = "IF(OR(" +
					"AND(ISBLANK(" + ergA + ");ISBLANK(" + ergB + "));" +
					"AND(" + ergA + "<14;" + ergB + "<14;" + ergA + ">-1;" + ergB + ">-1;" + ergA + "<>" + ergB + ")" +
					");\"\";\"" + I18n.get("schweizer.spielrunde.fehler.formel") + "\")";
			// @formatter:on

			StringCellValue cv = StringCellValue
					.from(sheet, Position.from(FEHLER_SPALTE, zeile), formel)
					.setCharColor(ColorHelper.CHAR_COLOR_RED)
					.setCharWeight(FontWeight.BOLD)
					.setCharHeight(14)
					.setHoriJustify(CellHoriJustify.CENTER);
			getSheetHelper().setFormulaInCell(cv);
		}
	}

	/**
	 * Spalte SpielerNR A verwenden um die letzte zeile zu ermitteln<br>
	 * Spalte ist dann ergebniss Team B
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public Position letztePositionRechtsUnten() throws GenerateException {
		Position spielerNrPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);

		RangePosition erstSpielrNrRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_A_SPALTE,
				ERSTE_DATEN_ZEILE + 999);

		// alle Daten einlesen (String oder Integer je nach Spielplan-Anzeige-Modus)
		RangeData nrDaten = RangeHelper.from(this, erstSpielrNrRange).getDataFromRange();

		// erste leere Zelle (leer = weder Int-Wert noch String-Inhalt)
		int index = IntStream.range(0, nrDaten.size()).filter(nrDatenIdx -> {
			String val = nrDaten.get(nrDatenIdx).get(0).getStringVal();
			return val == null || val.isEmpty();
		}).findFirst().orElse(-1);

		if (index == 0) {
			return null; // Keine Daten
		}
		if (index > 0) {
			spielerNrPos.zeilePlus(index - 1);
		}

		return spielerNrPos.spalte(ERG_TEAM_B_SPALTE);
	}

	protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr,
			List<SchweizerTeamErgebnis> ergebnisse) throws GenerateException {
		return neueSpielrunde(meldungen, neueSpielrundeNr, ergebnisse, isForceOk());
	}

	protected boolean neueSpielrunde(TeamMeldungen meldungen, SpielRundeNr neueSpielrundeNr,
			List<SchweizerTeamErgebnis> ergebnisse, boolean force) throws GenerateException {
		checkNotNull(meldungen);

		processBoxinfo("processbox.neue.spielrunde", neueSpielrundeNr.getNr());
		processBoxinfo("processbox.anzahl.meldungen", meldungen.size());

		// wenn hier dann neu erstellen
		if (!NewSheet.from(this, getSheetName(neueSpielrundeNr), getSpielrundeSchluessel(neueSpielrundeNr.getNr()))
				.pos(DefaultSheetPos.SCHWEIZER_WORK).setForceCreate(force).setActiv().hideGrid()
				.tabColor(getKonfigurationSheet().getSpielrundeTabFarbe()).create().isDidCreate()) {
			ProcessBox.from().info(I18n.get("schweizer.spielrunde.abbruch"));
			return false;
		}

		// neue Spielrunde speichern, sheet vorhanden
		getKonfigurationSheet().setAktiveSpielRunde(neueSpielrundeNr);

		SchweizerSystem schweizerSystem = new SchweizerSystem();

		List<TeamPaarung> paarungen;

		if (neueSpielrundeNr.getNr() == 1) {
			paarungen = schweizerSystem.ersteRunde(meldungen.teams());
		} else {
			paarungen = schweizerSystem.weitereRunde(meldungen.teams(), ergebnisse);
		}

		teamPaarungenEinfuegen(paarungen);
		datenErsteSpalte(); // BahnNr
		bahnNummerierungProDurchgangFallsAktiv(paarungen.size()); // ueberschreibt BahnNr pro Durchgang neu ab 1
		datenformatieren();
		fehlerSpalteFormatieren();
		header();
		trennlinienSetzen();
		durchgangTrennlinienSetzen(paarungen.size());
		druckBereichSetzen();
		SheetFreeze.from(getTurnierSheet()).anzZeilen(ERSTE_DATEN_ZEILE).doFreeze();

		return true;
	}

	/**
	 * Daten <br>
	 * kein hintergrund
	 * 
	 * @param paarungen
	 * @throws GenerateException
	 */

	private void teamPaarungenEinfuegen(List<TeamPaarung> paarungen) throws GenerateException {
		if (paarungen == null) {
			return;
		}

		boolean useTeamname = getKonfigurationSheet().getSpielplanTeamAnzeige() == SpielplanTeamAnzeige.NAME;
		int freispielPlus = getKonfigurationSheet().getFreispielPunktePlus();
		int freispielMinus = getKonfigurationSheet().getFreispielPunkteMinus();
		RangeData rangeData = new RangeData();

		for (TeamPaarung teamPaarung : paarungen) {
			SheetRunner.testDoCancelTask();
			if (!teamPaarung.hasB()) {
				// Freilos – Team A ohne Gegner eintragen, ERG mit Freispiel-Werten vorbelegen
				RowData freilosRow = rangeData.addNewRow();
				if (useTeamname) {
					freilosRow.add(new CellData("")); // Name folgt per Formel (teamNamenFormelnSchreiben)
				} else {
					freilosRow.add(new CellData(teamPaarung.getA().getNr()));
				}
				freilosRow.add(new CellData("")); // kein Gegner = Freilos
				freilosRow.add(new CellData(freispielPlus)); // ERG_TEAM_A vorbelegen
				freilosRow.add(new CellData(freispielMinus)); // ERG_TEAM_B vorbelegen
				continue;
			}
			if (useTeamname) {
				RowData row = rangeData.addNewRow();
				row.add(new CellData("")); // Name folgt per Formel (teamNamenFormelnSchreiben)
				row.add(new CellData(""));
			} else {
				rangeData.addNewRow(teamPaarung.getA().getNr(), teamPaarung.getB().getNr());
			}
		}

		Position startPos = Position.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);

		if (useTeamname) {
			teamNamenFormelnSchreiben(paarungen);
		}

		durchgangInfoSpaltenSchreiben(paarungen.size());
	}

	/**
	 * Schreibt (nur wenn {@code isDurchgangAufteilungWirksam()} und tatsaechlich mehr als ein
	 * Durchgang noetig ist) fuer jeden Durchgang-Block dessen Startzeit in die erste und dessen
	 * Endzeit in die letzte Datenzeile von {@link #ZEIT_SPALTE} (bei einzeiligen Bloecken ist das
	 * dieselbe Zelle — die zuletzt geschriebene Endzeit gewinnt dort). Jeder Durchgang ab dem
	 * zweiten verkettet sich direkt an die tatsaechliche Endzeit-Zelle des vorherigen Durchgangs
	 * plus Durchgang-Pause (kein rekonstruierter Faktor N-1 mehr — vermeidet Drift, falls Zeitlimit
	 * oder Pause sich waehrend der laufenden Runde aendern). Kein Text-Label ("Durchgang N") — die
	 * Durchgaenge werden stattdessen rein optisch per doppelter Trennlinie unterschieden, siehe
	 * {@link #durchgangTrennlinienSetzen}. Der Team-A/B/Erg-A/Erg-B-Datenstrom bleibt dadurch
	 * vollstaendig unangetastet (siehe {@code leseRundeEin()}).
	 */
	private void durchgangInfoSpaltenSchreiben(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		int bahnen = getKonfigurationSheet().getZeitplanAnzahlBahnen();
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen, bahnen);
		if (bloecke.size() <= 1) {
			return; // Paarungen passen in einen Durchgang, keine Aufteilung noetig
		}

		getSheetHelper().setColumnWidth(getXSpreadSheet(), Position.from(ZEIT_SPALTE, ERSTE_HEADER_ZEILE), 1800);

		String rundenStartzeitAdresse = Position.from(FEHLER_SPALTE, ZWEITE_HEADER_ZEILE).getAddressWith$();
		String zeitlimit = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_ZEITLIMIT_MINUTEN);
		String pause = GlobalImpl.FORMAT_PTM_INT_PROPERTY(SchweizerPropertiesSpalte.KONFIG_PROP_ZEITPLAN_DURCHGANG_PAUSE_MINUTEN);

		int zeile = ERSTE_DATEN_ZEILE;
		String vorherigeEndeAdresse = null;
		for (int groesse : bloecke) {
			SheetRunner.testDoCancelTask();

			int letzteZeileDesBlocks = zeile + groesse - 1;
			String startFormel = vorherigeEndeAdresse == null ? rundenStartzeitAdresse
					: vorherigeEndeAdresse + "+TIME(0;" + pause + ";0)";
			Position startPos = Position.from(ZEIT_SPALTE, zeile);
			zeitZelleSchreiben(startPos, startFormel);

			String endeAdresse;
			if (letzteZeileDesBlocks == zeile) {
				// einzeiliger Block: Endzeit ueberschreibt die soeben geschriebene Startzeit in derselben Zelle
				String endeFormel = startPos.getAddress() + "+TIME(0;" + zeitlimit + ";0)";
				zeitZelleSchreiben(startPos, endeFormel);
				endeAdresse = startPos.getAddress();
			} else {
				Position endePos = Position.from(ZEIT_SPALTE, letzteZeileDesBlocks);
				String endeFormel = startPos.getAddress() + "+TIME(0;" + zeitlimit + ";0)";
				zeitZelleSchreiben(endePos, endeFormel);
				endeAdresse = endePos.getAddress();
			}
			vorherigeEndeAdresse = endeAdresse;

			zeile += groesse;
		}
	}

	/** Schreibt eine Zeit-Formel (Zellformat Zeit HH:MM) in {@link #ZEIT_SPALTE}. */
	private void zeitZelleSchreiben(Position pos, String formel) throws GenerateException {
		StringCellValue zeitValue = StringCellValue.from(getXSpreadSheet(), pos, formel)
				.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER)
				.setCharHeight(12);
		getSheetHelper().setFormulaInCell(zeitValue);
		RangePosition zeitRange = RangePosition.from(pos, pos);
		RangeHelper.from(this, zeitRange).setRangeProperties(RangeProperties.from().numberFormat(UserNumberFormat.TIME));
	}

	/**
	 * Trennt die Durchgaenge optisch durch eine doppelte horizontale Linie am oberen Rand der
	 * jeweils ersten Zeile eines neuen Durchgangs (kein Trennstrich vor dem ersten Durchgang,
	 * der liegt direkt unter dem bereits abgegrenzten Header). Muss NACH {@link #datenformatieren()}
	 * aufgerufen werden, da dessen {@code allThin()}-Rahmen sonst die Trennlinie ueberschreiben
	 * wuerde; nur die obere Linie wird gesetzt (IsTopLineValid=true), alle anderen Seiten bleiben
	 * unveraendert (analog {@link #trennlinienSetzen()}).
	 */
	private void durchgangTrennlinienSetzen(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen,
				getKonfigurationSheet().getZeitplanAnzahlBahnen());
		if (bloecke.size() <= 1) {
			return;
		}

		XSpreadsheet sheet = getXSpreadSheet();
		TableBorder2 doppelteLinieOben = BorderFactory.from().doubleLn().forTop().toBorder();

		int zeile = ERSTE_DATEN_ZEILE;
		for (int i = 0; i < bloecke.size(); i++) {
			if (i > 0) {
				RangePosition trennzeile = RangePosition.from(BAHN_NR_SPALTE, zeile, FEHLER_SPALTE, zeile);
				getSheetHelper().setPropertyInRange(sheet, trennzeile, TABLE_BORDER2, doppelteLinieOben);
			}
			zeile += bloecke.get(i);
		}
	}

	/**
	 * Muss NACH {@link #datenErsteSpalte()} aufgerufen werden, da {@link #bahnNummerierungProDurchgang}
	 * dessen fortlaufende 1..Anzahl-Paarungen-Nummerierung gezielt ueberschreibt.
	 */
	private void bahnNummerierungProDurchgangFallsAktiv(int anzahlPaarungen) throws GenerateException {
		if (!getKonfigurationSheet().isDurchgangAufteilungWirksam() || anzahlPaarungen <= 0) {
			return;
		}
		List<Integer> bloecke = DurchgangAufteilungRechner.berechne(anzahlPaarungen,
				getKonfigurationSheet().getZeitplanAnzahlBahnen());
		if (bloecke.size() <= 1) {
			return;
		}
		bahnNummerierungProDurchgang(bloecke);
	}

	/**
	 * Bei aktiver Durchgang-Aufteilung beginnt die Bahn-Nummer in {@link #BAHN_NR_SPALTE} pro
	 * Durchgang-Block neu bei 1 (physische Bahnen werden zwischen Durchgaengen wiederverwendet).
	 * Ueberschreibt gezielt nur die Datenzeilen, die {@link #datenErsteSpalte()} zuvor im
	 * Standard-Modus (fortlaufend 1..Anzahl Paarungen) geschrieben hat. Nur fuer die
	 * "nummerierten" {@link SpielrundeSpielbahn}-Modi X/N sinnvoll — L (haendisch) und
	 * R (zufaellig) bleiben unveraendert, siehe Review-Punkt 2: diese Property ist unabhaengig
	 * von {@link SpielrundeSpielbahn} und veraendert dessen bestehende Semantik nicht.
	 */
	private void bahnNummerierungProDurchgang(List<Integer> bloecke) throws GenerateException {
		SpielrundeSpielbahn modus = getKonfigurationSheet().getSpielrundeSpielbahn();
		if (modus != SpielrundeSpielbahn.X && modus != SpielrundeSpielbahn.N) {
			return;
		}
		RangeData rangeData = new RangeData();
		for (int groesse : bloecke) {
			for (int n = 1; n <= groesse; n++) {
				rangeData.addNewRow(n);
			}
		}
		Position startPos = Position.from(BAHN_NR_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);

		bahnNrDuplikatPruefungProDurchgang(bloecke);
	}

	/**
	 * {@code SpielrundeHelper.datenErsteSpalte()} markiert doppelte Bahn-Nummern per bedingter
	 * Formatierung rot — die COUNTIF-Formel prueft dabei ueber die GESAMTE Spalte (siehe
	 * {@code Position.getSpalteAddressWith$()}). Da die Bahn-Nummern bei aktiver
	 * Durchgang-Aufteilung pro Durchgang bewusst neu ab 1 beginnen (z.B. 1,2,3,1,2,3,1,2), wuerde
	 * diese Pruefung faelschlich jede Zeile als „doppelt" rot einfaerben. Ersetzt die Pruefung
	 * durch eine pro Durchgang-Block skalierte Variante, die Duplikate nur innerhalb desselben
	 * Durchgangs erkennt.
	 */
	private void bahnNrDuplikatPruefungProDurchgang(List<Integer> bloecke) throws GenerateException {
		FehlerStyle fehlerStyle = new FehlerStyle();
		int zeile = ERSTE_DATEN_ZEILE;
		for (int groesse : bloecke) {
			RangePosition blockRange = RangePosition.from(BAHN_NR_SPALTE, zeile, BAHN_NR_SPALTE, zeile + groesse - 1);
			String conditionFindDoppelt = "COUNTIF(" + blockRange.getAddressWith$() + ";"
					+ ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")>1";
			String conditionNotEmpty = ConditionalFormatHelper.FORMULA_CURRENT_CELL + "<>\"\"";
			String formulaFindDoppelteBahnNr = "AND(" + conditionFindDoppelt + ";" + conditionNotEmpty + ")";
			ConditionalFormatHelper.from(this, blockRange).clear().formula1(formulaFindDoppelteBahnNr)
					.operator(ConditionOperator.FORMULA).style(fehlerStyle).applyAndDoReset();
			zeile += groesse;
		}
	}

	/**
	 * Schreibt die Team-Namen im Anzeigemodus {@link SpielplanTeamAnzeige#NAME} als
	 * SVERWEIS-Formel (statt statischem Text), damit eine spätere Umbenennung in der
	 * Meldeliste im Spielplan sofort sichtbar bleibt (siehe auch {@link #resolveTeamNr}).
	 */
	private void teamNamenFormelnSchreiben(List<TeamPaarung> paarungen) throws GenerateException {
		if (paarungen.isEmpty()) {
			return;
		}
		boolean teamnameAnzeigen = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAnzeigen = getKonfigurationSheet().isMeldeListeVereinsnameAnzeigen();
		Formation formation = getKonfigurationSheet().getMeldeListeFormation();

		String[][] formulas = new String[paarungen.size()][2];
		for (int i = 0; i < paarungen.size(); i++) {
			TeamPaarung teamPaarung = paarungen.get(i);
			formulas[i][0] = MeldeListeHelper.teamNameFormel(String.valueOf(teamPaarung.getA().getNr()),
					teamnameAnzeigen, formation, vereinsnameAnzeigen);
			formulas[i][1] = teamPaarung.hasB()
					? MeldeListeHelper.teamNameFormel(String.valueOf(teamPaarung.getB().getNr()), teamnameAnzeigen,
							formation, vereinsnameAnzeigen)
					: "";
		}

		RangePosition teamRange = RangePosition.from(TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, TEAM_B_SPALTE,
				ERSTE_DATEN_ZEILE + paarungen.size() - 1);
		getSheetHelper().setFormulaArrayInRange(getXSpreadSheet(), teamRange, formulas);

		// Die Runden-Erzeugung läuft mit deaktivierter Automatikberechnung (Performance);
		// ohne expliziten Rechenlauf blieben die neuen Formelzellen bis zum nächsten
		// Nutzer-Trigger auf 0 stehen.
		getxCalculatable().calculateAll();
	}

	/**
	 * fuer Test
	 * 
	 * @return
	 */
	public boolean isForceOk() {
		return forceOk;
	}

	/**
	 * fuer Test
	 * 
	 * @return
	 */
	public void setForceOk(boolean forceOk) {
		this.forceOk = forceOk;
	}

}
