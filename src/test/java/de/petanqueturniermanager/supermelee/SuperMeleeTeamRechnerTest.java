package de.petanqueturniermanager.supermelee;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

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
			assertEquals(anzahlMeldungen, teamRechner.getAnzTriplette() * 3 + teamRechner.getAnzDoublette() * 2, fehlrmldg);
			if (nurDoublette.contains(anzahlMeldungen)) {
				assertEquals(true, teamRechner.isNurDoubletteMoeglich(), fehlrmldg);
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
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(0, teamRechner.getAnzTriplette());
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

	@Test
	public void testCalcTeamsDoubletteAuffuellenMitTriplette() throws Exception {
		SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(4, SuperMeleeMode.Doublette);
		assertEquals(2, teamRechner.getAnzDoublette());
		assertEquals(0, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(5, SuperMeleeMode.Doublette);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(6, SuperMeleeMode.Doublette);
		assertEquals(0, teamRechner.getAnzDoublette());
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
		assertEquals(true, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(8, SuperMeleeMode.Doublette);
		assertEquals(4, teamRechner.getAnzDoublette());
		assertEquals(0, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(9, SuperMeleeMode.Doublette);
		assertEquals(3, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(10, SuperMeleeMode.Doublette);
		assertEquals(2, teamRechner.getAnzDoublette());
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(11, SuperMeleeMode.Doublette);
		assertEquals(1, teamRechner.getAnzDoublette());
		assertEquals(3, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(12, SuperMeleeMode.Doublette);
		assertEquals(6, teamRechner.getAnzDoublette());
		assertEquals(0, teamRechner.getAnzTriplette());
		assertEquals(true, teamRechner.isNurDoubletteMoeglich());
		assertEquals(true, teamRechner.isNurTripletteMoeglich());

		// -----------------------------
		teamRechner = new SuperMeleeTeamRechner(13, SuperMeleeMode.Doublette);
		assertEquals(5, teamRechner.getAnzDoublette());
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(false, teamRechner.isNurDoubletteMoeglich());
		assertEquals(false, teamRechner.isNurTripletteMoeglich());
	}

	@Test
	public void testCalcTeamsTripletteAuffuellenMitDoublette() throws Exception {
		// rest 0: nur Triplette (6 Spieler → 2 Tripl, 0 Doubl)
		SuperMeleeTeamRechner teamRechner = new SuperMeleeTeamRechner(6);
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(0, teamRechner.getAnzDoublette());

		// rest 1: 13 Spieler → 1 Tripl, 5 Doubl
		teamRechner = new SuperMeleeTeamRechner(13);
		assertEquals(1, teamRechner.getAnzTriplette());
		assertEquals(5, teamRechner.getAnzDoublette());

		// rest 2: 14 Spieler → 2 Tripl, 4 Doubl
		teamRechner = new SuperMeleeTeamRechner(14);
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(4, teamRechner.getAnzDoublette());

		// rest 3: 15 Spieler → 3 Tripl, 3 Doubl
		teamRechner = new SuperMeleeTeamRechner(15);
		assertEquals(3, teamRechner.getAnzTriplette());
		assertEquals(3, teamRechner.getAnzDoublette());

		// rest 4: 10 Spieler → 2 Tripl, 2 Doubl
		teamRechner = new SuperMeleeTeamRechner(10);
		assertEquals(2, teamRechner.getAnzTriplette());
		assertEquals(2, teamRechner.getAnzDoublette());

		// rest 5: 11 Spieler → 3 Tripl, 1 Doubl
		teamRechner = new SuperMeleeTeamRechner(11);
		assertEquals(3, teamRechner.getAnzTriplette());
		assertEquals(1, teamRechner.getAnzDoublette());
	}

	@Test
	public void testGetAnzSpielerUndGetMode() {
		SuperMeleeTeamRechner triplette = new SuperMeleeTeamRechner(10);
		assertEquals(10, triplette.getAnzSpieler());
		assertEquals(SuperMeleeMode.Triplette, triplette.getMode());

		SuperMeleeTeamRechner doublette = new SuperMeleeTeamRechner(10, SuperMeleeMode.Doublette);
		assertEquals(10, doublette.getAnzSpieler());
		assertEquals(SuperMeleeMode.Doublette, doublette.getMode());
	}

	@Test
	public void testValideAnzahlSpieler() {
		assertFalse(new SuperMeleeTeamRechner(7).valideAnzahlSpieler());
		assertTrue(new SuperMeleeTeamRechner(6).valideAnzahlSpieler());
		assertTrue(new SuperMeleeTeamRechner(8).valideAnzahlSpieler());
		assertTrue(new SuperMeleeTeamRechner(10).valideAnzahlSpieler());
	}

	@Test
	public void testGetAnzPaarungenUndGetAnzBahnen() {
		// Triplette-Modus: 12 Spieler → 4 Triplette + 0 Doublette = 4 Paarungen, 2 Bahnen
		SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(12);
		assertEquals(rechner.getAnzTriplette() + rechner.getAnzDoublette(), rechner.getAnzPaarungen());
		assertEquals(rechner.getAnzPaarungen() / 2, rechner.getAnzBahnen());
		assertEquals(4, rechner.getAnzPaarungen());
		assertEquals(2, rechner.getAnzBahnen());

		// Triplette-Modus: 10 Spieler → 2 Triplette + 2 Doublette = 4 Paarungen, 2 Bahnen
		rechner = new SuperMeleeTeamRechner(10);
		assertEquals(rechner.getAnzTriplette() + rechner.getAnzDoublette(), rechner.getAnzPaarungen());
		assertEquals(rechner.getAnzPaarungen() / 2, rechner.getAnzBahnen());
		assertEquals(4, rechner.getAnzPaarungen());
		assertEquals(2, rechner.getAnzBahnen());

		// Doublette-Modus: 12 Spieler → 6 Doublette = 6 Paarungen, 3 Bahnen
		rechner = new SuperMeleeTeamRechner(12, SuperMeleeMode.Doublette);
		assertEquals(rechner.getAnzTriplette() + rechner.getAnzDoublette(), rechner.getAnzPaarungen());
		assertEquals(rechner.getAnzPaarungen() / 2, rechner.getAnzBahnen());
		assertEquals(6, rechner.getAnzPaarungen());
		assertEquals(3, rechner.getAnzBahnen());
	}

	@Test
	public void testIsNurTripletteMoeglich() {
		assertTrue(new SuperMeleeTeamRechner(6).isNurTripletteMoeglich());
		assertTrue(new SuperMeleeTeamRechner(12).isNurTripletteMoeglich());
		assertTrue(new SuperMeleeTeamRechner(18).isNurTripletteMoeglich());
		assertFalse(new SuperMeleeTeamRechner(8).isNurTripletteMoeglich());
		assertFalse(new SuperMeleeTeamRechner(10).isNurTripletteMoeglich());
		assertFalse(new SuperMeleeTeamRechner(13).isNurTripletteMoeglich());
	}

	@Test
	public void testIsNurDoubletteMoeglichVal() {
		assertEquals(1, new SuperMeleeTeamRechner(8).isNurDoubletteMoeglichVal());
		assertEquals(1, new SuperMeleeTeamRechner(12).isNurDoubletteMoeglichVal());
		assertEquals(0, new SuperMeleeTeamRechner(10).isNurDoubletteMoeglichVal());
		assertEquals(0, new SuperMeleeTeamRechner(9).isNurDoubletteMoeglichVal());
	}

	@Test
	public void testGetAnzahlTripletteWennNurTriplette() {
		assertEquals(2, new SuperMeleeTeamRechner(6).getAnzahlTripletteWennNurTriplette());
		assertEquals(4, new SuperMeleeTeamRechner(12).getAnzahlTripletteWennNurTriplette());
		assertEquals(6, new SuperMeleeTeamRechner(18).getAnzahlTripletteWennNurTriplette());
		assertEquals(0, new SuperMeleeTeamRechner(10).getAnzahlTripletteWennNurTriplette());
	}

	@Test
	public void testGetAnzahlDoubletteWennNurDoublette() {
		assertEquals(4, new SuperMeleeTeamRechner(8).getAnzahlDoubletteWennNurDoublette());
		assertEquals(6, new SuperMeleeTeamRechner(12).getAnzahlDoubletteWennNurDoublette());
		assertEquals(10, new SuperMeleeTeamRechner(20).getAnzahlDoubletteWennNurDoublette());
		assertEquals(0, new SuperMeleeTeamRechner(9).getAnzahlDoubletteWennNurDoublette());
	}

	@Test
	public void testTeamRechnerDoubletteModeInvarianteFuerGroessereZahlen() {
		for (int anzahlMeldungen = 14; anzahlMeldungen <= 50; anzahlMeldungen++) {
			SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(anzahlMeldungen, SuperMeleeMode.Doublette);
			String fehlrmldg = "Doublette-Modus: Fehler bei AnzahlSpieler " + anzahlMeldungen;
			assertEquals(anzahlMeldungen, rechner.getAnzTriplette() * 3 + rechner.getAnzDoublette() * 2, fehlrmldg);
		}
	}

	@Test
	public void testTeamRechnerDoubletteModeStichproben() {
		// 16 Spieler – nur Doublette möglich (16 % 4 == 0)
		SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(16, SuperMeleeMode.Doublette);
		assertEquals(8, rechner.getAnzDoublette());
		assertEquals(0, rechner.getAnzTriplette());
		assertTrue(rechner.isNurDoubletteMoeglich());

		// 20 Spieler – nur Doublette möglich (20 % 4 == 0)
		rechner = new SuperMeleeTeamRechner(20, SuperMeleeMode.Doublette);
		assertEquals(10, rechner.getAnzDoublette());
		assertEquals(0, rechner.getAnzTriplette());
		assertTrue(rechner.isNurDoubletteMoeglich());

		// 14 Spieler – gemischt (14 % 4 != 0, 14 % 6 != 0)
		rechner = new SuperMeleeTeamRechner(14, SuperMeleeMode.Doublette);
		assertEquals(14, rechner.getAnzTriplette() * 3 + rechner.getAnzDoublette() * 2);

		// 15 Spieler – gemischt
		rechner = new SuperMeleeTeamRechner(15, SuperMeleeMode.Doublette);
		assertEquals(15, rechner.getAnzTriplette() * 3 + rechner.getAnzDoublette() * 2);
	}

	@Test
	public void testUngueltigeAnzSpielerWirftException() {
		assertThrows(IllegalArgumentException.class, () -> new SuperMeleeTeamRechner(0));
		assertThrows(IllegalArgumentException.class, () -> new SuperMeleeTeamRechner(-1));
		assertThrows(IllegalArgumentException.class, () -> new SuperMeleeTeamRechner(0, SuperMeleeMode.Doublette));
		assertThrows(IllegalArgumentException.class, () -> new SuperMeleeTeamRechner(-1, SuperMeleeMode.Triplette));
	}

}
