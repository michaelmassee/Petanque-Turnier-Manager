package de.petanqueturniermanager.schweizer.rangliste;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellHoriJustify;
import com.sun.star.table.CellVertJustify2;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.algorithmen.SchweizerSystem;
import de.petanqueturniermanager.helper.print.PrintArea;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.rangliste.RangListeSpalte;
import de.petanqueturniermanager.helper.sheet.search.RangeSearchHelper;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.CellProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.RanglisteGeradeUngeradeFormatHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Erstellt die Rangliste für das Schweizer Turniersystem.
 * <p>
 * Liest alle vorhandenen Spielrunden-Sheets ein und berechnet die Sortierreihenfolge.
 * Die Zellwerte (Siege, BHZ, FBHZ, Punkte+, Punkte-, Differenz) werden als Sheet-Formeln
 * eingetragen, sodass sie sich bei Änderungen an den Spielrunden-Sheets automatisch aktualisieren.
 * <p>
 * Sortierkriterien (Schweizer System):
 * <ol>
 *   <li>Siege (absteigend)</li>
 *   <li>BHZ = Summe der Siege aller Gegner (absteigend)</li>
 *   <li>FBHZ = Summe der BHZ-Werte aller Gegner (absteigend)</li>
 *   <li>Punktedifferenz (absteigend)</li>
 * </ol>
 */
public class SchweizerRanglisteSheet extends SchweizerSheet implements IRangliste {

	private static final Logger LOGGER = LogManager.getLogger(SchweizerRanglisteSheet.class);

	public static final String SHEETNAME = "Rangliste";
	public static final String SHEET_COLOR = "d637e8";

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

	private final RangListeSorter rangListeSorter;

	public SchweizerRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		rangListeSorter = new RangListeSorter(this);
	}

	protected RangListeSorter getRangListeSorter() {
		return rangListeSorter;
	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	public void doRun() throws GenerateException {
		processBoxinfo("Erstelle Rangliste...");

		NewSheet.from(this, SHEETNAME)
				.pos(DefaultSheetPos.SCHWEIZER_ENDRANGLISTE)
				.forceCreate()
				.tabColor(SHEET_COLOR)
				.create();

		XSpreadsheet sheet = getXSpreadSheet();
		if (sheet == null) {
			return;
		}

		SchweizerMeldeListeSheetUpdate meldeliste = new SchweizerMeldeListeSheetUpdate(getWorkingSpreadsheet());
		TeamMeldungen aktiveMeldungen = meldeliste.getAktiveMeldungen();
		if (aktiveMeldungen == null || aktiveMeldungen.size() == 0) {
			processBoxinfo("Keine aktiven Meldungen gefunden.");
			return;
		}

		int bisSpielrunde = getKonfigurationSheet().getAktiveSpielRunde().getNr();
		SchweizerRankingModus modus = getKonfigurationSheet().getRankingModus();
		List<TeamRanglisteData> ranglisteData = leseAlleSpielergebnisse(aktiveMeldungen, bisSpielrunde, meldeliste);

		List<SchweizerTeamErgebnis> ergebnisse = ranglisteData.stream()
				.map(TeamRanglisteData::toErgebnis)
				.collect(Collectors.toList());
		List<SchweizerTeamErgebnis> sortiert = new SchweizerSystem().sortiereNachAuswertungskriterien(ergebnisse,
				modus);

		insertHeader(sheet, modus);
		insertDaten(sheet, sortiert, meldeliste);

		if (!sortiert.isEmpty()) {
			insertFormeln(sheet, sortiert.size(), bisSpielrunde, modus);
			new RangListeSpalte(PLATZ_SPALTE, this).upDateRanglisteSpalte();
			getRangListeSorter().insertSortValidateSpalte(true);
			// Pl.-Spalte: dicke Linie rechts (boldTop kommt von RangListeSpalte, hier ergänzt)
			int letzteZeilePlatz = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
			getSheetHelper().setPropertiesInRange(sheet,
					RangePosition.from(PLATZ_SPALTE, ERSTE_DATEN_ZEILE, PLATZ_SPALTE, letzteZeilePlatz),
					CellProperties.from()
							.setBorder(BorderFactory.from().allThin().boldLn().forTop().forRight().toBorder())
							.setCharWeight(com.sun.star.awt.FontWeight.BOLD));
		}

		// Zebra-Formatierung für Datenbereich
		if (!sortiert.isEmpty()) {
			int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
			RangePosition datenRange = RangePosition.from(TEAM_NR_SPALTE, ERSTE_DATEN_ZEILE,
					PUNKTE_DIFF_SPALTE, letzteZeile);
			RanglisteGeradeUngeradeFormatHelper.from(this, datenRange)
					.geradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeGerade())
					.ungeradeFarbe(getKonfigurationSheet().getRanglisteHintergrundFarbeUnGerade())
					.validateSpalte(validateSpalte())
					.apply();
		}

		// Druckbereich: Header + Daten, ohne Validator-Spalte
		int letzteZeile = sortiert.isEmpty() ? ZWEITE_HEADER_ZEILE
				: ERSTE_DATEN_ZEILE + sortiert.size() - 1;
		setzeDruckbereich(sheet, letzteZeile);

		getSheetHelper().setActiveSheet(sheet);
	}

	private List<TeamRanglisteData> leseAlleSpielergebnisse(TeamMeldungen aktiveMeldungen, int bisSpielrunde,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {

		Map<Integer, int[]> statsMap = new HashMap<>(); // teamNr → [0]=siege, [1]=punkte+, [2]=punkte-
		Map<Integer, List<Integer>> gegnerMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[3]);
			gegnerMap.put(team.getNr(), new ArrayList<>());
		}

		for (int runde = 1; runde <= bisSpielrunde; runde++) {
			SheetRunner.testDoCancelTask();
			String rundeSheetName = runde + ". " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN;
			XSpreadsheet rundeSheet = getSheetHelper().findByName(rundeSheetName);
			if (rundeSheet == null) {
				continue;
			}
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

		// Lese ab TEAM_A_SPALTE(1) bis ERG_TEAM_B_SPALTE(4), ab ERSTE_DATEN_ZEILE(2)
		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 999);
		RangeData rowsData = RangeHelper
				.from(rundeSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		for (RowData row : rowsData) {
			if (row.size() < 2) break;

			int nrA = resolveTeamNr(row.get(0), meldeliste);
			if (nrA <= 0) break; // Ende der Daten
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

			gegnerMap.computeIfAbsent(nrA, k -> new ArrayList<>()).add(nrB);
			gegnerMap.computeIfAbsent(nrB, k -> new ArrayList<>()).add(nrA);

			int ergA = (row.size() > 2) ? row.get(2).getIntVal(0) : 0;
			int ergB = (row.size() > 3) ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
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
				{ TEAM_NR_SPALTE,      COL_WIDTH_NR   },
				{ TEAM_NAME_SPALTE,    COL_WIDTH_NAME  },
				{ SIEGE_SPALTE,        COL_WIDTH_DATA  },
				{ BHZ_SPALTE,          ohneBuchholz ? 0 : COL_WIDTH_DATA },
				{ FBHZ_SPALTE,         ohneBuchholz ? 0 : COL_WIDTH_DATA },
				{ PUNKTE_PLUS_SPALTE,  COL_WIDTH_DATA  },
				{ PUNKTE_MINUS_SPALTE, COL_WIDTH_DATA  },
				{ PUNKTE_DIFF_SPALTE,  COL_WIDTH_DATA  },
		};
		for (int[] sw : spaltenBreiten) {
			getSheetHelper().setColumnProperties(sheet, sw[0],
					ColumnProperties.from().setWidth(sw[1])
							.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER));
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
		String nameSpalteHeader = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen() ? "Teamname" : "Team";
		String[] einzelTexte = { "Platz", "Nr", nameSpalteHeader, "Siege", "BHZ", "FBHZ" };
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
					.setEndPosMergeZeilePlus(1);  // vertikal Row 0 + Row 1
			if (col == PLATZ_SPALTE) {
				cv.setRotate90().setCharWeight(com.sun.star.awt.FontWeight.BOLD)
						.setVertJustify(CellVertJustify2.CENTER);
			}
			getSheetHelper().setStringValueInCell(cv);
		}

		// ── Zeile 0: "Punkte" horizontal über 3 Spalten zusammengeführt ──────────
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(PUNKTE_PLUS_SPALTE, HEADER_ZEILE), "Punkte")
				.setCellBackColor(headerColor)
				.setBorder(BorderFactory.from().allThin().toBorder())
				.setHoriJustify(CellHoriJustify.CENTER)
				.setEndPosMergeSpalte(PUNKTE_DIFF_SPALTE));  // horizontal G–I

		// ── Zeile 1: Sub-Header für die Punkte-Spalten ───────────────────────────
		String[] subTexte = { "+", "-", "Δ" };
		int[] subCols    = { PUNKTE_PLUS_SPALTE, PUNKTE_MINUS_SPALTE, PUNKTE_DIFF_SPALTE };
		for (int i = 0; i < subCols.length; i++) {
			getSheetHelper().setStringValueInCell(StringCellValue
					.from(sheet, Position.from(subCols[i], ZWEITE_HEADER_ZEILE), subTexte[i])
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().boldLn().forBottom().toBorder())
					.setHoriJustify(CellHoriJustify.CENTER));
		}
	}

	/**
	 * Schreibt TeamNr in sortierter Reihenfolge.
	 * Namens-Spalte als Formel mit Verweis auf die Meldeliste:
	 * <ul>
	 *   <li>NR-Modus: alle Spielernamen des Teams (Vorname Nachname, getrennt durch " / ")</li>
	 *   <li>NAME-Modus: Teamname aus der Teamname-Spalte der Meldeliste</li>
	 * </ul>
	 * Änderungen in der Meldeliste werden dadurch automatisch übernommen.
	 */
	private void insertDaten(XSpreadsheet sheet, List<SchweizerTeamErgebnis> sortiert,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {

		int zeile = ERSTE_DATEN_ZEILE;
		for (SchweizerTeamErgebnis erg : sortiert) {
			schreibeZahl(sheet, zeile, TEAM_NR_SPALTE, erg.teamNr());
			zeile++;
		}

		if (!sortiert.isEmpty()) {
			int letzteZeile = ERSTE_DATEN_ZEILE + sortiert.size() - 1;
			String formel = getKonfigurationSheet().isMeldeListeTeamnameAnzeigen()
					? erstelleTeamnameFormel(meldeliste)
					: erstelleSpielerNamenFormel(meldeliste);
			schreibeFormel(sheet, TEAM_NAME_SPALTE, formel, letzteZeile);
			getSheetHelper().setPropertiesInRange(sheet,
					RangePosition.from(TEAM_NAME_SPALTE, ERSTE_DATEN_ZEILE, TEAM_NAME_SPALTE, letzteZeile),
					CellProperties.from().setHoriJustify(CellHoriJustify.LEFT));
		}
	}

	/**
	 * Erstellt eine INDEX/MATCH-Formel, die den Teamnamen aus der Meldeliste anhand der TeamNr
	 * in der aktuellen Zeile nachschlägt (NAME-Modus).
	 */
	private String erstelleTeamnameFormel(SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {
		String mlSheetName = MeldeListeKonstanten.SHEETNAME;
		int mlErsteZeile   = meldeliste.getErsteDatenZiele() + 1; // 0-basiert → 1-basiert

		String mlNrCol   = spaltenBuchstabe(0);
		String mlNameCol = spaltenBuchstabe(meldeliste.getTeamnameSpalte());

		String mlNrRange   = mlBereich(mlSheetName, mlNrCol,   mlErsteZeile);
		String mlNameRange = mlBereich(mlSheetName, mlNameCol, mlErsteZeile);
		String teamNrRef   = teamNrIndirect();

		return "IFERROR(INDEX(" + mlNameRange + ";MATCH(" + teamNrRef + ";" + mlNrRange + ";0));\"\")";
	}

	/**
	 * Erstellt eine Formel, die alle Spielernamen des Teams als
	 * "Vorname Nachname / Vorname Nachname / ..." aus der Meldeliste liest (NR-Modus).
	 */
	private String erstelleSpielerNamenFormel(SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {
		String mlSheetName = MeldeListeKonstanten.SHEETNAME;
		int mlErsteZeile   = meldeliste.getErsteDatenZiele() + 1; // 0-basiert → 1-basiert
		int anzSpieler     = getKonfigurationSheet().getMeldeListeFormation().getAnzSpieler();

		String mlNrRange = mlBereich(mlSheetName, spaltenBuchstabe(0), mlErsteZeile);
		String matchExpr = "MATCH(" + teamNrIndirect() + ";" + mlNrRange + ";0)";

		StringBuilder sb = new StringBuilder();
		for (int s = 0; s < anzSpieler; s++) {
			String vCol = spaltenBuchstabe(meldeliste.getVornameSpalte(s));
			String nCol = spaltenBuchstabe(meldeliste.getNachnameSpalte(s));
			String vRange = mlBereich(mlSheetName, vCol, mlErsteZeile);
			String nRange = mlBereich(mlSheetName, nCol, mlErsteZeile);

			// "Vorname Nachname" für Spieler s, leer wenn nicht gefunden
			String spieler = "TRIM(IFERROR(INDEX(" + vRange + ";" + matchExpr + ");\"\")"
					+ "&\" \"&IFERROR(INDEX(" + nRange + ";" + matchExpr + ");\"\")" + ")";

			if (s > 0) {
				sb.append("&IF(LEN(").append(spieler).append(")>0;\"/\"&").append(spieler).append(";\"\")");
			} else {
				sb.append(spieler);
			}
		}

		return "IFERROR(TRIM(" + sb + ");\"\")";
	}

	/** Hilfsmethode: absoluter Meldelisten-Bereichs-String für eine Spalte. */
	private static String mlBereich(String sheetName, String spalte, int ersteZeile) {
		return "$'" + sheetName + "'.$" + spalte + "$" + ersteZeile
				+ ":$" + spalte + "$" + (ersteZeile + 999);
	}

	/** Hilfsmethode: INDIRECT-Ausdruck für TeamNr in der aktuellen Ranglisten-Zeile. */
	private static String teamNrIndirect() {
		return "INDIRECT(ADDRESS(ROW();" + (TEAM_NR_SPALTE + 1) + ";4))";
	}

	/**
	 * Schreibt Siege, BHZ, FBHZ, Punkte+, Punkte- und Differenz als Sheet-Formeln,
	 * die direkt auf die Spielrunden-Sheets verweisen.
	 * Damit aktualisieren sich die Werte automatisch bei Ergebnisänderungen.
	 */
	/** Wandelt 0-basierten Spaltenindex in Spaltenbuchstabe(n) um (z.B. 0→"A", 25→"Z", 26→"AA"). */
	private static String spaltenBuchstabe(int spalteIndex) {
		StringBuilder sb = new StringBuilder();
		int n = spalteIndex;
		do {
			sb.insert(0, (char) ('A' + (n % 26)));
			n = n / 26 - 1;
		} while (n >= 0);
		return sb.toString();
	}

	private void insertFormeln(XSpreadsheet sheet, int anzTeams, int bisSpielrunde, SchweizerRankingModus modus)
			throws GenerateException {

		int letzteZeile = ERSTE_DATEN_ZEILE + anzTeams - 1;

		// 1-indizierte Zeilennummern für Adressformeln
		int dzSr = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 1; // Spielrunde: erste Datenzeile (1-indiziert)
		int dzRl = ERSTE_DATEN_ZEILE + 1;                                   // Rangliste:  erste Datenzeile (1-indiziert)

		// Spaltenbuchstaben aus Konstanten ableiten (Spielrunde)
		String colTa = spaltenBuchstabe(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE);
		String colTb = spaltenBuchstabe(SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE);
		String colEa = spaltenBuchstabe(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE);
		String colEb = spaltenBuchstabe(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE);

		// Spaltenbuchstaben aus Konstanten ableiten (Rangliste)
		String colNr   = spaltenBuchstabe(TEAM_NR_SPALTE);
		String colSieg = spaltenBuchstabe(SIEGE_SPALTE);
		String colBhz  = spaltenBuchstabe(BHZ_SPALTE);

		// Referenz auf TeamNr in der aktuellen Zeile (1-indizierte Spalte)
		String teamNrRef = "INDIRECT(ADDRESS(ROW();" + (TEAM_NR_SPALTE + 1) + ";4))";

		// Rangliste-Ranges (absolut, für BHZ/FBHZ-Lookup)
		String rlNr   = "$" + colNr   + "$" + dzRl + ":$" + colNr   + "$" + (dzRl + 999);
		String rlSieg = "$" + colSieg + "$" + dzRl + ":$" + colSieg + "$" + (dzRl + 999);
		String rlBHZ  = "$" + colBhz  + "$" + dzRl + ":$" + colBhz  + "$" + (dzRl + 999);

		StringBuilder siegeF = new StringBuilder();
		StringBuilder ppF    = new StringBuilder(); // Punkte+
		StringBuilder pmF    = new StringBuilder(); // Punkte-
		StringBuilder bhzF   = new StringBuilder();
		StringBuilder fbhzF  = new StringBuilder();

		for (int r = 1; r <= bisSpielrunde; r++) {
			String shRef = "$'" + r + ". " + SchweizerAbstractSpielrundeSheet.SHEET_NAMEN + "'.";
			String tA = shRef + "$" + colTa + "$" + dzSr + ":$" + colTa + "$" + (dzSr + 999);
			String tB = shRef + "$" + colTb + "$" + dzSr + ":$" + colTb + "$" + (dzSr + 999);
			String eA = shRef + "$" + colEa + "$" + dzSr + ":$" + colEa + "$" + (dzSr + 999);
			String eB = shRef + "$" + colEb + "$" + dzSr + ":$" + colEb + "$" + (dzSr + 999);

			String sep = r > 1 ? "+" : "";

			// SIEGE: Gewinner-Paarungen (ergA > ergB oder ergB > ergA) + Freilos (tB=0)
			siegeF.append(sep)
					.append("IFERROR(SUMPRODUCT((").append(tA).append("=").append(teamNrRef).append(")*(")
					.append(eA).append(">").append(eB).append("));0)")
					.append("+IFERROR(SUMPRODUCT((").append(tB).append("=").append(teamNrRef).append(")*(")
					.append(eB).append(">").append(eA).append("));0)")
					.append("+IFERROR(SUMPRODUCT((").append(tA).append("=").append(teamNrRef).append(")*(")
					.append(tB).append("=0));0)");

			// PUNKTE+: eigene Ergebnisse summieren
			ppF.append(sep)
					.append("IFERROR(SUMPRODUCT((").append(tA).append("=").append(teamNrRef).append(")*")
					.append(eA).append(");0)")
					.append("+IFERROR(SUMPRODUCT((").append(tB).append("=").append(teamNrRef).append(")*")
					.append(eB).append(");0)");

			// PUNKTE-: Gegner-Ergebnisse summieren
			pmF.append(sep)
					.append("IFERROR(SUMPRODUCT((").append(tA).append("=").append(teamNrRef).append(")*")
					.append(eB).append(");0)")
					.append("+IFERROR(SUMPRODUCT((").append(tB).append("=").append(teamNrRef).append(")*")
					.append(eA).append(");0)");

			// BHZ: Summe der Siege aller Gegner (via INDEX/MATCH auf Rangliste-SIEGE-Spalte)
			bhzF.append(sep)
					.append("IFERROR(SUMPRODUCT((").append(tA).append("=").append(teamNrRef)
					.append(")*IFERROR(INDEX(").append(rlSieg).append(";MATCH(").append(tB).append(";")
					.append(rlNr).append(";0));0));0)")
					.append("+IFERROR(SUMPRODUCT((").append(tB).append("=").append(teamNrRef)
					.append(")*IFERROR(INDEX(").append(rlSieg).append(";MATCH(").append(tA).append(";")
					.append(rlNr).append(";0));0));0)");

			// FBHZ: Summe der BHZ aller Gegner (via INDEX/MATCH auf Rangliste-BHZ-Spalte)
			fbhzF.append(sep)
					.append("IFERROR(SUMPRODUCT((").append(tA).append("=").append(teamNrRef)
					.append(")*IFERROR(INDEX(").append(rlBHZ).append(";MATCH(").append(tB).append(";")
					.append(rlNr).append(";0));0));0)")
					.append("+IFERROR(SUMPRODUCT((").append(tB).append("=").append(teamNrRef)
					.append(")*IFERROR(INDEX(").append(rlBHZ).append(";MATCH(").append(tA).append(";")
					.append(rlNr).append(";0));0));0)");
		}

		// PUNKTE_DIFF = Punkte+ - Punkte- (relative Referenz auf aktuelle Zeile)
		String diffF = "INDIRECT(ADDRESS(ROW();" + (PUNKTE_PLUS_SPALTE + 1) + ";4))"
				+ "-INDIRECT(ADDRESS(ROW();" + (PUNKTE_MINUS_SPALTE + 1) + ";4))";

		schreibeFormel(sheet, SIEGE_SPALTE,        siegeF.toString(), letzteZeile);
		schreibeFormel(sheet, PUNKTE_PLUS_SPALTE,  ppF.toString(),    letzteZeile);
		schreibeFormel(sheet, PUNKTE_MINUS_SPALTE, pmF.toString(),    letzteZeile);
		schreibeFormel(sheet, PUNKTE_DIFF_SPALTE,  diffF,             letzteZeile);

		if (modus != SchweizerRankingModus.OHNE_BUCHHOLZ) {
			schreibeFormel(sheet, BHZ_SPALTE,  bhzF.toString(),  letzteZeile);
			schreibeFormel(sheet, FBHZ_SPALTE, fbhzF.toString(), letzteZeile);
		}
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
		return PUNKTE_DIFF_SPALTE;
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
		return VALIDATE_SPALTE;
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

	private void schreibeFormel(XSpreadsheet sheet, int spalte, String formel, int bisZeile)
			throws GenerateException {
		getSheetHelper().setFormulaInCell(StringCellValue
				.from(sheet, Position.from(spalte, ERSTE_DATEN_ZEILE))
				.setValue(formel)
				.setFillAutoDown(bisZeile));
		// fillAutoDown überträgt keine Formatierung – Border explizit auf gesamten Bereich setzen
		getSheetHelper().setPropertiesInRange(sheet,
				RangePosition.from(spalte, ERSTE_DATEN_ZEILE, spalte, bisZeile),
				CellProperties.from().setAllThinBorder());
	}

	private void schreibeZahl(XSpreadsheet sheet, int zeile, int spalte, int wert) throws GenerateException {
		var cv = NumberCellValue.from(sheet, Position.from(spalte, zeile)).setValue(wert);
		if (spalte == TEAM_NR_SPALTE) {
			cv.setCharColor(ColorHelper.CHAR_COLOR_GRAY_SPIELER_NR)
					.setBorder(BorderFactory.from().allThin().doubleLn().forRight().toBorder());
		} else {
			cv.setAllThinBorder();
		}
		getSheetHelper().setNumberValueInCell(cv);
	}

	private void schreibeText(XSpreadsheet sheet, int zeile, int spalte, String text) throws GenerateException {
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(spalte, zeile), text)
				.setAllThinBorder()
				.setHoriJustify(CellHoriJustify.LEFT));
	}

	/** Setzt den Druckbereich: Spalten A–I (ohne Validator-Spalte J), Zeilen 1 bis letzteZeile. */
	private void setzeDruckbereich(XSpreadsheet sheet, int letzteZeile) throws GenerateException {
		var linksOben  = Position.from(TEAM_NR_SPALTE, HEADER_ZEILE);
		var rechtsUnten = Position.from(PUNKTE_DIFF_SPALTE, letzteZeile);
		PrintArea.from(sheet, getWorkingSpreadsheet())
				.setPrintArea(RangePosition.from(linksOben, rechtsUnten));
	}
}
