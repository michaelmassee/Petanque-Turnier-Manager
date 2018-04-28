/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.SpielRunde;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;

public class SpielRundeTest {

	private SpielRunde spielRunde;

	@Before
	public void setup() {
		this.spielRunde = new SpielRunde(1);
	}

	@Test
	public void testValidateSpielerTeam() throws Exception {

		Team teamA = new Team(1);
		Team teamB = new Team(2);

		this.spielRunde.addTeamWennNichtVorhanden(teamA);
		this.spielRunde.addTeamWennNichtVorhanden(teamB);

		teamA.addSpielerWennNichtVorhanden(new Spieler(1));
		teamB.addSpielerWennNichtVorhanden(new Spieler(1));

		try {
			this.spielRunde.validateSpielerTeam(null);
			fail("Erwarte AlgorithmenException Exception");
		} catch (AlgorithmenException exp) {

		}
	}

}
