/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.fail;

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

}
