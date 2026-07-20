package de.petanqueturniermanager.schweizer.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.assertj.core.api.SoftAssertions;

import com.google.common.base.Preconditions;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;

/**
 * Test-Hilfsklasse: assertiert die Rangliste-Werte (Siege, BHZ, FBHZ, Punkte+, Punkte-,
 * Punktedifferenz) einer Schweizer- oder Maastrichter-Vorrunden-Rangliste direkt gegen die
 * LibreOffice-Sheets (nicht gegen JSON-Snapshots) — unabhängig von
 * {@link de.petanqueturniermanager.algorithmen.schweizer.SchweizerSystem} nachgerechnet.
 *
 * <p>Prüft:
 * <ol>
 *   <li>Siege / Punkte+ / Punkte- / Punktedifferenz gegen die aus den Spielrunden-Sheets
 *       ("N. Spielrunde") aufsummierten Werte, inkl. Freilos-Buchung mit den konfigurierten
 *       Freispiel-Punkten.</li>
 *   <li>BHZ (Buchholz) = Summe der Siege aller Gegner. Ungespielte Paarungen (Ergebnis 0:0)
 *       zählen dabei nicht als Gegner (analog {@code SchweizerRanglisteSheet.leseRundeEin}).</li>
 *   <li>FBHZ (Feinbuchholz) = Summe der BHZ-Werte aller Gegner.</li>
 *   <li>Rangliste-Sortierung: Siege ↓ → BHZ ↓ → FBHZ ↓ → Punktedifferenz ↓ → Punkte+ ↓.</li>
 * </ol>
 *
 * <p>Funktioniert für beide Systeme, die {@link SchweizerRanglisteSheet} für den Aufbau
 * nutzen (Schweizer System und Maastrichter-Vorrunde) — der Rangliste-Sheetname und die
 * Freispiel-Punkte werden als Parameter übergeben.
 *
 * <p>SYNC-CHECK: Prüflogik muss synchron mit {@code tools/analyse_schweizer_rangliste.py}
 * bleiben.
 */
public class SchweizerRanglisteAnalyseAssert {

	private static final int MAX_DATEN_ZEILEN = 300;

	private final WorkingSpreadsheet ws;
	private final SheetHelper sheetHelper;
	private final XSpreadsheetDocument xDoc;

	private SchweizerRanglisteAnalyseAssert(WorkingSpreadsheet ws) {
		this.ws = Preconditions.checkNotNull(ws);
		this.xDoc = ws.getWorkingSpreadsheetDocument();
		this.sheetHelper = new SheetHelper(ws.getxContext(), xDoc);
	}

	public static SchweizerRanglisteAnalyseAssert fuer(WorkingSpreadsheet ws) {
		return new SchweizerRanglisteAnalyseAssert(ws);
	}

	/**
	 * Prüft die Rangliste-Werte und deren Sortierung.
	 *
	 * @param ranglisteSheetName sichtbarer Name des Rangliste-Sheets (z.B. {@link SheetNamen#rangliste()}
	 *                           oder {@link SheetNamen#maastrichterVorrundenRangliste()})
	 * @param anzRunden          Anzahl der auszuwertenden Spielrunden ("1. Spielrunde" … "N. Spielrunde")
	 * @param freispielPlus      konfigurierte Freispiel-Punkte+ (bei Freilos)
	 * @param freispielMinus     konfigurierte Freispiel-Punkte- (bei Freilos)
	 */
	public void pruefe(String ranglisteSheetName, int anzRunden, int freispielPlus, int freispielMinus)
			throws GenerateException {
		pruefe(ranglisteSheetName, anzRunden, freispielPlus, freispielMinus, SchweizerRankingModus.MIT_BUCHHOLZ);
	}

	/**
	 * Wie {@link #pruefe(String, int, int, int)}, aber mit explizitem Ranking-Modus: bei
	 * {@link SchweizerRankingModus#OHNE_BUCHHOLZ} entfallen BHZ/FBHZ im Wertvergleich und im
	 * Sortierschlüssel (Siege ↓ → Punktedifferenz ↓ → Punkte+ ↓), da
	 * {@code SchweizerRanglisteSheet} dort 0 in die BHZ/FBHZ-Spalten schreibt und
	 * {@code SchweizerSystem.sortiereNachAuswertungskriterien} ohne BHZ/FBHZ sortiert.
	 */
	public void pruefe(String ranglisteSheetName, int anzRunden, int freispielPlus, int freispielMinus,
			SchweizerRankingModus modus) throws GenerateException {
		Map<Integer, TeamStats> statsProTeam = leseSpielrunden(anzRunden, freispielPlus, freispielMinus);
		Map<Integer, Integer> bhzMap = berechneBuchholz(statsProTeam);
		Map<Integer, Integer> fbhzMap = berechneFeinbuchholz(statsProTeam, bhzMap);
		boolean ohneBuchholz = modus == SchweizerRankingModus.OHNE_BUCHHOLZ;
		pruefeRanglisteKorrektheit(ranglisteSheetName, statsProTeam, bhzMap, fbhzMap, ohneBuchholz);
	}

	// ----- Spielrunden lesen -----

	private Map<Integer, TeamStats> leseSpielrunden(int anzRunden, int freispielPlus, int freispielMinus) {
		Map<Integer, TeamStats> statsProTeam = new HashMap<>();

		for (int rundeNr = 1; rundeNr <= anzRunden; rundeNr++) {
			String sheetName = SheetNamen.spielrunde(rundeNr);
			XSpreadsheet sheet = sheetHelper.findByName(sheetName);
			assertThat(sheet).as("Spielrunden-Sheet '%s' muss existieren", sheetName).isNotNull();
			verarbeiteSpielrundeSheet(sheet, statsProTeam, freispielPlus, freispielMinus);
		}
		return statsProTeam;
	}

	private void verarbeiteSpielrundeSheet(XSpreadsheet sheet, Map<Integer, TeamStats> statsProTeam,
			int freispielPlus, int freispielMinus) {
		RangeData data = RangeHelper.from(sheet, xDoc,
				RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
						SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + MAX_DATEN_ZEILEN - 1))
				.getDataFromRange();

		for (RowData row : data) {
			if (row.size() < 2) {
				break;
			}
			int nrA = row.get(0).getIntVal(-1);
			if (nrA <= 0) {
				break; // Ende der Daten
			}
			int nrB = row.get(1).getIntVal(-1);

			if (nrB <= 0) {
				// Freilos für Team A: Sieg + konfigurierte Freispiel-Punkte, kein Gegner-Eintrag
				TeamStats statsA = statsProTeam.computeIfAbsent(nrA, k -> new TeamStats());
				statsA.siege++;
				statsA.punktePlus += freispielPlus;
				statsA.punkteMinus += freispielMinus;
				continue;
			}

			int ergA = row.size() > 2 ? row.get(2).getIntVal(0) : 0;
			int ergB = row.size() > 3 ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
				TeamStats statsA = statsProTeam.computeIfAbsent(nrA, k -> new TeamStats());
				TeamStats statsB = statsProTeam.computeIfAbsent(nrB, k -> new TeamStats());
				statsA.gegnerNrn.add(nrB);
				statsB.gegnerNrn.add(nrA);
				statsA.punktePlus += ergA;
				statsA.punkteMinus += ergB;
				statsB.punktePlus += ergB;
				statsB.punkteMinus += ergA;
				if (ergA > ergB) {
					statsA.siege++;
				} else if (ergB > ergA) {
					statsB.siege++;
				}
			}
		}
	}

	// ----- BHZ / FBHZ -----

	private Map<Integer, Integer> berechneBuchholz(Map<Integer, TeamStats> statsProTeam) {
		Map<Integer, Integer> bhzMap = new HashMap<>();
		for (var entry : statsProTeam.entrySet()) {
			int bhz = entry.getValue().gegnerNrn.stream()
					.mapToInt(gegnerNr -> statsProTeam.getOrDefault(gegnerNr, new TeamStats()).siege)
					.sum();
			bhzMap.put(entry.getKey(), bhz);
		}
		return bhzMap;
	}

	private Map<Integer, Integer> berechneFeinbuchholz(Map<Integer, TeamStats> statsProTeam,
			Map<Integer, Integer> bhzMap) {
		Map<Integer, Integer> fbhzMap = new HashMap<>();
		for (var entry : statsProTeam.entrySet()) {
			int fbhz = entry.getValue().gegnerNrn.stream()
					.mapToInt(gegnerNr -> bhzMap.getOrDefault(gegnerNr, 0))
					.sum();
			fbhzMap.put(entry.getKey(), fbhz);
		}
		return fbhzMap;
	}

	// ----- Rangliste-Korrektheit -----

	private void pruefeRanglisteKorrektheit(String ranglisteSheetName, Map<Integer, TeamStats> statsProTeam,
			Map<Integer, Integer> bhzMap, Map<Integer, Integer> fbhzMap, boolean ohneBuchholz)
			throws GenerateException {
		XSpreadsheet sheet = sheetHelper.findByName(ranglisteSheetName);
		assertThat(sheet).as("Rangliste-Sheet '%s' muss existieren", ranglisteSheetName).isNotNull();

		RangeData data = RangeHelper.from(sheet, xDoc,
				RangePosition.from(SchweizerRanglisteSheet.TEAM_NR_SPALTE, SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE,
						SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE,
						SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE + MAX_DATEN_ZEILEN - 1))
				.getDataFromRange();

		var ranglisteEintraege = new ArrayList<RanglisteEintrag>();
		var softly = new SoftAssertions();

		for (RowData row : data) {
			int nr = row.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(-1);
			if (nr <= 0) {
				break;
			}
			TeamStats stats = statsProTeam.getOrDefault(nr, new TeamStats());
			int erwSiege = stats.siege;
			int erwBhz = bhzMap.getOrDefault(nr, 0);
			int erwFbhz = fbhzMap.getOrDefault(nr, 0);
			int erwPlus = stats.punktePlus;
			int erwMinus = stats.punkteMinus;
			int erwDiff = erwPlus - erwMinus;

			int istSiege = row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(Integer.MIN_VALUE);
			int istBhz = row.get(SchweizerRanglisteSheet.BHZ_SPALTE).getIntVal(Integer.MIN_VALUE);
			int istFbhz = row.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getIntVal(Integer.MIN_VALUE);
			int istPlus = row.get(SchweizerRanglisteSheet.PUNKTE_PLUS_SPALTE).getIntVal(Integer.MIN_VALUE);
			int istMinus = row.get(SchweizerRanglisteSheet.PUNKTE_MINUS_SPALTE).getIntVal(Integer.MIN_VALUE);
			int istDiff = row.get(SchweizerRanglisteSheet.PUNKTE_DIFF_SPALTE).getIntVal(Integer.MIN_VALUE);

			softly.assertThat(istSiege).as("Team #%d: Siege", nr).isEqualTo(erwSiege);
			if (!ohneBuchholz) {
				softly.assertThat(istBhz).as("Team #%d: BHZ", nr).isEqualTo(erwBhz);
				softly.assertThat(istFbhz).as("Team #%d: FBHZ", nr).isEqualTo(erwFbhz);
			}
			softly.assertThat(istPlus).as("Team #%d: Punkte+", nr).isEqualTo(erwPlus);
			softly.assertThat(istMinus).as("Team #%d: Punkte-", nr).isEqualTo(erwMinus);
			softly.assertThat(istDiff).as("Team #%d: Punktedifferenz", nr).isEqualTo(erwDiff);

			ranglisteEintraege.add(new RanglisteEintrag(nr, istSiege, istBhz, istFbhz, istDiff, istPlus));
		}

		softly.assertAll();
		pruefeRanglisteSortierung(ranglisteSheetName, ranglisteEintraege, ohneBuchholz);
	}

	// ----- Rangliste-Sortierung -----

	private void pruefeRanglisteSortierung(String ranglisteSheetName, List<RanglisteEintrag> eintraege,
			boolean ohneBuchholz) {
		Comparator<RanglisteEintrag> comparator = ohneBuchholz
				? Comparator.<RanglisteEintrag>comparingInt(e -> -e.siege())
						.thenComparingInt(e -> -e.diff())
						.thenComparingInt(e -> -e.punktePlus())
				: Comparator.<RanglisteEintrag>comparingInt(e -> -e.siege())
						.thenComparingInt(e -> -e.bhz())
						.thenComparingInt(e -> -e.fbhz())
						.thenComparingInt(e -> -e.diff())
						.thenComparingInt(e -> -e.punktePlus());

		var erwartet = eintraege.stream().sorted(comparator).map(RanglisteEintrag::nr).toList();
		var aktuell = eintraege.stream().map(RanglisteEintrag::nr).toList();
		String kriterien = ohneBuchholz ? "Siege↓ → Punktedifferenz↓ → Punkte+↓"
				: "Siege↓ → BHZ↓ → FBHZ↓ → Punktedifferenz↓ → Punkte+↓";
		assertThat(aktuell)
				.as("%s: Rangliste-Sortierung (%s)", ranglisteSheetName, kriterien)
				.isEqualTo(erwartet);
	}

	// ----- Hilfsdatenstrukturen -----

	private static final class TeamStats {
		int siege;
		int punktePlus;
		int punkteMinus;
		final List<Integer> gegnerNrn = new ArrayList<>();
	}

	record RanglisteEintrag(int nr, int siege, int bhz, int fbhz, int diff, int punktePlus) {
	}
}
