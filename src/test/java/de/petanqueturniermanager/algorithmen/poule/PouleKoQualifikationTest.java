/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.poule;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.poule.PouleKoQualifikation.Turnier;

class PouleKoQualifikationTest {

	/** Gruppe 1: 1,2,3,4 – Gruppe 2: 5,6,7,8 (jeweils schon nach Platz sortiert). */
	private static final List<List<PouleTeamErgebnis>> ZWEI_VIERER_GRUPPEN = List.of(gruppe(1, 2, 3, 4),
			gruppe(5, 6, 7, 8));

	@Test
	void ohneAusstieg_crossSeedingWieBisher() {
		var qualifikation = PouleKoQualifikation.von(ZWEI_VIERER_GRUPPEN, alle(8));

		assertThat(qualifikation.aTeams()).containsExactly(1, 5, 6, 2);
		assertThat(qualifikation.bTeams()).containsExactly(3, 7, 8, 4);
		assertThat(qualifikation.turnier(2)).isEqualTo(Turnier.A);
		assertThat(qualifikation.turnier(3)).isEqualTo(Turnier.B);
	}

	@Test
	void ausgestiegenerGruppensieger_gruppendritterRuecktInsATurnierNach() {
		var qualifikation = PouleKoQualifikation.von(ZWEI_VIERER_GRUPPEN, ohne(alle(8), 1));

		assertThat(qualifikation.aTeams()).as("2 wird Gruppenerster, 3 Gruppenzweiter").containsExactly(2, 5, 6, 3);
		assertThat(qualifikation.bTeams()).as("4 rückt auf Platz 3, Gruppe 1 hat keinen Vierten mehr")
				.containsExactly(4, 7, 8);
		assertThat(qualifikation.turnier(1)).isEqualTo(Turnier.KEINES);
		assertThat(qualifikation.turnier(3)).isEqualTo(Turnier.A);
		assertThat(qualifikation.turnier(4)).isEqualTo(Turnier.B);
	}

	@Test
	void ausgestiegenerGruppendritter_nurBTurnierRuecktNach() {
		var qualifikation = PouleKoQualifikation.von(ZWEI_VIERER_GRUPPEN, ohne(alle(8), 7));

		assertThat(qualifikation.aTeams()).containsExactly(1, 5, 6, 2);
		assertThat(qualifikation.bTeams()).containsExactly(3, 8, 4);
	}

	@Test
	void fuenferGruppe_fuenfterRuecktNachAusstiegInsBTurnier() {
		var qualifikation = PouleKoQualifikation.von(List.of(gruppe(1, 2, 3, 4, 5)), ohne(alle(5), 2));

		assertThat(qualifikation.turnier(5)).as("vorher Platz 5, jetzt Platz 4").isEqualTo(Turnier.B);
		assertThat(qualifikation.aTeams()).containsExactly(1, 3);
		assertThat(qualifikation.bTeams()).containsExactly(4, 5);
	}

	@Test
	void platzFuenfOhneAusstieg_spieltKeinTurnier() {
		var qualifikation = PouleKoQualifikation.von(List.of(gruppe(1, 2, 3, 4, 5)), alle(5));

		assertThat(qualifikation.turnier(5)).isEqualTo(Turnier.KEINES);
	}

	private static List<PouleTeamErgebnis> gruppe(int... teamNrnNachPlatz) {
		return Arrays.stream(teamNrnNachPlatz).mapToObj(nr -> new PouleTeamErgebnis(nr, 0, 0, 0, 0, List.of()))
				.toList();
	}

	private static Set<Integer> alle(int anzTeams) {
		return IntStream.rangeClosed(1, anzTeams).boxed().collect(Collectors.toSet());
	}

	private static Set<Integer> ohne(Set<Integer> teams, int teamNr) {
		return teams.stream().filter(nr -> nr != teamNr).collect(Collectors.toSet());
	}
}
