package de.petanqueturniermanager.jedergegenjeden;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

public class JGJGruppenAufteilerTest {

	@Test
	public void ohneSetzPos_blockweiseInReihenfolge() {
		TeamMeldungen meldungen = meldungen(team(1, 0), team(2, 0), team(3, 0), team(4, 0), team(5, 0), team(6, 0));

		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(meldungen, 3);

		assertThat(gruppen).hasSize(2);
		assertThat(teamNummern(gruppen.get(0))).containsExactly(1, 2, 3);
		assertThat(teamNummern(gruppen.get(1))).containsExactly(4, 5, 6);
	}

	@Test
	public void mitSetzPos_gleicheSetzPosLandetInVerschiedenenGruppen() {
		// 6 Teams, Gruppengröße 3, Setzpos 1 zweimal, Setzpos 2 zweimal, Rest = 0
		TeamMeldungen meldungen = meldungen(
				team(1, 1), team(2, 1), team(3, 2), team(4, 2), team(5, 0), team(6, 0));

		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(meldungen, 3);

		assertThat(gruppen).hasSize(2);
		assertNichtZusammen(gruppen, 1, 2);
		assertNichtZusammen(gruppen, 3, 4);
	}

	@Test
	public void mitSetzPos_dreiGruppen_alleSetzPos1VerteiltAufAlleGruppen() {
		// 9 Teams, Gruppengröße 3, alle drei Setzpos=1 müssen in unterschiedliche Gruppen
		TeamMeldungen meldungen = meldungen(
				team(1, 1), team(2, 1), team(3, 1),
				team(4, 0), team(5, 0), team(6, 0),
				team(7, 0), team(8, 0), team(9, 0));

		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(meldungen, 3);

		assertThat(gruppen).hasSize(3);
		assertNichtZusammen(gruppen, 1, 2);
		assertNichtZusammen(gruppen, 1, 3);
		assertNichtZusammen(gruppen, 2, 3);
		// Gruppen sollten gleich groß sein (3 Teams pro Gruppe)
		gruppen.forEach(g -> assertThat(g.size()).isEqualTo(3));
	}

	@Test
	public void leereMeldungen_liefertEinzigeLeereGruppe() {
		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(new TeamMeldungen(), 4);

		assertThat(gruppen).hasSize(1);
		assertThat(gruppen.get(0).size()).isEqualTo(0);
	}

	@Test
	public void mitSetzPos_zweiGruppen_kollisionUnvermeidbar_endetInGleicherGruppe() {
		// 4 Teams mit Setzpos=1, nur 2 Gruppen: zwei Teams müssen kollidieren – best effort
		TeamMeldungen meldungen = meldungen(team(1, 1), team(2, 1), team(3, 1), team(4, 1));

		List<TeamMeldungen> gruppen = JGJGruppenAufteiler.teileInGruppen(meldungen, 2);

		assertThat(gruppen).hasSize(2);
		// Insgesamt 4 Teams in 2 Gruppen – Algorithmus darf trotz unmöglicher Constraint nicht abstürzen.
		assertThat(gruppen.get(0).size() + gruppen.get(1).size()).isEqualTo(4);
	}

	// ─── Helpers ─────────────────────────────────────────────────────────

	private static Team team(int nr, int setzPos) {
		return Team.from(nr).setSetzPos(setzPos);
	}

	private static TeamMeldungen meldungen(Team... teams) {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (Team team : teams) {
			meldungen.addTeamWennNichtVorhanden(team);
		}
		return meldungen;
	}

	private static List<Integer> teamNummern(TeamMeldungen meldungen) {
		return meldungen.teams().stream().map(Team::getNr).toList();
	}

	private static void assertNichtZusammen(List<TeamMeldungen> gruppen, int teamA, int teamB) {
		for (TeamMeldungen gruppe : gruppen) {
			Set<Integer> nrs = new HashSet<>(teamNummern(gruppe));
			assertThat(nrs.contains(teamA) && nrs.contains(teamB))
					.as("Teams %d und %d dürfen nicht in derselben Gruppe sein", teamA, teamB)
					.isFalse();
		}
	}
}
