package de.petanqueturniermanager.supermelee;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;

import org.junit.Test;

import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;

/*
* TeamRechnerTest.java
*
* Erstellung     : 11.09.2017 / Michael Massee
*
*/

public class SuperMeleeTeamRechnerTest {

	@Test
	public void testTeamRechner() throws Exception {

		HashSet<Integer> nurDoublette = new HashSet<>();
		for (int doublCntr = 4; doublCntr < 44; doublCntr += 4) {
			nurDoublette.add(doublCntr);
		}

		SuperMeleeTeamRechner teamRechner;

		for (int anzahlMeldungen = 10; anzahlMeldungen < 60; anzahlMeldungen++) {
			teamRechner = new SuperMeleeTeamRechner(anzahlMeldungen);
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
		SuperMeleeTeamRechner teamRechner;

		teamRechner = new SuperMeleeTeamRechner(5);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(6);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(7); // INVALID !!
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(-1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(8);
		assertEquals(4, teamRechner.getAnzDoublette());
		assertEquals(0, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(9);
		assertEquals(3, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(10);
		assertEquals(2, teamRechner.getAnzDoublette());
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(11);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(3, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(12);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(4, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(13);
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
	}

	@Test
	public void testTeamRechnerStichProbeWeiterOben() throws Exception {
		SuperMeleeTeamRechner teamRechner;
		teamRechner = new SuperMeleeTeamRechner(36);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(12, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(37);
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(9, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(38);
		assertEquals(4, teamRechner.getAnzDoublette());
		assertEquals(10, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(39);
		assertEquals(3, teamRechner.getAnzDoublette());
		assertEquals(11, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(40);
		assertEquals(2, teamRechner.getAnzDoublette());
		assertEquals(12, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(41);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(13, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());

		teamRechner = new SuperMeleeTeamRechner(42);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(14, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
	}

}
