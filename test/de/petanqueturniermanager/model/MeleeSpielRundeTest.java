/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.exception.AlgorithmenException;

public class MeleeSpielRundeTest {

	private MeleeSpielRunde spielRunde;

	@Before
	public void setup() {
		spielRunde = new MeleeSpielRunde(1);
	}

	@Test
	public void testValidateSpielerTeam() throws Exception {

		Team teamA = new Team(1);
		Team teamB = new Team(2);

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
