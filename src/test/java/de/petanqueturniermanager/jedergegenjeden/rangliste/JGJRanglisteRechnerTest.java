package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteRechner.TeamStats;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

/**
 * Unit-Tests der reinen (LibreOffice-freien) Kombinations- und Sortierlogik des
 * {@link JGJRanglisteRechner}.
 */
class JGJRanglisteRechnerTest {

	private static TeamStats stats(int teamNr, int siege, int niederlagen, int punktePlus, int punkteMinus) {
		return new TeamStats(teamNr, siege, niederlagen, punktePlus, punkteMinus);
	}

	@Test
	void vergleicherSortiertNachSiegenDannDiffDannPunktePlus() {
		TeamStats wenigerSiege = stats(1, 2, 3, 100, 10);
		TeamStats mehrSiege = stats(2, 4, 1, 40, 30);
		TeamStats gleichSiegeBessereDiff = stats(3, 4, 1, 60, 20); // Diff 40
		TeamStats gleichSiegeGleicheDiffMehrPlus = stats(4, 4, 1, 70, 30); // Diff 40, mehr +

		List<TeamStats> liste = new ArrayList<>(
				List.of(wenigerSiege, mehrSiege, gleichSiegeBessereDiff, gleichSiegeGleicheDiffMehrPlus));
		liste.sort(JGJRanglisteRechner.vergleicher());

		assertThat(liste).containsExactly(
				gleichSiegeGleicheDiffMehrPlus, // 4 Siege, Diff 40, + 70
				gleichSiegeBessereDiff,         // 4 Siege, Diff 40, + 60
				mehrSiege,                      // 4 Siege, Diff 10
				wenigerSiege);                  // 2 Siege
	}

	@Test
	void snakeKombinationVerzahntGruppenNachRang() {
		List<TeamStats> gruppeA = List.of(stats(11, 5, 0, 0, 0), stats(12, 3, 2, 0, 0), stats(13, 1, 4, 0, 0));
		List<TeamStats> gruppeB = List.of(stats(21, 5, 0, 0, 0), stats(22, 3, 2, 0, 0), stats(23, 1, 4, 0, 0));

		List<TeamStats> kombiniert = JGJRanglisteRechner.snakeKombination(List.of(gruppeA, gruppeB));

		assertThat(kombiniert).extracting(TeamStats::teamNr)
				.containsExactly(11, 21, 12, 22, 13, 23);
	}

	@Test
	void snakeKombinationUeberspringtKuerzereGruppenAmEnde() {
		List<TeamStats> gruppeA = List.of(stats(11, 5, 0, 0, 0), stats(12, 3, 2, 0, 0));
		List<TeamStats> gruppeB = List.of(stats(21, 5, 0, 0, 0));

		List<TeamStats> kombiniert = JGJRanglisteRechner.snakeKombination(List.of(gruppeA, gruppeB));

		assertThat(kombiniert).extracting(TeamStats::teamNr)
				.containsExactly(11, 21, 12);
	}

	@Test
	void snakeKombinationOhneGruppenLiefertLeereListe() {
		assertThat(JGJRanglisteRechner.snakeKombination(List.of())).isEmpty();
	}

	@Test
	void sortiereGruppenMitStatsSortiertJedeGruppeUndNutztNullwerteFuerFehlendeTeams() {
		TeamMeldungen gruppeA = new TeamMeldungen()
				.addTeamWennNichtVorhanden(Team.from(1))
				.addTeamWennNichtVorhanden(Team.from(2));
		TeamMeldungen gruppeB = new TeamMeldungen()
				.addTeamWennNichtVorhanden(Team.from(3));
		Map<Integer, int[]> statsRaw = Map.of(
				1, new int[] { 1, 2, 20, 30 },
				2, new int[] { 3, 0, 30, 10 });

		List<List<TeamStats>> sortierteGruppen = JGJRanglisteRechner.sortiereGruppenMitStats(
				List.of(gruppeA, gruppeB), statsRaw);

		assertThat(sortierteGruppen).hasSize(2);
		assertThat(sortierteGruppen.get(0)).extracting(TeamStats::teamNr)
				.containsExactly(2, 1);
		assertThat(sortierteGruppen.get(1)).containsExactly(stats(3, 0, 0, 0, 0));
	}

	@Test
	void teamStatsBerechnetDifferenzen() {
		TeamStats stats = stats(1, 4, 1, 65, 40);

		assertThat(stats.spielDiff()).isEqualTo(3);
		assertThat(stats.spielPunkteDiff()).isEqualTo(25);
	}

	private static final String PARTIE = "=PTM.UUID.TEAMNR(...)";
	private static final String KOPF = "0";

	/** Spielplan mit zwei Gruppen: A = 1–4, B = 5–8 (Kopfzeilen mit festem Wert 0). */
	private static RangeData zweiGruppenSpielplan() {
		RangeData data = new RangeData();
		data.add(new RowData(0, 0));
		data.add(new RowData(1, 2));
		data.add(new RowData(3, 4));
		data.add(new RowData(1, 3));
		data.add(new RowData(0, 0));
		data.add(new RowData(5, 6));
		data.add(new RowData(7, 8));
		return data;
	}

	private static String[][] formeln(String... teamAFormeln) {
		return Arrays.stream(teamAFormeln).map(formel -> new String[] { formel, PARTIE })
				.toArray(String[][]::new);
	}

	private static TeamMeldungen meldungen(int... teamNrn) {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int nr : teamNrn) {
			meldungen.addTeamWennNichtVorhanden(Team.from(nr));
		}
		return meldungen;
	}

	private static List<List<Integer>> nummern(List<TeamMeldungen> gruppen) {
		return gruppen.stream().map(gruppe -> gruppe.teams().stream().map(Team::getNr).toList()).toList();
	}

	@Test
	void spielplanGruppenLiestGruppenAusKopfzeilen() {
		List<List<Integer>> gruppen = JGJRanglisteRechner.spielplanGruppen(zweiGruppenSpielplan(),
				formeln(KOPF, PARTIE, PARTIE, PARTIE, KOPF, PARTIE, PARTIE));
		assertThat(gruppen).containsExactly(List.of(1, 2, 3, 4), List.of(5, 6, 7, 8));
	}

	@Test
	void geloeschtesTeamMitFormelTrenntKeineGruppe() {
		RangeData data = zweiGruppenSpielplan();
		data.set(2, new RowData(0, 4)); // Team 3 gelöscht: Formel ergibt 0
		List<List<Integer>> gruppen = JGJRanglisteRechner.spielplanGruppen(data,
				formeln(KOPF, PARTIE, PARTIE, PARTIE, KOPF, PARTIE, PARTIE));
		assertThat(gruppen).containsExactly(List.of(1, 2, 4, 3), List.of(5, 6, 7, 8));
	}

	@Test
	void spielplanOhneGruppenErgibtEineGruppe() {
		RangeData data = new RangeData();
		data.add(new RowData(1, 2));
		data.add(new RowData(3, 0)); // Freilos
		assertThat(JGJRanglisteRechner.spielplanGruppen(data, formeln(PARTIE, PARTIE)))
				.containsExactly(List.of(1, 2, 3));
	}

	@Test
	void ausgestiegenesTeamVerschiebtKeineGruppe() {
		List<TeamMeldungen> gruppen = JGJRanglisteRechner.gruppenAusSpielplan(
				List.of(List.of(1, 2, 3, 4), List.of(5, 6, 7, 8)), meldungen(1, 3, 4, 5, 6, 7, 8));
		assertThat(nummern(gruppen)).containsExactly(List.of(1, 3, 4), List.of(5, 6, 7, 8));
	}

	@Test
	void eineSpielplanGruppeBehaeltAlleAktivenTeams() {
		List<TeamMeldungen> gruppen = JGJRanglisteRechner.gruppenAusSpielplan(List.of(List.of(1, 2, 3)),
				meldungen(1, 2, 3, 9));
		assertThat(nummern(gruppen)).containsExactly(List.of(1, 2, 3, 9));
	}
}
