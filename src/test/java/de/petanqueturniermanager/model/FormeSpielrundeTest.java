package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class FormeSpielrundeTest {

	@Test
	public void testAddPaarungWennNichtVorhanden() throws Exception {

		FormeSpielrunde spielRunde = new FormeSpielrunde(1);
		TeamPaarung teamPaarung = new TeamPaarung(Team.from(1), Team.from(2));
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		assertThat(spielRunde.getTeamPaarungen()).isNotEmpty().hasSize(1);
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		assertThat(spielRunde.getTeamPaarungen()).isNotEmpty().hasSize(1);

		teamPaarung = new TeamPaarung(Team.from(3), Team.from(4));
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		assertThat(spielRunde.getTeamPaarungen()).isNotEmpty().hasSize(2);
	}

	@Test
	public void testGetAundBTeams() throws Exception {
		FormeSpielrunde spielRunde = new FormeSpielrunde(1);
		Team team1 = Team.from(1);
		Team team2 = Team.from(2);
		Team team3 = Team.from(3);
		Team team4 = Team.from(4);
		TeamPaarung teamPaarung = new TeamPaarung(team1, team2);
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		teamPaarung = new TeamPaarung(team3, team4);
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);

		assertThat(spielRunde.getATeams()).hasSize(2).containsExactly(team1, team3);
		assertThat(spielRunde.getBTeams()).hasSize(2).containsExactly(team2, team4);
		assertThat(spielRunde.getAundBTeams()).hasSize(4).containsExactly(team1, team3, team2, team4);
	}

	@Test
	public void testFindTeamPaarung() throws Exception {
		FormeSpielrunde spielRunde = new FormeSpielrunde(1);
		Team team1 = Team.from(1);
		Team team2 = Team.from(2);
		Team team3 = Team.from(3);
		Team team4 = Team.from(4);
		TeamPaarung teamPaarung = new TeamPaarung(team1, team2);
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		teamPaarung = new TeamPaarung(team3, team4);
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);

		assertThat(spielRunde.findTeamPaarung(team2)).isNotNull();
		assertThat(spielRunde.findTeamPaarung(team2).getA()).isNotNull().isEqualTo(team1);
		assertThat(spielRunde.findTeamPaarung(team2).getB()).isNotNull().isEqualTo(team2);
	}
}
