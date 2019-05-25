package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.Test;

public class TeamPaarungTest {

	@Test
	public void testTeamPaarungmit2Teams() throws Exception {
		Team teamA = new Team(1);
		Team teamB = new Team(2);

		TeamPaarung paarung = new TeamPaarung(teamA, teamB);
		assertThat(paarung.getA()).isEqualTo(teamA);
		assertThat(paarung.getB()).isEqualTo(teamB);
		assertThat(paarung.getB()).isNotEqualTo(teamA);
	}

	@Test
	public void testTeamPaarungmitNull() throws Exception {
		Team teamA = new Team(1);
		TeamPaarung paarung = new TeamPaarung(teamA, Optional.empty());
		assertThat(paarung.getA()).isEqualTo(teamA);
		assertThat(paarung.getB()).isNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testTeamPaarungmitEqual() throws Exception {
		Team teamA = new Team(1);
		new TeamPaarung(teamA, teamA);
	}

}
