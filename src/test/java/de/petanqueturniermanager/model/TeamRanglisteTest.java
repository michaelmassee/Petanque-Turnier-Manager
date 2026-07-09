package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

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

	@Test
	public void testAddAll() throws Exception {
		TeamRangliste liste = new TeamRangliste();
		liste.addAll(List.of(Team.from(1), Team.from(2), Team.from(3)));
		assertThat(liste.size()).isEqualTo(3);
	}

	@Test
	public void testRanglisteVonOben() throws Exception {
		TeamRangliste liste = new TeamRangliste();
		liste.addAll(List.of(Team.from(1), Team.from(2), Team.from(3), Team.from(4)));

		TeamRangliste oben = liste.ranglisteVonOben(2);

		assertThat(oben.size()).isEqualTo(2);
		assertThat(oben.get(0)).isEqualTo(Team.from(1));
		assertThat(oben.get(1)).isEqualTo(Team.from(2));
	}

	@Test
	public void testRanglisteVonLetzte() throws Exception {
		TeamRangliste liste = new TeamRangliste();
		liste.addAll(List.of(Team.from(1), Team.from(2), Team.from(3), Team.from(4)));

		TeamRangliste letzte = liste.ranglisteVonLetzte(2);

		assertThat(letzte.size()).isEqualTo(2);
		assertThat(letzte.get(0)).isEqualTo(Team.from(3));
		assertThat(letzte.get(1)).isEqualTo(Team.from(4));
	}

}
