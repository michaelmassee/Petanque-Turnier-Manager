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

import de.petanqueturniermanager.algorithmen.schweizer.FreilosBuchholz;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
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
 *       zählen dabei nicht als Gegner (analog {@code SchweizerRanglisteSheet.leseRundeEin}).
 *       Bei Freilos-Buchholz NEUTRAL je Freilos ⌊(r − 1) / 2⌋, r = Runden mit eingetragenem Ergebnis.</li>
 *   <li>FBHZ (Feinbuchholz) = Summe der BHZ-Werte aller Gegner, bei NEUTRAL je Freilos
 *       eigene Siege + (r − 1) · r / 2.</li>
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
	 * Prüft die Rangliste-Werte und deren Sortierung mit den Einstellungen aus der Konfiguration (Freispiel-Punkte,
	 * Ranking-Modus, Freilos-Buchholz). Bei {@link SchweizerRankingModus#OHNE_BUCHHOLZ} entfallen BHZ/FBHZ im
	 * Wertvergleich und im Sortierschlüssel (Siege ↓ → Punktedifferenz ↓ → Punkte+ ↓).
	 *
	 * @param ranglisteSheetName sichtbarer Name des Rangliste-Sheets (z.B. {@link SheetNamen#rangliste()}
	 *                           oder {@link SheetNamen#maastrichterVorrundenRangliste()})
	 * @param anzRunden          Anzahl der auszuwertenden Spielrunden ("1. Spielrunde" … "N. Spielrunde")
	 * @param konfig             Konfiguration des Turniers (Schweizer oder Maastrichter)
	 */
	public void pruefe(String ranglisteSheetName, int anzRunden, SchweizerKonfigurationSheet konfig)
			throws GenerateException {
		RundenStats rundenStats = leseSpielrunden(anzRunden, konfig.getFreispielPunktePlus(),
				konfig.getFreispielPunkteMinus());
		var freilos = new FreilosBuchholz(konfig.getFreilosBuchholzModus(), rundenStats.anzGewerteteRunden());
		Map<Integer, Double> bhzMap = berechneBuchholz(rundenStats.statsProTeam(), freilos);
		Map<Integer, Double> fbhzMap = berechneFeinbuchholz(rundenStats.statsProTeam(), bhzMap, freilos);
		boolean ohneBuchholz = konfig.getRankingModus() == SchweizerRankingModus.OHNE_BUCHHOLZ;
		pruefeRanglisteKorrektheit(ranglisteSheetName, rundenStats.statsProTeam(), bhzMap, fbhzMap, ohneBuchholz);
	}

	// ----- Spielrunden lesen -----

	private RundenStats leseSpielrunden(int anzRunden, int freispielPlus, int freispielMinus) {
		Map<Integer, TeamStats> statsProTeam = new HashMap<>();
		int anzGewerteteRunden = 0;

		for (int rundeNr = 1; rundeNr <= anzRunden; rundeNr++) {
			String sheetName = SheetNamen.spielrunde(rundeNr);
			XSpreadsheet sheet = sheetHelper.findByName(sheetName);
			assertThat(sheet).as("Spielrunden-Sheet '%s' muss existieren", sheetName).isNotNull();
			if (verarbeiteSpielrundeSheet(sheet, statsProTeam, freispielPlus, freispielMinus)) {
				anzGewerteteRunden++;
			}
		}
		return new RundenStats(statsProTeam, anzGewerteteRunden);
	}

	/**
	 * @return {@code true}, wenn die Runde mindestens ein eingetragenes Ergebnis hat; nur dann zählen ihre Freilose
	 *         für den Freilos-Buchholz
	 */
	private boolean verarbeiteSpielrundeSheet(XSpreadsheet sheet, Map<Integer, TeamStats> statsProTeam,
			int freispielPlus, int freispielMinus) {
		RangeData data = RangeHelper.from(sheet, xDoc,
				RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
						SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
						SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + MAX_DATEN_ZEILEN - 1))
				.getDataFromRange();

		boolean hatErgebnis = false;
		List<TeamStats> freilosTeams = new ArrayList<>();
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
				freilosTeams.add(statsA);
				continue;
			}

			int ergA = row.size() > 2 ? row.get(2).getIntVal(0) : 0;
			int ergB = row.size() > 3 ? row.get(3).getIntVal(0) : 0;

			if (ergA > 0 || ergB > 0) {
				hatErgebnis = true;
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
		if (hatErgebnis) {
			freilosTeams.forEach(stats -> stats.anzFreilose++);
		}
		return hatErgebnis;
	}

	// ----- BHZ / FBHZ -----

	private Map<Integer, Double> berechneBuchholz(Map<Integer, TeamStats> statsProTeam, FreilosBuchholz freilos) {
		Map<Integer, Double> bhzMap = new HashMap<>();
		statsProTeam.forEach((nr, stats) -> bhzMap.put(nr, stats.gegnerNrn.stream()
				.mapToInt(gegnerNr -> statsProTeam.getOrDefault(gegnerNr, new TeamStats()).siege).sum()
				+ stats.anzFreilose * freilos.bhzBeitrag()));
		return bhzMap;
	}

	private Map<Integer, Double> berechneFeinbuchholz(Map<Integer, TeamStats> statsProTeam,
			Map<Integer, Double> bhzMap, FreilosBuchholz freilos) {
		Map<Integer, Double> fbhzMap = new HashMap<>();
		statsProTeam.forEach((nr, stats) -> fbhzMap.put(nr, stats.gegnerNrn.stream()
				.mapToDouble(gegnerNr -> bhzMap.getOrDefault(gegnerNr, 0.0)).sum()
				+ stats.anzFreilose * freilos.fbhzBeitrag(stats.siege)));
		return fbhzMap;
	}

	// ----- Rangliste-Korrektheit -----

	private void pruefeRanglisteKorrektheit(String ranglisteSheetName, Map<Integer, TeamStats> statsProTeam,
			Map<Integer, Double> bhzMap, Map<Integer, Double> fbhzMap, boolean ohneBuchholz)
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
			double erwBhz = bhzMap.getOrDefault(nr, 0.0);
			double erwFbhz = fbhzMap.getOrDefault(nr, 0.0);
			int erwPlus = stats.punktePlus;
			int erwMinus = stats.punkteMinus;
			int erwDiff = erwPlus - erwMinus;

			int istSiege = row.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(Integer.MIN_VALUE);
			double istBhz = row.get(SchweizerRanglisteSheet.BHZ_SPALTE).getDoubleVal(Double.NaN);
			double istFbhz = row.get(SchweizerRanglisteSheet.FBHZ_SPALTE).getDoubleVal(Double.NaN);
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
						.thenComparingDouble(e -> -e.bhz())
						.thenComparingDouble(e -> -e.fbhz())
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
		int anzFreilose;
		final List<Integer> gegnerNrn = new ArrayList<>();
	}

	private record RundenStats(Map<Integer, TeamStats> statsProTeam, int anzGewerteteRunden) {
	}

	record RanglisteEintrag(int nr, int siege, double bhz, double fbhz, int diff, int punktePlus) {
	}
}
