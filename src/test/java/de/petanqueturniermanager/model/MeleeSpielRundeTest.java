/*
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.exception.AlgorithmenException;

public class MeleeSpielRundeTest {

	private MeleeSpielRunde spielRunde;

	@BeforeEach
	public void setup() {
		spielRunde = new MeleeSpielRunde(1);
	}

	@Test
	public void testValidateSpielerTeam() throws Exception {

		Team teamA = Team.from(1);
		Team teamB = Team.from(2);

		spielRunde.addTeamWennNichtVorhanden(teamA);
		spielRunde.addTeamWennNichtVorhanden(teamB);

		teamA.addSpielerWennNichtVorhanden(Spieler.from(1));
		teamB.addSpielerWennNichtVorhanden(Spieler.from(1));

		try {
			spielRunde.validateSpielerTeam(null);
			fail("Erwarte AlgorithmenException Exception");
		} catch (AlgorithmenException exp) {

		}
	}

	@Test
	public void testValidateSpielerTeam_neuesTeamMitDuplikat_wirftException() throws Exception {
		Team teamA = Team.from(1);
		teamA.addSpielerWennNichtVorhanden(Spieler.from(1));
		spielRunde.addTeamWennNichtVorhanden(teamA);

		Team neuesTeam = Team.from(2);
		neuesTeam.addSpielerWennNichtVorhanden(Spieler.from(1));

		assertThatThrownBy(() -> spielRunde.validateSpielerTeam(neuesTeam))
				.isInstanceOf(AlgorithmenException.class);
	}

	@Test
	public void testAddTeamsWennNichtVorhanden() throws Exception {
		Team teamA = Team.from(1);
		Team teamB = Team.from(2);

		spielRunde.addTeamsWennNichtVorhanden(List.of(teamA, teamB));

		assertThat(spielRunde.teams()).hasSize(2).containsExactly(teamA, teamB);

		// erneutes hinzufuegen desselben Teams darf keine Duplikate erzeugen
		spielRunde.addTeamsWennNichtVorhanden(List.of(teamA));
		assertThat(spielRunde.teams()).hasSize(2);
	}

	@Test
	public void testDeleteAllTeams() throws Exception {
		Team teamA = Team.from(1);
		teamA.addSpielerWennNichtVorhanden(Spieler.from(1));
		spielRunde.addTeamWennNichtVorhanden(teamA);

		spielRunde.deleteAllTeams();

		assertThat(spielRunde.teams()).isEmpty();
		assertThat(teamA.spieler()).isEmpty();
	}

	@Test
	public void testToString() throws Exception {
		Team teamA = Team.from(1);
		Team teamB = Team.from(2);
		spielRunde.addTeamWennNichtVorhanden(teamA);
		spielRunde.addTeamWennNichtVorhanden(teamB);

		assertThat(spielRunde.toString()).contains("Nr=1").contains("Teams=[");
	}

}
