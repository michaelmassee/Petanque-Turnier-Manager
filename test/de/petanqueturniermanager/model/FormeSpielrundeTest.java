package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class FormeSpielrundeTest {

	@Test
	public void testAddPaarungWennNichtVorhanden() throws Exception {

		FormeSpielrunde spielRunde = new FormeSpielrunde(1);
		TeamPaarung teamPaarung = new TeamPaarung(new Team(1), new Team(2));
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		assertThat(spielRunde.getTeamPaarungen()).isNotEmpty().hasSize(1);
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		assertThat(spielRunde.getTeamPaarungen()).isNotEmpty().hasSize(1);

		teamPaarung = new TeamPaarung(new Team(3), new Team(4));
		spielRunde.addPaarungWennNichtVorhanden(teamPaarung);
		assertThat(spielRunde.getTeamPaarungen()).isNotEmpty().hasSize(2);
	}
}
