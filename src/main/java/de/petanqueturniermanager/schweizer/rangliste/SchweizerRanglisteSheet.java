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
import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.border.BorderFactory;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Erstellt die Rangliste für das Schweizer Turniersystem.
 * <p>
 * Liest alle vorhandenen Spielrunden-Sheets ein und berechnet pro Team:
 * Siege, BHZ (Buchholz), FBHZ (Feinbuchholz), Punkte+, Punkte-, Differenz.
 * <p>
 * Sortierkriterien (Schweizer System):
 * <ol>
 *   <li>Siege (absteigend)</li>
 *   <li>BHZ = Summe der Siege aller Gegner (absteigend)</li>
 *   <li>FBHZ = Summe der BHZ-Werte aller Gegner (absteigend)</li>
 *   <li>Punktedifferenz (absteigend)</li>
 * </ol>
 */
public class SchweizerRanglisteSheet extends SchweizerSheet implements ISheet {

	private static final Logger LOGGER = LogManager.getLogger(SchweizerRanglisteSheet.class);

	public static final String SHEETNAME = "Rangliste";
	public static final String SHEET_COLOR = "d637e8";

	public static final int HEADER_ZEILE          = 0;
	public static final int ERSTE_DATEN_ZEILE     = 1;

	public static final int PLATZ_SPALTE          = 0;  // A
	public static final int TEAM_NR_SPALTE        = 1;  // B
	public static final int TEAM_NAME_SPALTE      = 2;  // C
	public static final int SIEGE_SPALTE          = 3;  // D
	public static final int BHZ_SPALTE            = 4;  // E
	public static final int FBHZ_SPALTE           = 5;  // F
	public static final int PUNKTE_PLUS_SPALTE    = 6;  // G
	public static final int PUNKTE_MINUS_SPALTE   = 7;  // H
	public static final int PUNKTE_DIFF_SPALTE    = 8;  // I

	private static final int COL_WIDTH_NR   = 800;
	private static final int COL_WIDTH_NAME = 7000;
	private static final int COL_WIDTH_DATA = 1400;

	/** Hält die erweiterten Auswertungsdaten für die Ranglisten-Anzeige. */
	private record TeamRanglisteData(int teamNr, int siege, int punktePlus, int punkteMinus,
			List<Integer> gegnerNrn) {

		int punkteDiff() {
			return punktePlus - punkteMinus;
		}

		SchweizerTeamErgebnis toErgebnis() {
			return new SchweizerTeamErgebnis(teamNr, siege, punkteDiff(), gegnerNrn);
		}
	}

	public SchweizerRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
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
		List<TeamRanglisteData> ranglisteData = leseAlleSpielergebnisse(aktiveMeldungen, bisSpielrunde, meldeliste);

		List<SchweizerTeamErgebnis> ergebnisse = ranglisteData.stream()
				.map(TeamRanglisteData::toErgebnis)
				.collect(Collectors.toList());
		List<SchweizerTeamErgebnis> sortiert = new SchweizerSystem().sortiereNachAuswertungskriterien(ergebnisse);

		// BHZ (Buchholz) = Summe der Siege aller Gegner
		Map<Integer, Integer> siegeMap = ergebnisse.stream()
				.collect(Collectors.toMap(SchweizerTeamErgebnis::teamNr, SchweizerTeamErgebnis::siege));
		Map<Integer, Integer> bhzMap = new HashMap<>();
		for (SchweizerTeamErgebnis e : sortiert) {
			int bhz = e.gegnerNrn().stream().mapToInt(gnr -> siegeMap.getOrDefault(gnr, 0)).sum();
			bhzMap.put(e.teamNr(), bhz);
		}

		// FBHZ (Feinbuchholz) = Summe der BHZ-Werte aller Gegner
		Map<Integer, Integer> fbhzMap = new HashMap<>();
		for (SchweizerTeamErgebnis e : sortiert) {
			int fbhz = e.gegnerNrn().stream().mapToInt(gnr -> bhzMap.getOrDefault(gnr, 0)).sum();
			fbhzMap.put(e.teamNr(), fbhz);
		}

		Map<Integer, TeamRanglisteData> dataByNr = ranglisteData.stream()
				.collect(Collectors.toMap(TeamRanglisteData::teamNr, d -> d));

		insertHeader(sheet);
		insertDaten(sheet, sortiert, dataByNr, bhzMap, fbhzMap, meldeliste);

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

	private void insertHeader(XSpreadsheet sheet) throws GenerateException {
		Integer headerColor = getKonfigurationSheet().getMeldeListeHeaderFarbe();

		String[] headers = { "Pl.", "Nr", "Name", "Siege", "BHZ", "FBHZ", "Punkte+", "Punkte-", "Diff" };
		int[] cols = { PLATZ_SPALTE, TEAM_NR_SPALTE, TEAM_NAME_SPALTE, SIEGE_SPALTE,
				BHZ_SPALTE, FBHZ_SPALTE, PUNKTE_PLUS_SPALTE, PUNKTE_MINUS_SPALTE, PUNKTE_DIFF_SPALTE };
		int[] widths = { COL_WIDTH_NR, COL_WIDTH_NR, COL_WIDTH_NAME,
				COL_WIDTH_DATA, COL_WIDTH_DATA, COL_WIDTH_DATA,
				COL_WIDTH_DATA, COL_WIDTH_DATA, COL_WIDTH_DATA };

		for (int i = 0; i < cols.length; i++) {
			ColumnProperties colProp = ColumnProperties.from().setWidth(widths[i])
					.setHoriJustify(CellHoriJustify.CENTER).setVertJustify(CellVertJustify2.CENTER);
			StringCellValue cv = StringCellValue
					.from(sheet, Position.from(cols[i], HEADER_ZEILE), headers[i])
					.addColumnProperties(colProp)
					.setCellBackColor(headerColor)
					.setBorder(BorderFactory.from().allThin().toBorder())
					.setHoriJustify(CellHoriJustify.CENTER);
			getSheetHelper().setStringValueInCell(cv);
		}
	}

	private void insertDaten(XSpreadsheet sheet, List<SchweizerTeamErgebnis> sortiert,
			Map<Integer, TeamRanglisteData> dataByNr,
			Map<Integer, Integer> bhzMap, Map<Integer, Integer> fbhzMap,
			SchweizerMeldeListeSheetUpdate meldeliste) throws GenerateException {

		int platz = 1;
		int zeile = ERSTE_DATEN_ZEILE;
		for (SchweizerTeamErgebnis erg : sortiert) {
			TeamRanglisteData data = dataByNr.get(erg.teamNr());
			if (data == null) continue;

			int bhz  = bhzMap.getOrDefault(erg.teamNr(), 0);
			int fbhz = fbhzMap.getOrDefault(erg.teamNr(), 0);
			String name = meldeliste.getTeamNameByNr(erg.teamNr());
			if (name == null) name = "";

			schreibeZahl(sheet, zeile, PLATZ_SPALTE,        platz);
			schreibeZahl(sheet, zeile, TEAM_NR_SPALTE,      erg.teamNr());
			schreibeText(sheet, zeile, TEAM_NAME_SPALTE,    name);
			schreibeZahl(sheet, zeile, SIEGE_SPALTE,        erg.siege());
			schreibeZahl(sheet, zeile, BHZ_SPALTE,          bhz);
			schreibeZahl(sheet, zeile, FBHZ_SPALTE,         fbhz);
			schreibeZahl(sheet, zeile, PUNKTE_PLUS_SPALTE,  data.punktePlus());
			schreibeZahl(sheet, zeile, PUNKTE_MINUS_SPALTE, data.punkteMinus());
			schreibeZahl(sheet, zeile, PUNKTE_DIFF_SPALTE,  data.punkteDiff());

			platz++;
			zeile++;
		}
	}

	private void schreibeZahl(XSpreadsheet sheet, int zeile, int spalte, int wert) throws GenerateException {
		getSheetHelper().setNumberValueInCell(
				NumberCellValue.from(sheet, Position.from(spalte, zeile)).setValue(wert));
	}

	private void schreibeText(XSpreadsheet sheet, int zeile, int spalte, String text) throws GenerateException {
		getSheetHelper().setStringValueInCell(StringCellValue
				.from(sheet, Position.from(spalte, zeile), text)
				.setHoriJustify(CellHoriJustify.LEFT));
	}
}
