package de.petanqueturniermanager.schweizer.rangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerSystem;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheetsync.EingabeSignatur;
import de.petanqueturniermanager.helper.sheetsync.SheetSyncSignaturStore;
import de.petanqueturniermanager.helper.rangliste.SignaturQuellen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Erstellt die Rangliste für das Schweizer Turniersystem.
 * <p>
 * Liest alle vorhandenen Spielrunden-Sheets ein und berechnet die Sortierreihenfolge.
 * Alle Zellwerte (Siege, BHZ, FBHZ, Punkte+, Punkte-, Differenz, Teamname) werden direkt
 * als Werte via RangeData geschrieben – keine Sheet-Formeln.
 * <p>
 * Sortierkriterien (Schweizer System):
 * <ol>
 *   <li>Siege (absteigend)</li>
 *   <li>BHZ = Summe der Siege aller Gegner (absteigend)</li>
 *   <li>FBHZ = Summe der BHZ-Werte aller Gegner (absteigend)</li>
 *   <li>Punktedifferenz (absteigend)</li>
 * </ol>
 */
public class SchweizerRanglisteSheet extends SheetRunner implements IRangliste {

	public static final int HEADER_ZEILE          = 0;
	public static final int ZWEITE_HEADER_ZEILE   = 1;
	public static final int ERSTE_DATEN_ZEILE     = 2;

	public static final int TEAM_NR_SPALTE        = 0;  // A
	public static final int TEAM_NAME_SPALTE      = 1;  // B
	public static final int PLATZ_SPALTE          = 2;  // C
	public static final int SIEGE_SPALTE          = 3;  // D
	public static final int BHZ_SPALTE            = 4;  // E
	public static final int FBHZ_SPALTE           = 5;  // F
	public static final int PUNKTE_PLUS_SPALTE    = 6;  // G
	public static final int PUNKTE_MINUS_SPALTE   = 7;  // H
	public static final int PUNKTE_DIFF_SPALTE    = 8;  // I
	public static final int VALIDATE_SPALTE       = PUNKTE_DIFF_SPALTE + 1;  // J (versteckt)

	private static final int COL_WIDTH_NR   = 800;
	private static final int COL_WIDTH_NAME = 7000;
	private static final int COL_WIDTH_DATA = 1400;

	private static final Logger logger = LogManager.getLogger(SchweizerRanglisteSheet.class);

	/** Hält die erweiterten Auswertungsdaten für die Ranglisten-Sortierung. */
	private record TeamRanglisteData(int teamNr, int siege, int punktePlus, int punkteMinus,
			List<Integer> gegnerNrn) {

		int punkteDiff() {
			return punktePlus - punkteMinus;
		}

		SchweizerTeamErgebnis toErgebnis() {
			return new SchweizerTeamErgebnis(teamNr, siege, punkteDiff(), punktePlus, gegnerNrn);
		}
	}

	private final SchweizerKonfigurationSheet konfigurationSheet;
	private final RangListeSorter rangListeSorter;

	public SchweizerRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, TurnierSystem.SCHWEIZER);
	}

	protected SchweizerRanglisteSheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem ts) {
		super(workingSpreadsheet, ts);
		konfigurationSheet = initKonfigurationSheet(workingSpreadsheet);
		rangListeSorter = new RangListeSorter(this);
	}

	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new SchweizerKonfigurationSheet(workingSpreadsheet);
	}

	/** Basisname der Spielrunden-Sheets (z.B. "Spielrunde" oder "Vorrunde"). */
	protected String getSpielrundenBasisName() {
		return SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
	}

	/** Name des Ranglisten-Sheets. */
	protected String getRanglistenSheetName() {
		return SheetNamen.rangliste();
	}

	/** Named-Range-Schlüssel für die Sheet-Metadaten (überschreibbar für Subklassen). */
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_SCHWEIZER_RANGLISTE;
	}

	/**
	 * Signatur-Engine für den Hash-Commit nach Vollaufbau (überschreibbar für Subklassen).
	 * Muss die identischen Quellen liefern wie der zugehörige
	 * {@link de.petanqueturniermanager.helper.sheetsync.SheetSyncListener}.
	 */
	protected EingabeSignatur getEingabeSignatur() {
		return new EingabeSignatur(SignaturQuellen::fuerSchweizer);
	}

	/**
	 * Named-Range-Schlüssel für ein einzelnes Spielrunden-Sheet.
	 * Subklassen (z.B. Maastrichter) überschreiben dies, damit beim Lookup nicht
	 * versehentlich ein systemfremder Schlüssel ins Dokument geheilt wird.
	 */
	protected String getSpielrundenMetadatenSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselSchweizerSpielrunde(rundeNr);
	}

	/** Erstellt das Meldelisten-Sheet-Objekt für das Lesen der Teamnamen/Nummern. */
	protected SchweizerMeldeListeSheetUpdate erstelleMeldeListeSheet() {
		return new SchweizerMeldeListeSheetUpdate(getWorkingSpreadsheet());
	}

	@Override
	public SchweizerKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	/**
	 * Letzte sichtbare Datenspalte (vor der versteckten Validate-Spalte).
	 * <p>
	 * Subklassen (z.B. Maastrichter mit zusätzlicher Gruppe-Spalte) überschreiben
	 * diesen Hook, damit Druckbereich, Zebra-Bereich und Validate-Spaltenposition
	 * automatisch mitwachsen.
	 */
	protected int letzteAnzeigeSpalte() {
		return PUNKTE_DIFF_SPALTE;
	}

	/**
	 * Hook für Subklassen, die zusätzliche Header-Spalten rechts der Standard-Header
	 * benötigen. Default: no-op.
	 */
	protected void erweitereHeader(XSpreadsheet sheet, Integer headerColor) throws GenerateException {
		// no-op
	}

	/**
	 * Hook für Subklassen, die zusätzliche Datenspalten rechts der Punkte-Differenz
	 * füllen. Wird aufgerufen, nachdem die Standardspalten geschrieben sind.
	 * Default: no-op.
	 */
	protected void erweitereDaten(XSpreadsheet sheet, List<SchweizerTeamErgebnis> sortiert,
			int letzteZeile) throws GenerateException {
		// no-op
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return SheetMetadataHelper.findeSheetUndHeile(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getMetadatenSchluessel(), getRanglistenSheetName());
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public void doRun() throws GenerateException {
		doRunIntern();
	}

	private void doRunIntern() throws GenerateException {
		logger.debug("doRunIntern START – Thread='{}'", Thread.currentThread().getName());
		processBoxinfo("processbox.rangliste.einfuegen");

		NewSheet.from(this, getRanglistenSheetName(), getMetadatenSchluessel())
				.pos(DefaultSheetPos.SCHWEIZER_ENDRANGLISTE)
				.forceCreate()
				.tabColor(getKonfigurationSheet().getRanglisteTabFarbe())
				.create();

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			return;
		}

		SchweizerMeldeListeSheetUpdate meldeliste = erstelleMeldeListeSheet();
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen.size() == 0) {
			processBoxinfo("processbox.abbruch");
			return;
		}

		insertHeader(sheet, getKonfigurationSheet().getRankingModus());
		berechnungUndSchreiben(sheet, meldeliste, aktiveMeldungen);

		if (SheetRunner.isRunning()) {
			getSheetHelper().setActiveSheet(sheet);
			SheetRunner.unterdrückeNaechstesSelectionChange();
		}
		SheetSyncSignaturStore.commitVollaufbau(
				getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				getMetadatenSchluessel(),
				getEingabeSignatur());
		logger.debug("doRunIntern ENDE – Thread='{}'", Thread.currentThread().getName());
	}

	/**
	 * Berechnet alle Ranglisten-Daten aus den Spielrunden und schreibt sie in das Sheet.
	 * <p>
	 * Wird sowohl vom vollständigen Neuaufbau ({@link #doRunIntern()}) als auch vom
	 * inkrementellen Update ({@link SchweizerRanglisteSheetUpdate}) verwendet.
	 * Das Sheet muss bereits existieren und der Header muss gesetzt sein.
	 */
	protected void berechnungUndSchreiben(XSpreadsheet sheet,
			SchweizerMeldeListeSheetUpdate meldeliste, TeamMeldungen aktiveMeldungen) throws GenerateException {
		int bisSpielrunde = getKonfigurationSheet().getAktiveSpielRunde().getNr();
		SchweizerRankingModus modus = getKonfigurationSheet().getRankingModus();
		logger.debug("berechnungUndSchreiben – {} Spielrunden, Modus={}, Thread='{}'",
				bisSpielrunde, modus, Thread.currentThread().getName());

		List<TeamRanglisteData> ranglisteData = leseAlleSpielergebnisse(aktiveMeldungen, bisSpielrunde, meldeliste);
		logger.debug("berechnungUndSchreiben – {} Teams, Siege-Summe={}",
				ranglisteData.size(), ranglisteData.stream().mapToInt(TeamRanglisteData::siege).sum());

		List<SchweizerTeamErgebnis> ergebnisse = ranglisteData.stream()
				.map(TeamRanglisteData::toErgebnis)
				.collect(Collectors.toList());
		List<SchweizerTeamErgebnis> sortiert = new SchweizerSystem().sortiereNachAuswertungskriterien(ergebnisse,
				modus);

		// BHZ/FBHZ nur berechnen wenn der Modus es erfordert – bei OHNE_BUCHHOLZ bleiben die Spalten leer (0)
		var schweizerSystem = new SchweizerSystem();
		Map<Integer, Integer> bhzMap;
		Map<Integer, Integer> fbhzMap;
		if (modus == SchweizerRankingModus.OHNE_BUCHHOLZ) {
			bhzMap  = new HashMap<>();
			fbhzMap = new HashMap<>();
		} else {
			bhzMap  = schweizerSystem.berechneBuchholz(ergebnisse);
			fbhzMap = schweizerSystem.berechneFeinbuchholz(ergebnisse, bhzMap);
		}

		// Teamnamen direkt aus der Meldeliste lesen
		Map<Integer, String> teamNrZuName = leseTeamnamenAusSheet(meldeliste);
		insertDatenAlsWerte(sheet, sortiert, bhzMap, fbhzMap, teamNrZuName);
		getSheetHelper().setOptimaleBreitePlusMarge(sheet, TEAM_NR_SPALTE, SheetHelper.OPTIMALE_BREITE_MARGE);

		if (!sortiert.isEmpty()) {
			new RangListeSpalte(PLATZ_SPALTE, this).upDateRanglisteSpalte();
			getRangListeSorter().insertSortValidateSpalte(true);
			// Pl.-Spalte: dicke Linie rechts + Fett
			int letzteZeilePlatz = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
			getSheetHelper().setPropertiesInRange(sheet,
					RangePosition.from(PLATZ_SPALTE, ERSTE_DATEN_ZEILE, PLATZ_SPALTE, letzteZeilePlatz),
					CellProperties.from()
							.margin(MeldeListeKonstanten.CELL_MARGIN)
							.setBorder(BorderFactory.from().allThin().boldLn().forTop().forRight().toBorder())
							.setCharWeight(com.sun.star.awt.FontWeight.BOLD));
		}

		// Erweiterungen für Subklassen (z.B. Maastrichter: Gruppe-Spalte)
		erweitereDaten(sheet, sortiert, ERSTE_DATEN_ZEILE + sortiert.size() - 1);

		// Zebra-Formatierung für Datenbereich
		if (!sortiert.isEmpty()) {
			int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
			RangePosition datenRange = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE,
					letzteAnzeigeSpalte(), letzteZeile);
			RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
					.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
					.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade())
					.validateSpalte(validateSpalte())
					.apply();

			// Absicherung: Leerzeile + Fußzeile direkt unterhalb der Daten dürfen keine
			// Zebra-Hintergrundfarbe tragen (z.B. Rest aus einem früheren Aufbau mit mehr Zeilen).
			RangePosition leerBereichUnterDaten = RangePosition.from(TEAM_NR_SPALTE, letzteZeile + 1,
					letzteAnzeigeSpalte(), letzteZeile + 2);
			getSheetHelper().setPropertiesInRange(sheet, leerBereichUnterDaten,
					CellProperties.from().setCellBackColor(-1));
		}

		// Footer und Druckbereich
		int letzteZeile;
		if (sortiert.isEmpty()) {
			letzteZeile = ZWEITE_HEADER_ZEILE;
		} else {
			insertFooter(sheet, sortiert.size(), modus);
			letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() + 1;
		}
		setzeDruckbereich(sheet, letzteZeile);
		getxCalculatable().calculateAll();
	}

	private List<TeamRanglisteData> leseAlleSpielergebnisse(TeamMeldungen aktiveMeldungen, int bisSpielrunde,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punkte+, [2]=punkte-
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		var xDoc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		for (int runde = 1; runde <= bisSpielrunde; runde++) {
			SheetRunner.testDoCancelTask();
			// Iterations-Lookup: Metadaten-first (überlebt Umbenennung), Fallback auf Namen
			XSpreadsheet rundeSheet = SheetMetadataHelper.findeSheetUndHeile(xDoc,
					getSpielrundenMetadatenSchluessel(runde), runde + ". " + getSpielrundenBasisName());
			if (rundeSheet == null) {
				logger.debug("leseAlleSpielergebnisse: Runde {} – Sheet nicht gefunden, übersprungen", runde);
				continue;
			}
			logger.debug("leseAlleSpielergebnisse: lese Runde {}", runde);
			leseRundeEin(rundeSheet, aktiveMeldungen, statsMap, gegnerMap, meldeliste);
		}

		List<TeamRanglisteData> result = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] stats = statsMap.getOrDefault(team.getNr(), new int[3]);
			List<Integer> gegnerNrn = gegnerMap.getOrDefault(team.getNr(), new ArrayList<>());
			result.add(new TeamRanglisteData(team.getNr(), stats[0], stats[1], stats[2], gegnerNrn));
		}
		return result;
	}

	private void leseRundeEin(XSpreadsheet rundeSheet, TeamMeldungen aktiveMeldungen,
			Map<Integer, int[]> statsMap, Map<Integer, List<Integer>> gegnerMap,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {
		int dbgSiegeVorher = statsMap.values().stream().mapToInt(a -> a[0]).sum();

		// Lese ab TEAM_A_SPALTE(1) bis ERG_TEAM_B_SPALTE(4), ab ERSTE_DATEN_ZEILE(2)
		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(rundeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		int dbgZeilen = 0;
		for (RowData row : rowsData) {
			if (row.size() < 2) break;

			int nrA = resolveTeamNr(row.get(0), meldeliste);
			if (nrA <= 0) break; // Ende der Daten
			dbgZeilen++;
			Team teamA = aktiveMeldungen.getTeam(nrA);
			if (teamA == null) continue;

			int nrB = resolveTeamNr(row.get(1), meldeliste);
			if (nrB <= 0) {
				// Freilos für Team A
				statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++;
				continue;
			}
			Team teamB = aktiveMeldungen.getTeam(nrB);
			if (teamB == null) continue;

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
				// Gegner erst bei tatsächlich eingetragenem Ergebnis für BHZ/FBHZ zählen –
				// eine bereits erzeugte, aber noch ungespielte Paarung darf die Buchholz-Werte
				// der Vorrunden-Rangliste nicht beeinflussen.
				gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
				gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

				statsMap.computeIfAbsent(nrA, k -> new int[3])[1] += ergA; // punkte+
				statsMap.computeIfAbsent(nrA, k -> new int[3])[2] += ergB; // punkte-
				statsMap.computeIfAbsent(nrB, k -> new int[3])[1] += ergB; // punkte+
				statsMap.computeIfAbsent(nrB, k -> new int[3])[2] += ergA; // punkte-
				if (ergA > ergB) {
					statsMap.computeIfAbsent(nrA, k -> new int[3])[0]++; // siege für A
				} else if (ergB > ergA) {
					statsMap.computeIfAbsent(nrB, k -> new int[3])[0]++; // siege für B
				}
			}
		}
		int dbgSiegeNachher = statsMap.values().stream().mapToInt(a -> a[0]).sum();
		logger.debug("leseRundeEin: {} Paarungszeilen verarbeitet, neue Siege in dieser Runde: {}",
				dbgZeilen, dbgSiegeNachher - dbgSiegeVorher);
	}

	private int resolveTeamNr(CellData cell, SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {
		int nr = cell.getIntVal(0);
		if (nr > 0) return nr;
		String name = cell.getStringVal();
		if (name != null && !name.isEmpty()) {
			return meldeliste.getTeamNrByTeamname(name);
		}
		return 0;
	}

	private void insertHeader(XSpreadsheet sheet, SchweizerRankingModus modus) throws GenerateException {
		Integer headerColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();
		boolean ohneBuchholz = modus == SchweizerRankingModus.OHNE_BUCHHOLZ;

		// ── Spaltenbreiten setzen ──────────────────────────────────────────────────
		int[][] spaltenBreiten = {
				{ PLATZ_SPALTE,        COL_WIDTH_NR   },
				{ TEAM_NAME_SPALTE,    COL_WIDTH_NAME  },
				{ SIEGE_SPALTE,        COL_WIDTH_DATA  },
				{ BHZ_SPALTE,          COL_WIDTH_DATA  },
				{ FBHZ_SPALTE,         COL_WIDTH_DATA  },
				{ PUNKTE_PLUS_SPALTE,  COL_WIDTH_DATA  },
				{ PUNKTE_MINUS_SPALTE, COL_WIDTH_DATA  },
				{ PUNKTE_DIFF_SPALTE,  COL_WIDTH_DATA  },
		};
		for (int[] sw : spaltenBreiten) {
			ColumnProperties props = ColumnProperties.from().setWidth(sw[1])
					.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER);
			// LO ignoriert Width=0 — BHZ/FBHZ müssen via IsVisible=false ausgeblendet werden
			if (ohneBuchholz && (sw[0] == BHZ_SPALTE || sw[0] == FBHZ_SPALTE)) {
				props.isVisible(false);
			} else {
				props.isVisible(true);
			}
			getSheetHelper().setColumnProperties(sheet, sw[0], props);
		}

		// ── Zeile 0: Einzel-Spalten, vertikal über beide Header-Zeilen zusammengeführt ──
		var einzelSpalten = new int[][] {
				{ PLATZ_SPALTE,      1 }, // 1 = boldRight
				{ TEAM_NR_SPALTE,    2 }, // 2 = doubleRight
				{ TEAM_NAME_SPALTE,  0 },
				{ SIEGE_SPALTE,      0 },
				{ BHZ_SPALTE,        0 },
				{ FBHZ_SPALTE,       0 },
		};
		String nameSpalteHeader = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()
				? I18n.get("column.header.teamname") : I18n.get("schweizer.rangliste.spalte.team");
		String[] einzelTexte = {
				I18n.get("column.header.platz"),
				I18n.get("column.header.nr"),
				nameSpalteHeader,
				I18n.get("column.header.siege"),
				I18n.get("column.header.bhz"),
				I18n.get("column.header.fbhz") };
		for (int i = 0; i < einzelSpalten.length; i++) {
			int col = einzelSpalten[i][0];
			int borderTyp = einzelSpalten[i][1];
			var border = borderTyp == 1
					? BorderFactory.from().allThin().boldLn().forBottom().forRight().toBorder()
					: borderTyp == 2
					? BorderFactory.from().allThin().boldLn().forBottom().doubleLn().forRight().toBorder()
					: BorderFactory.from().allThin().boldLn().forBottom().toBorder();
			var cv = StringCellValue
					.from(sheet, Position.from(col, HEADER_ZEILE), einzelTexte[i])
					.setCellBackColor(headerColor)
					.setBorder(border)
					.setHoriJustify(CellHoriJustify.CENTER)
					.setVertJustify(CellVertJustify2.CENTER)
					.setEndPosMergeZeilePlus(1)  // vertikal Row 0 + Row 1
					.setShrinkToFit(true);
			if (col == PLATZ_SPALTE) {
				cv.setRotate90().setCharWeight(com.sun.star.awt.FontWeight.BOLD);
			}
			getSheetHelper().setStringValueInCell(cv);
		}

		// ── Zeile 0: "Punkte" horizontal über 3 Spalten zusammengeführt ──────────
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(PUNKTE_PLUS_SPALTE, HEADER_ZEILE), I18n.get("column.header.punkte"))
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setEndPosMergeSpalte(PUNKTE_DIFF_SPALTE)  // horizontal G–I
				.setShrinkToFit(true));

		// ── Zeile 1: Sub-Header für die Punkte-Spalten ───────────────────────────
		String[] subTexte = {
				I18n.get("schweizer.rangliste.spalte.punkte.plus"),
				I18n.get("schweizer.rangliste.spalte.punkte.minus"),
				I18n.get("schweizer.rangliste.spalte.punkte.differenz") };
		int[] subCols    = { PUNKTE_PLUS_SPALTE, PUNKTE_MINUS_SPALTE, PUNKTE_DIFF_SPALTE };
		for (int i = 0; i < subCols.length; i++) {
			getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet, Position.from(subCols[i], ZWEITE_HEADER_ZEILE), subTexte[i])
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setHoriJustify(CellHoriJustify.CENTER)
					.setVertJustify(CellVertJustify2.CENTER)
					.setShrinkToFit(true));
		}

		// Erweiterungen für Subklassen (z.B. Maastrichter: Gruppe-Spalte)
		erweitereHeader(sheet, headerColor);
	}

	/**
	 * Liest Teamnamen (teamname-Modus) oder Spielernamen (NR-Modus) aus dem Meldeliste-Sheet
	 * via Bulk-Read in eine Map teamNr → Anzeigename.
	 */
	private Map<Integer, String> leseTeamnamenAusSheet(SchweizerMeldeListeSheetUpdate meldeliste)
			throws GenerateException {
		Map<Integer, String> result = new HashMap<>();
		XSpreadsheet mlSheet = meldeliste.getXSpreadSheet();
		if (mlSheet == null) return result;

		XSpreadsheetDocument doc = getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
		int nrSpalte   = meldeliste.getTeamNrSpalte();
		int ersteZeile = meldeliste.getErsteDatenZiele();

		if (getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()) {
			// Teamname-Modus: Nr + Teamname Spalte lesen
			int nameSpalte = meldeliste.getTeamnameSpalte();
			int maxSpalte  = Math.max(nrSpalte, nameSpalte);
			RangeData data = RangeHelper
					.from(mlSheet, doc, RangePosition.from(0, ersteZeile, maxSpalte, ersteZeile + 999))
					.getDataFromRange();
			for (RowData row : data) {
				if (row.size() <= nrSpalte) break;
				int nr = row.get(nrSpalte).getIntVal(0);
				if (nr <= 0) break;
				String name = nameSpalte < row.size() ? row.get(nameSpalte).getStringVal() : null;
				result.put(nr, name != null ? name.trim() : "");
			}
		} else {
			// NR-Modus: Spieler-Vor+Nachnamen aller Spieler im Team zusammenbauen
			int anzSpieler = getKonfigurationSheet().getMeldeListeFormation().getAnzSpieler();
			int[] vorSpalten  = new int[anzSpieler];
			int[] nachSpalten = new int[anzSpieler];
			int maxSpalte = nrSpalte;
			for (int s = 0; s < anzSpieler; s++) {
				vorSpalten[s]  = meldeliste.getVornameSpalte(s);
				nachSpalten[s] = meldeliste.getNachnameSpalte(s);
				maxSpalte = Math.max(maxSpalte, Math.max(vorSpalten[s], nachSpalten[s]));
			}
			RangeData data = RangeHelper
					.from(mlSheet, doc, RangePosition.from(0, ersteZeile, maxSpalte, ersteZeile + 999))
					.getDataFromRange();
			for (RowData row : data) {
				if (row.size() <= nrSpalte) break;
				int nr = row.get(nrSpalte).getIntVal(0);
				if (nr <= 0) break;
				var sb = new StringBuilder();
				for (int s = 0; s < anzSpieler; s++) {
					String vn = vorSpalten[s] < row.size()  ? row.get(vorSpalten[s]).getStringVal()  : null;
					String nn = nachSpalten[s] < row.size() ? row.get(nachSpalten[s]).getStringVal() : null;
					String spielerName = buildSpielerName(vn, nn);
					if (!spielerName.isEmpty()) {
						if (sb.length() > 0) sb.append(" / ");
						sb.append(spielerName);
					}
				}
				result.put(nr, sb.toString());
			}
		}
		return result;
	}

	private static String buildSpielerName(String vorname, String nachname) {
		String vn = vorname  != null ? vorname.trim()  : "";
		String nn = nachname != null ? nachname.trim() : "";
		if (vn.isEmpty() && nn.isEmpty()) return "";
		if (vn.isEmpty()) return nn;
		if (nn.isEmpty()) return vn;
		return vn + " " + nn;
	}

	/**
	 * Schreibt alle Daten der sortierten Rangliste direkt als Werte via RangeData –
	 * keine Sheet-Formeln.
	 * <p>
	 * Block 1 (Spalten A–B): TeamNr + Teamname<br>
	 * Block 2 (Spalten D–I): Siege, BHZ, FBHZ, Punkte+, Punkte-, PunkteDiff
	 */
	private void insertDatenAlsWerte(XSpreadsheet sheet, List<SchweizerTeamErgebnis> sortiert,
			Map<Integer, Integer> bhzMap, Map<Integer, Integer> fbhzMap,
			Map<Integer, String> teamNrZuName) throws GenerateException {
		if (sortiert.isEmpty()) return;

		int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;

		// ── Block 1: TeamNr + Teamname ─────────────────────────────────────────
		RangeData block1 = new RangeData();
		for (SchweizerTeamErgebnis erg : sortiert) {
			RowData row = block1.addNewRow();
			row.newInt(erg.teamNr());
			row.newString(teamNrZuName.getOrDefault(erg.teamNr(), ""));
		}
		RangeHelper.from(this,
				block1.getRangePosition(Position.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE)))
				.setDataInRange(block1);

		// ── Block 2: Siege, BHZ, FBHZ, Punkte+, Punkte−, PunkteDiff ──────────
		RangeData block2 = new RangeData();
		for (SchweizerTeamErgebnis erg : sortiert) {
			int punktePlus  = erg.erzieltePunkte();
			int punkteMinus = erg.erzieltePunkte() - erg.punktedifferenz();
			RowData row = block2.addNewRow();
			row.newInt(erg.siege());
			row.newInt(bhzMap.getOrDefault(erg.teamNr(), 0));
			row.newInt(fbhzMap.getOrDefault(erg.teamNr(), 0));
			row.newInt(punktePlus);
			row.newInt(punkteMinus);
			row.newInt(erg.punktedifferenz());
		}
		RangeHelper.from(this,
				block2.getRangePosition(Position.from(SIEGE_SPALTE, ERSTE_DATEN_ZEILE)))
				.setDataInRange(block2);

		// ── Formatierung ───────────────────────────────────────────────────────
		// TeamNr: grau + doppelte rechte Linie + zentriert
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE, letzteZeile),
				CellProperties.from()
						.margin(MeldeListeKonstanten.CELL_MARGIN)
						.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
						.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder())
						.centerJustify());

		// Teamname: links ausgerichtet, vertikal zentriert
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(TEAM_NAME_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NAME_SPALTE, letzteZeile),
				CellProperties.from().margin(MeldeListeKonstanten.CELL_MARGIN).setAllThinBorder()
						.setHoriJustify(CellHoriJustify.LEFT).centerVertJustify());

		// Zahlen-Spalten: horizontal + vertikal zentriert
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(SIEGE_SPALTE, ERSTE_DATEN_ZEILE, PUNKTE_DIFF_SPALTE, letzteZeile),
				CellProperties.from().margin(MeldeListeKonstanten.CELL_MARGIN).setAllThinBorder().centerJustify());
	}

	// ── IRangliste ──────────────────────────────────────────────────────────────

	@Override
	public int getErsteDatenZiele() throws GenerateException {
		return ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getErsteSpalte() throws GenerateException {
		return TEAM_NR_SPALTE;
	}

	@Override
	public int getLetzteSpalte() throws GenerateException {
		return letzteAnzeigeSpalte();
	}

	@Override
	public int getErsteSummeSpalte() throws GenerateException {
		return SIEGE_SPALTE;
	}

	@Override
	public int getManuellSortSpalte() throws GenerateException {
		return -1;
	}

	@Override
	public int validateSpalte() throws GenerateException {
		return letzteAnzeigeSpalte() + 1;
	}

	@Override
	public void calculateAll() {
		// nicht benötigt
	}

	@Override
	public List<Position> getRanglisteSpalten() throws GenerateException {
		SchweizerRankingModus modus = getKonfigurationSheet().getRankingModus();
		List<Position> spalten = new ArrayList<>();
		spalten.add(Position.from(SIEGE_SPALTE, ERSTE_DATEN_ZEILE));
		if (modus != SchweizerRankingModus.OHNE_BUCHHOLZ) {
			spalten.add(Position.from(BHZ_SPALTE, ERSTE_DATEN_ZEILE));
			spalten.add(Position.from(FBHZ_SPALTE, ERSTE_DATEN_ZEILE));
		}
		spalten.add(Position.from(PUNKTE_DIFF_SPALTE, ERSTE_DATEN_ZEILE));
		return spalten;
	}

	@Override
	public int sucheLetzteZeileMitSpielerNummer() throws GenerateException {
		var searchProp = new java.util.HashMap<String, Object>();
		searchProp.put(RangeSearchHelper.SEARCH_BACKWARDS, true);
		Position result = RangeSearchHelper
				.from(this, RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NR_SPALTE,
						ERSTE_DATEN_ZEILE + 999))
				.searchNachRegExprInSpalte("^\\d", searchProp);
		return result != null ? result.getZeile() : ERSTE_DATEN_ZEILE;
	}

	@Override
	public int getLetzteMitDatenZeileInSpielerNrSpalte() throws GenerateException {
		return sucheLetzteZeileMitSpielerNummer();
	}

	// ── Hilfsmethoden ────────────────────────────────────────────────────────────

	private void insertFooter(XSpreadsheet sheet, int anzTeams, SchweizerRankingModus modus) throws GenerateException {
		processBoxinfo("processbox.fusszeile.einfuegen");
		int footerZeile = ERSTE_DATEN_ZEILE + anzTeams + 1;
		String schluessel = modus == SchweizerRankingModus.OHNE_BUCHHOLZ
				? "schweizer.rangliste.reihenfolge.platzierung.ohnebuchholz"
				: "schweizer.rangliste.reihenfolge.platzierung";
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(TEAM_NR_SPALTE, footerZeile), I18n.get(schluessel))
				.setHoriJustify(CellHoriJustify.LEFT)
				.setCharHeight(8)
				.setEndPosMergeSpalte(letzteAnzeigeSpalte()));
	}

	/** Setzt den Druckbereich: Spalten A bis letzteAnzeigeSpalte (ohne Validator-Spalte), Zeilen 1 bis letzteZeile. */
	private void setzeDruckbereich(XSpreadsheet sheet, int letzteZeile) throws GenerateException {
		var linksOben   = Position.from(TEAM_NR_SPALTE, HEADER_ZEILE);
		var rechtsUnten = Position.from(letzteAnzeigeSpalte(), letzteZeile);
		PrintArea.from(sheet, getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}
}
