package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;

public class JederGegenJedenTest {

	@Test
	public void testAnzRunden() throws Exception {

		assertThat(new JederGegenJeden(newMeldungenList(3)).anzRunden()).isEqualTo(3);
		assertThat(new JederGegenJeden(newMeldungenList(4)).anzRunden()).isEqualTo(3);
		assertThat(new JederGegenJeden(newMeldungenList(5)).anzRunden()).isEqualTo(5);
		assertThat(new JederGegenJeden(newMeldungenList(6)).anzRunden()).isEqualTo(5);
		assertThat(new JederGegenJeden(newMeldungenList(7)).anzRunden()).isEqualTo(7);
		assertThat(new JederGegenJeden(newMeldungenList(8)).anzRunden()).isEqualTo(7);
	}

	private TeamMeldungen newMeldungenList(int anz) {
		TeamMeldungen teamMeldungen = new TeamMeldungen();
		for (int i = 0; i < anz; i++) {
			teamMeldungen.addTeamWennNichtVorhanden(Team.from(i + 1));
		}

		return teamMeldungen;
	}

	@Test
	public void testGenerate_6() throws Exception {
		List<List<TeamPaarung>> result = new JederGegenJeden(newMeldungenList(6)).generate();
		assertThat(result).isNotNull().isNotEmpty().hasSize(5);

		TeamPaarung[] expRunde1 = new TeamPaarung[] { newTeamPaarung(1, 6), newTeamPaarung(2, 5), newTeamPaarung(3, 4) };
		assertThat(result.get(0)).containsExactly(expRunde1);
		TeamPaarung[] expRunde2 = new TeamPaarung[] { newTeamPaarung(6, 2), newTeamPaarung(3, 1), newTeamPaarung(4, 5) };
		assertThat(result.get(1)).containsExactly(expRunde2);
		TeamPaarung[] expRunde3 = new TeamPaarung[] { newTeamPaarung(3, 6), newTeamPaarung(4, 2), newTeamPaarung(5, 1) };
		assertThat(result.get(2)).containsExactly(expRunde3);
		TeamPaarung[] expRunde4 = new TeamPaarung[] { newTeamPaarung(6, 4), newTeamPaarung(5, 3), newTeamPaarung(1, 2) };
		assertThat(result.get(3)).containsExactly(expRunde4);
		TeamPaarung[] expRunde5 = new TeamPaarung[] { newTeamPaarung(5, 6), newTeamPaarung(1, 4), newTeamPaarung(2, 3) };
		assertThat(result.get(4)).containsExactly(expRunde5);
	}

	@Test
	public void testGenerate_7() throws Exception {
		List<List<TeamPaarung>> result = new JederGegenJeden(newMeldungenList(7)).generate();
		assertThat(result).isNotNull().isNotEmpty().hasSize(7);

		TeamPaarung[] expRunde1 = new TeamPaarung[] { newTeamPaarung(1, null), newTeamPaarung(2, 7), newTeamPaarung(3, 6), newTeamPaarung(4, 5) };
		assertThat(result.get(0)).containsExactly(expRunde1);
		TeamPaarung[] expRunde2 = new TeamPaarung[] { newTeamPaarung(2, null), newTeamPaarung(3, 1), newTeamPaarung(4, 7), newTeamPaarung(5, 6) };
		assertThat(result.get(1)).containsExactly(expRunde2);
		TeamPaarung[] expRunde3 = new TeamPaarung[] { newTeamPaarung(3, null), newTeamPaarung(4, 2), newTeamPaarung(5, 1), newTeamPaarung(6, 7) };
		assertThat(result.get(2)).containsExactly(expRunde3);
		TeamPaarung[] expRunde4 = new TeamPaarung[] { newTeamPaarung(4, null), newTeamPaarung(5, 3), newTeamPaarung(6, 2), newTeamPaarung(7, 1) };
		assertThat(result.get(3)).containsExactly(expRunde4);
		TeamPaarung[] expRunde5 = new TeamPaarung[] { newTeamPaarung(5, null), newTeamPaarung(6, 4), newTeamPaarung(7, 3), newTeamPaarung(1, 2) };
		assertThat(result.get(4)).containsExactly(expRunde5);
		TeamPaarung[] expRunde6 = new TeamPaarung[] { newTeamPaarung(6, null), newTeamPaarung(7, 5), newTeamPaarung(1, 4), newTeamPaarung(2, 3) };
		assertThat(result.get(5)).containsExactly(expRunde6);
		TeamPaarung[] expRunde7 = new TeamPaarung[] { newTeamPaarung(7, null), newTeamPaarung(1, 6), newTeamPaarung(2, 5), newTeamPaarung(3, 4) };
		assertThat(result.get(6)).containsExactly(expRunde7);
	}

	private TeamPaarung newTeamPaarung(Integer a, Integer b) {
		return new TeamPaarung(Team.from(a), (b == null) ? null : Team.from(b));
	}

}
