package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteRechner.TeamStats;

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
	void teamStatsBerechnetDifferenzen() {
		TeamStats stats = stats(1, 4, 1, 65, 40);

		assertThat(stats.spielDiff()).isEqualTo(3);
		assertThat(stats.spielPunkteDiff()).isEqualTo(25);
	}
}
