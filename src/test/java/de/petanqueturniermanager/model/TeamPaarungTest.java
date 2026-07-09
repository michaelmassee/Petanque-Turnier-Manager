package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Optional;

import org.junit.jupiter.api.Test;

public class TeamPaarungTest {

	@Test
	public void testTeamPaarungmit2Teams() throws Exception {
		Team teamA = Team.from(1);
		Team teamB = Team.from(2);

		TeamPaarung paarung = new TeamPaarung(teamA, teamB);
		assertThat(paarung.getA()).isEqualTo(teamA);
		assertThat(paarung.getB()).isEqualTo(teamB);
		assertThat(paarung.getB()).isNotEqualTo(teamA);
	}

	@Test
	public void testTeamPaarungmitNull() throws Exception {
		Team teamA = Team.from(1);
		TeamPaarung paarung = new TeamPaarung(teamA, Optional.empty());
		assertThat(paarung.getA()).isEqualTo(teamA);
		assertThat(paarung.getB()).isNull();
		assertThat(paarung.isFreilos()).isTrue();
		assertThat(paarung.hasB()).isFalse();
	}

	@Test
	public void testTeamPaarungmitEqual() throws Exception {
		Team teamA = Team.from(1);
		assertThrows(IllegalArgumentException.class, () -> new TeamPaarung(teamA, teamA));
	}

	@Test
	public void testIsInPaarung() throws Exception {
		Team teamA = Team.from(1);
		Team teamB = Team.from(8);
		TeamPaarung paarung = new TeamPaarung(teamA);
		assertThat(paarung.isInPaarung(teamA)).isTrue();
		assertThat(paarung.isInPaarung(Team.from(1))).isTrue();
		assertThat(paarung.isInPaarung(Team.from(2))).isFalse();
		assertThat(paarung.isInPaarung(null)).isFalse();
		paarung = new TeamPaarung(teamA, teamB);
		assertThat(paarung.isInPaarung(Team.from(1))).isTrue();
		assertThat(paarung.isInPaarung(teamB)).isTrue();
		assertThat(paarung.isInPaarung(Team.from(2))).isFalse();
	}

	@Test
	public void testEqualsNull() throws Exception {
		TeamPaarung paarung = new TeamPaarung(Team.from(1), Team.from(2));
		assertThat(paarung.equals(null)).isFalse();
	}

	@Test
	public void testEqualsAndererTyp() throws Exception {
		TeamPaarung paarung = new TeamPaarung(Team.from(1), Team.from(2));
		assertThat(paarung.equals("kein TeamPaarung")).isFalse();
	}

	@Test
	public void testToString() throws Exception {
		TeamPaarung paarung = new TeamPaarung(Team.from(1), Team.from(2));
		assertThat(paarung.toString()).contains("Teams=[").contains("nr=1").contains("nr=2");
	}

	@Test
	public void testClone() throws Exception {
		TeamPaarung paarung = new TeamPaarung(Team.from(1), Team.from(2));
		TeamPaarung kopie = paarung.clone();

		assertThat(kopie).isEqualTo(paarung).isNotSameAs(paarung);
		assertThat(kopie.getA()).isNotSameAs(paarung.getA());
		assertThat(kopie.getB()).isNotSameAs(paarung.getB());
	}

	@Test
	public void testRemoveHatGegner() throws Exception {
		Team teamA = Team.from(1);
		Team teamB = Team.from(2);
		TeamPaarung paarung = new TeamPaarung(teamA, teamB).setHatGegner();

		assertThat(teamA.isHatGegner()).isTrue();
		assertThat(teamB.isHatGegner()).isTrue();

		paarung.removeHatGegner();

		assertThat(teamA.isHatGegner()).isFalse();
		assertThat(teamB.isHatGegner()).isFalse();
	}

}
