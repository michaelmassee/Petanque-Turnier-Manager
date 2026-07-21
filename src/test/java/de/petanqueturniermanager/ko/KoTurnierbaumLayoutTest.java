/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KoTurnierbaumLayoutTest {

	@Test
	void berechnetFinaleUndPlatz3MitGruppenHeader() {
		KoTurnierbaumLayout layout = KoTurnierbaumLayout.from(16, false, true, true, true);

		assertEquals(4, layout.numRunden());
		assertEquals(3, layout.teamAZeile(1, 0));
		assertEquals(4, layout.teamBZeile(1, 0));
		assertEquals(24, layout.teamAZeile(1, 7));
		assertEquals(25, layout.teamBZeile(1, 7));
		assertEquals(10, layout.finaleTeamAZeile());
		assertEquals(18, layout.finaleTeamBZeile());
		assertEquals(14, layout.siegerZeile());
		assertTrue(layout.hatSpielUmPlatz3());
		assertEquals(29, layout.platz3TeamAZeile());
		assertEquals(30, layout.platz3TeamBZeile());
		assertEquals(29, layout.platz3SiegerZeile());
	}

	@Test
	void beruecksichtigtBahnspaltenOhneCadrageNurInRunde1() {
		KoTurnierbaumLayout layout = KoTurnierbaumLayout.from(8, true, true, true, false);

		assertEquals(1, layout.teamSpalte(1));
		assertEquals(4, layout.teamSpalte(2));
		assertEquals(7, layout.teamSpalte(3));
		assertEquals(10, layout.siegerSpalte());
	}
}
