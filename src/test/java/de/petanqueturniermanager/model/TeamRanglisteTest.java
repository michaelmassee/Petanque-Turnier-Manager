package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class TeamRanglisteTest {

	@Test
	public void testAddTeams() throws Exception {

		TeamRangliste liste = new TeamRangliste();

		liste.add(Team.from(1));
		assertThat(liste.getTeamListe()).hasSize(1);
		liste.add(Team.from(2));
		assertThat(liste.getTeamListe()).hasSize(2);
	}

	@Test
	public void testAddDoubleTeams() throws Exception {
		assertThrows(IllegalArgumentException.class, () -> {
			TeamRangliste liste = new TeamRangliste();
			liste.add(Team.from(1));
			liste.add(Team.from(1));
		});
	}

}
