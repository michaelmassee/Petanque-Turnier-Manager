package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.google.common.collect.ImmutableList;

import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.model.TeamRangliste;

public class KoRundeTeamPaarungenTest {

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorEmptyGeneratSpielRunde() throws Exception {
		new KoRundeTeamPaarungen(new TeamRangliste());
	}

	@Test(expected = NullPointerException.class)
	public void testConstructorNull() throws Exception {
		new KoRundeTeamPaarungen(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConstructorUngeradeAnzahlGeneratSpielRunde() throws Exception {
		TeamRangliste teamRangliste = new TeamRangliste();
		teamRangliste.add(Team.from(1));
		teamRangliste.add(Team.from(2));
		teamRangliste.add(Team.from(3));
		new KoRundeTeamPaarungen(teamRangliste);
	}

	@Test
	public void testGeneratSpielRundeOhneGegner() throws Exception {
		TeamRangliste teamRangliste = new TeamRangliste();
		Team teamA = teamRangliste.add(Team.from(1));
		Team teamB = teamRangliste.add(Team.from(2));
		Team teamC = teamRangliste.add(Team.from(3));
		Team teamD = teamRangliste.add(Team.from(4));
		KoRundeTeamPaarungen paarungen = new KoRundeTeamPaarungen(teamRangliste);
		assertThat(paarungen.getTeamRangListe().size()).isEqualTo(4);

		FormeSpielrunde generatSpielRunde = paarungen.generatSpielRunde();
		assertThat(generatSpielRunde.size()).isEqualTo(2);

		ImmutableList<TeamPaarung> teamPaarungen = generatSpielRunde.getTeamPaarungen();

		assertThat(teamPaarungen.get(0).getA()).isEqualTo(teamA);
		assertThat(teamPaarungen.get(0).getB()).isEqualTo(teamD);

		assertThat(teamPaarungen.get(1).getA()).isEqualTo(teamB);
		assertThat(teamPaarungen.get(1).getB()).isEqualTo(teamC);
	}

	// test hat bereits gegeneinander gespielt
	@Test
	public void testGeneratSpielRundeMitGegner() throws Exception {
		TeamRangliste teamRangliste = new TeamRangliste();
		Team teamA = teamRangliste.add(Team.from(1));
		Team teamB = teamRangliste.add(Team.from(2));
		Team teamC = teamRangliste.add(Team.from(3));
		Team teamD = teamRangliste.add(Team.from(4));

		teamA.addGegner(teamD);
		KoRundeTeamPaarungen paarungen = new KoRundeTeamPaarungen(teamRangliste);
		FormeSpielrunde generatSpielRunde = paarungen.generatSpielRunde();
		ImmutableList<TeamPaarung> teamPaarungen = generatSpielRunde.getTeamPaarungen();

		assertThat(teamPaarungen.get(0).getA()).isEqualTo(teamA);
		assertThat(teamPaarungen.get(0).getB()).isEqualTo(teamC);

		assertThat(teamPaarungen.get(1).getA()).isEqualTo(teamB);
		assertThat(teamPaarungen.get(1).getB()).isEqualTo(teamD);
	}

	@Test
	public void testGeneratSpielRundeMitGegnerMussTauschen() throws Exception {
		TeamRangliste teamRangliste = new TeamRangliste();
		Team teamA = teamRangliste.add(Team.from(1));
		Team teamB = teamRangliste.add(Team.from(2));
		Team teamC = teamRangliste.add(Team.from(3));
		Team teamD = teamRangliste.add(Team.from(4));

		teamB.addGegner(teamC); // = 2 Paarungen was nicht geht

		KoRundeTeamPaarungen paarungen = new KoRundeTeamPaarungen(teamRangliste);
		FormeSpielrunde generatSpielRunde = paarungen.generatSpielRunde();
		ImmutableList<TeamPaarung> teamPaarungen = generatSpielRunde.getTeamPaarungen();

		assertThat(teamPaarungen.get(0).getA()).isEqualTo(teamA);
		assertThat(teamPaarungen.get(0).getB()).isEqualTo(teamC);

		assertThat(teamPaarungen.get(1).getA()).isEqualTo(teamB);
		assertThat(teamPaarungen.get(1).getB()).isEqualTo(teamD);
	}

	@Test
	public void testGeneratSpielRundeMitDoppelte() throws Exception {
		TeamRangliste teamRangliste = new TeamRangliste();
		Team teamA = teamRangliste.add(Team.from(1));
		Team teamB = teamRangliste.add(Team.from(2));
		Team teamC = teamRangliste.add(Team.from(3));
		Team teamD = teamRangliste.add(Team.from(4));

		teamA.addGegner(teamC);
		teamB.addGegner(teamC);

		KoRundeTeamPaarungen paarungen = new KoRundeTeamPaarungen(teamRangliste);
		FormeSpielrunde generatSpielRunde = paarungen.generatSpielRunde();
		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isTrue();

		ImmutableList<TeamPaarung> teamPaarungen = generatSpielRunde.getTeamPaarungen();

		assertThat(teamPaarungen.get(0).getA()).isEqualTo(teamA);
		assertThat(teamPaarungen.get(0).getB()).isEqualTo(teamD);

		assertThat(teamPaarungen.get(1).getA()).isEqualTo(teamB);
		assertThat(teamPaarungen.get(1).getB()).isEqualTo(teamC);
	}

}
