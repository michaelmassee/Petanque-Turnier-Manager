package de.petanqueturniermanager.jedergegenjeden.rangliste;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.JGJGruppenAufteiler;
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

	/**
	 * Gruppen für Rangliste, Gesamtrangliste und Finalrunde. Maßgeblich ist die Gruppeneinteilung des bestehenden
	 * Spielplans, gefiltert auf die aktiven Teams: Ein nachträglich deaktiviertes Team fällt nur aus seiner Gruppe
	 * heraus, die übrigen Teams bleiben in der Gruppe, in der sie gespielt haben. Eine Neuaufteilung der aktiven Teams
	 * würde Teams in fremde Gruppen verschieben. Nur ohne Spielplan werden die Gruppen wie beim Erstellen neu
	 * aufgeteilt.
	 *
	 * @return mindestens eine Gruppe; bei genau einer Gruppe alle aktiven Teams
	 */
	public List<TeamMeldungen> ermittleGruppen(TeamMeldungen aktiveMeldungen, int gruppengroesse)
			throws GenerateException {
		XSpreadsheet spielplanSheet = spielplanSheet();
		if (spielplanSheet == null) {
			boolean gruppenModus = gruppengroesse > 0 && aktiveMeldungen.size() > gruppengroesse;
			return gruppenModus ? JGJGruppenAufteiler.teileInGruppen(aktiveMeldungen, gruppengroesse)
					: List.of(aktiveMeldungen);
		}
		String[][] teamAFormeln = new SheetHelper(workingSpreadsheet).getFormulaArrayFromRange(spielplanSheet,
				teamNummernBereich(spielplanSheet));
		return gruppenAusSpielplan(spielplanGruppen(leseTeamNummern(spielplanSheet), teamAFormeln),
				aktiveMeldungen);
	}

	/**
	 * Ordnet die aktiven Teams den Spielplan-Gruppen zu. Bei höchstens einer Gruppe bleibt es bei allen aktiven Teams
	 * (auch nachgemeldeten ohne Partie). Im Gruppenmodus bleibt jede Gruppe erhalten, auch wenn alle ihre Teams
	 * ausgestiegen sind, damit die Gruppenbuchstaben stabil bleiben.
	 */
	static List<TeamMeldungen> gruppenAusSpielplan(List<List<Integer>> spielplanGruppen,
			TeamMeldungen aktiveMeldungen) {
		if (spielplanGruppen.size() <= 1) {
			return List.of(aktiveMeldungen);
		}
		List<TeamMeldungen> gruppen = new ArrayList<>();
		for (List<Integer> teamNrn : spielplanGruppen) {
			TeamMeldungen gruppe = new TeamMeldungen();
			for (int teamNr : teamNrn) {
				Team team = aktiveMeldungen.getTeam(teamNr);
				if (team != null) {
					gruppe.addTeamWennNichtVorhanden(team);
				}
			}
			gruppen.add(gruppe);
		}
		return gruppen;
	}

	/**
	 * Gruppenstruktur des Spielplans: Teamnummern je Gruppe in Spielplan-Reihenfolge. Eine Gruppe beginnt nach einer
	 * Kopfzeile; ein Spielplan ohne Gruppen ergibt genau eine Gruppe. Kopfzeilen tragen in der Team-A-Spalte den
	 * festen Wert 0, Partiezeilen eine Formel – so trennt auch ein gelöschtes Team (Formel ergibt 0) keine Gruppe.
	 *
	 * @param teamNrData   Spalten Team-A-Nr und Team-B-Nr ab der ersten Spieltag-Datenzeile
	 * @param teamAFormeln Formeln derselben Zeilen (Spalte 0 = Team A)
	 */
	static List<List<Integer>> spielplanGruppen(RangeData teamNrData, String[][] teamAFormeln) {
		List<Set<Integer>> gruppen = new ArrayList<>();
		boolean neueGruppe = true;
		for (int i = 0; i < teamNrData.size(); i++) {
			RowData zeile = teamNrData.get(i);
			if (zeile.size() < 2) {
				break;
			}
			int nrA = zeile.get(0).getIntVal(0);
			int nrB = zeile.get(1).getIntVal(0);
			if (nrA <= 0 && !istPartieZeile(teamAFormeln, i)) {
				neueGruppe = true; // Gruppen-Kopfzeile oder Leerzeile
				continue;
			}
			if (neueGruppe) {
				gruppen.add(new LinkedHashSet<>());
				neueGruppe = false;
			}
			Set<Integer> gruppe = gruppen.get(gruppen.size() - 1);
			if (nrA > 0) {
				gruppe.add(nrA);
			}
			if (nrB > 0) {
				gruppe.add(nrB);
			}
		}
		return gruppen.stream().map(List::copyOf).toList();
	}

	private static boolean istPartieZeile(String[][] teamAFormeln, int zeile) {
		return zeile < teamAFormeln.length && teamAFormeln[zeile].length > 0 && teamAFormeln[zeile][0] != null
				&& teamAFormeln[zeile][0].startsWith("=");
	}

	/** Statistik der übergebenen Teams berechnen und nach {@link #vergleicher()} sortieren. */
	public List<TeamStats> berechneUndSortiere(TeamMeldungen aktiveMeldungen) throws GenerateException {
		Map<Integer, int[]> statsRaw = leseSpielplanStats(aktiveMeldungen);
		return sortiereMitStats(aktiveMeldungen, statsRaw);
	}

	/**
	 * Statistik einmal für alle Gruppen lesen und anschließend jede Gruppe separat
	 * sortieren.
	 */
	public List<List<TeamStats>> berechneUndSortiereGruppen(List<TeamMeldungen> gruppen) throws GenerateException {
		TeamMeldungen alle = new TeamMeldungen();
		for (TeamMeldungen gruppe : gruppen) {
			gruppe.teams().forEach(alle::addTeamWennNichtVorhanden);
		}
		Map<Integer, int[]> statsRaw = leseSpielplanStats(alle);
		return sortiereGruppenMitStats(gruppen, statsRaw);
	}

	static List<List<TeamStats>> sortiereGruppenMitStats(List<TeamMeldungen> gruppen, Map<Integer, int[]> statsRaw) {
		List<List<TeamStats>> sortierteGruppen = new ArrayList<>();
		for (TeamMeldungen gruppe : gruppen) {
			sortierteGruppen.add(sortiereMitStats(gruppe, statsRaw));
		}
		return sortierteGruppen;
	}

	static List<TeamStats> sortiereMitStats(TeamMeldungen aktiveMeldungen, Map<Integer, int[]> statsRaw) {
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
		XSpreadsheet spielplanSheet = spielplanSheet();
		if (spielplanSheet == null) {
			return statsMap;
		}

		RangeData teamNrData = leseTeamNummern(spielplanSheet);

		RangeData spielpunkteData = RangeHelper.from(spielplanSheet, xDoc,
				RangePosition.from(JGJSpielPlanSheet.SPIELPNKT_A_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
						JGJSpielPlanSheet.SPIELPNKT_B_SPALTE, letzteSpielplanZeile(spielplanSheet)))
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

	private XSpreadsheet spielplanSheet() {
		return SheetMetadataHelper.findeSheetUndHeile(workingSpreadsheet.getWorkingSpreadsheetDocument(),
				SheetMetadataHelper.SCHLUESSEL_JGJ_SPIELPLAN, JGJSpielPlanSheet.sheetName());
	}

	private RangeData leseTeamNummern(XSpreadsheet spielplanSheet) throws GenerateException {
		return RangeHelper.from(spielplanSheet, workingSpreadsheet.getWorkingSpreadsheetDocument(),
				teamNummernBereich(spielplanSheet)).getDataFromRange();
	}

	private static RangePosition teamNummernBereich(XSpreadsheet spielplanSheet) {
		return RangePosition.from(JGJSpielPlanSheet.TEAM_A_NR_SPALTE, JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE,
				JGJSpielPlanSheet.TEAM_B_NR_SPALTE, letzteSpielplanZeile(spielplanSheet));
	}

	/**
	 * Letzte zu lesende Spielplan-Zeile. Ohne feste Obergrenze: Ein Spielplan mit 34 Teams hat schon 561 Partien
	 * je Runde. Mindestens die erste Datenzeile, damit der Bereich auch bei leerem Spielplan gültig bleibt.
	 */
	private static int letzteSpielplanZeile(XSpreadsheet spielplanSheet) {
		return Math.max(JGJSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE, SheetHelper.letzteBelegteZeile(spielplanSheet));
	}
}
