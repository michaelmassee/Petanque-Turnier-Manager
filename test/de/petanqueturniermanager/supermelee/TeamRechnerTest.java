package de.petanqueturniermanager.supermelee;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import de.petanqueturniermanager.supermelee.TeamRechner;

/*
* TeamRechnerTest.java
*
* Erstellung     : 11.09.2017 / Michael Massee
*
*/

public class TeamRechnerTest {

	@Test
	public void testTeamRechner() throws Exception {

		HashSet<Integer> nurDoublette = new HashSet<>();
		for (int doublCntr = 4; doublCntr < 44; doublCntr += 4) {
			nurDoublette.add(doublCntr);
		}

		TeamRechner teamRechner;

		for (int anzahlMeldungen = 10; anzahlMeldungen < 60; anzahlMeldungen++) {
			teamRechner = new TeamRechner(anzahlMeldungen);
			String fehlrmldg = "Fehler beim AnzahlSpieler " + anzahlMeldungen + " ";
			assertEquals(fehlrmldg, anzahlMeldungen,
					teamRechner.getAnzTriplette() * 3 + teamRechner.getAnzDoublette() * 2);
			if (nurDoublette.contains(anzahlMeldungen)) {
				assertEquals(fehlrmldg, true, teamRechner.isNurDoubletteMoeglich());
			}
		}
	}

	@Test
	public void testTeamRechnerStichProbe() throws Exception {
		TeamRechner teamRechner;

		teamRechner = new TeamRechner(5);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(6);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(7); // INVALID !!
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(-1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(8);
		assertEquals(4, teamRechner.getAnzDoublette());
		assertEquals(0, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(9);
		assertEquals(3, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(10);
		assertEquals(2, teamRechner.getAnzDoublette());
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(11);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(3, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(12);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(4, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(13);
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
	}

	@Test
	public void testTeamRechnerStichProbeWeiterOben() throws Exception {
		TeamRechner teamRechner;
		teamRechner = new TeamRechner(36);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(12, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(37);
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(9, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(38);
		assertEquals(4, teamRechner.getAnzDoublette());
		assertEquals(10, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(39);
		assertEquals(3, teamRechner.getAnzDoublette());
		assertEquals(11, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(40);
		assertEquals(2, teamRechner.getAnzDoublette());
		assertEquals(12, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(41);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(13, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new TeamRechner(42);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(14, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
	}

}
