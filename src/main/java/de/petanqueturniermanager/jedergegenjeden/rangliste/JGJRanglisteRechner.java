package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJSpielPlanSheet;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Liest den JGJ-Spielplan ein und berechnet die Ranglisten-Statistik je Team.
 * <p>
 * Ausgelagert aus {@link JGJRanglisteSheet}, damit sowohl die (gruppierte)
 * Einzel-Rangliste als auch die gruppenübergreifende {@link JGJGesamtranglisteSheet}
 * dieselbe Berechnung nutzen. Die reinen Kombinations-/Sortiermethoden sind
 * statisch und ohne LibreOffice testbar.
 */
public class JGJRanglisteRechner {

	/** Statistik eines Teams: Spiele (+/-) und Spielpunkte (+/-). */
	public record TeamStats(int teamNr, int spielePlus, int spieleMinus,
			int spielPunktePlus, int spielPunkteMinus) {

		public int spielDiff() {
			return spielePlus - spieleMinus;
		}

		public int spielPunkteDiff() {
			return spielPunktePlus - spielPunkteMinus;
		}
	}

	private static final int MAX_SPIELPLAN_ZEILEN = 500;

	private final WorkingSpreadsheet workingSpreadsheet;

	public JGJRanglisteRechner(WorkingSpreadsheet workingSpreadsheet) {
		this.workingSpreadsheet = workingSpreadsheet;
	}

	/**
	 * Sortierkriterien der JGJ-Rangliste: Spiele+ ↓, dann Spielpunkte-Differenz ↓,
	 * dann Spielpunkte+ ↓.
	 */
	public static Comparator<TeamStats> vergleicher() {
		return Comparator.comparingInt(TeamStats::spielePlus).reversed()
				.thenComparing(Comparator.comparingInt(TeamStats::spielPunkteDiff).reversed())
				.thenComparing(Comparator.comparingInt(TeamStats::spielPunktePlus).reversed());
	}

	/** Statistik der übergebenen Teams berechnen und nach {@link #vergleicher()} sortieren. */
	public List<TeamStats> berechneUndSortiere(TeamMeldungen aktiveMeldungen) throws GenerateException {
		Map<Integer, int[]> statsRaw = leseSpielplanStats(aktiveMeldungen);
		List<TeamStats> daten = new ArrayList<>();
		for (Team team : aktiveMeldungen.teams()) {
			int[] s = statsRaw.getOrDefault(team.getNr(), new int[4]);
			daten.add(new TeamStats(team.getNr(), s[0], s[1], s[2], s[3]));
		}
		daten.sort(vergleicher());
		return daten;
	}

	/**
	 * „Snake"-Kombination mehrerer bereits je Gruppe sortierter Listen: erst alle
	 * Erstplatzierten, dann alle Zweitplatzierten usw. Kürzere Gruppen werden ab
	 * ihrem Ende übersprungen (faire Reihenfolge auch bei ungleich großen Gruppen).
	 */
	public static List<TeamStats> snakeKombination(List<List<TeamStats>> sortierteGruppen) {
		List<TeamStats> ergebnis = new ArrayList<>();
		int maxLaenge = sortierteGruppen.stream().mapToInt(List::size).max().orElse(0);
		for (int rang = 0; rang < maxLaenge; rang++) {
			for (List<TeamStats> gruppe : sortierteGruppen) {
				if (rang < gruppe.size()) {
					ergebnis.add(gruppe.get(rang));
				}
			}
		}
		return ergebnis;
	}

	/**
	 * Liest den zentralen JGJ-Spielplan blockweise und summiert je Team:
	 * {@code [spielePlus, spieleMinus, spielpunktePlus, spielpunkteMinus]}.
	 */
	public Map<Integer, int[]> leseSpielplanStats(TeamMeldungen aktiveMeldungen) throws GenerateException {
		Map<Integer, int[]> statsMap = new HashMap<>();
		for (Team team : aktiveMeldungen.teams()) {
			statsMap.put(team.getNr(), new int[4]);
		}

		var xDoc = workingSpreadsheet.getWorkingSpreadsheetDocument();
		XSpreadsheet spielplanSheet = SheetMetadataHelper.findeSheetUndHeile(
				xDoc, SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN, JGJSpielPlanSheet.sheetName());
		if (spielplanSheet == null) {
			return statsMap;
		}

		int startZeile = JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		RangeData teamNrData = RangeHelper.from(spielplanSheet, xDoc,
				RangePosition.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE, startZeile,
						JGJSpielPlanSheet.TEAM_B_NR_SPALTE, startZeile + MAX_SPIELPLAN_ZEILEN))
				.getDataFromRange();

		RangeData spielpunkteData = RangeHelper.from(spielplanSheet, xDoc,
				RangePosition.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE, startZeile,
						JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, startZeile + MAX_SPIELPLAN_ZEILEN))
				.getDataFromRange();

		for (int i = 0; i < teamNrData.size(); i++) {
			RowData teamNrZeile = teamNrData.get(i);
			if (teamNrZeile.size() < 2) {
				break;
			}
			int nrA = teamNrZeile.get(0).getIntVal(0);
			if (nrA <= 0) {
				continue; // Gruppen-Header-Zeile oder Ende
			}
			int nrB = teamNrZeile.get(1).getIntVal(0);

			if (nrB <= 0) {
				var freispielStats = statsMap.computeIfAbsent(nrA, k -> new int[4]);
				freispielStats[0]++;
				RowData freispielPunkte = spielpunkteData.get(i);
				freispielStats[2] += freispielPunkte.size() > 0 ? freispielPunkte.get(0).getIntVal(0) : 0;
				freispielStats[3] += freispielPunkte.size() > 1 ? freispielPunkte.get(1).getIntVal(0) : 0;
				continue;
			}

			RowData punkteZeile = spielpunkteData.get(i);
			int pktA = punkteZeile.size() > 0 ? punkteZeile.get(0).getIntVal(0) : 0;
			int pktB = punkteZeile.size() > 1 ? punkteZeile.get(1).getIntVal(0) : 0;

			if (pktA <= 0 && pktB <= 0) {
				continue;
			}

			statsMap.computeIfAbsent(nrA, k -> new int[4])[2] += pktA;
			statsMap.computeIfAbsent(nrA, k -> new int[4])[3] += pktB;
			statsMap.computeIfAbsent(nrB, k -> new int[4])[2] += pktB;
			statsMap.computeIfAbsent(nrB, k -> new int[4])[3] += pktA;

			if (pktA > pktB) {
				statsMap.computeIfAbsent(nrA, k -> new int[4])[0]++;
				statsMap.computeIfAbsent(nrB, k -> new int[4])[1]++;
			} else if (pktB > pktA) {
				statsMap.computeIfAbsent(nrB, k -> new int[4])[0]++;
				statsMap.computeIfAbsent(nrA, k -> new int[4])[1]++;
			}
		}
		return statsMap;
	}
}
