/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Testet die einheitliche Breite der Mannschaftsspalten. */
class TeamSpaltenBreiteTest {

	@Test
	void spielernamen4cmJeSpieler() {
		assertThat(TeamSpaltenBreite.fuer(TeamAnzeige.SPIELERNAMEN, false, Formation.TRIPLETTE, 6000)).isEqualTo(12000);
		assertThat(TeamSpaltenBreite.fuer(TeamAnzeige.SPIELERNAMEN, false, Formation.DOUBLETTE, 3000)).isEqualTo(8000);
	}

	@Test
	void standardBreiteIstMindestbreite() {
		assertThat(TeamSpaltenBreite.fuer(TeamAnzeige.SPIELERNAMEN, false, Formation.TETE, 6000)).isEqualTo(6000);
	}

	@Test
	void nummernUndTeamnamenBehaltenStandardbreite() {
		assertThat(TeamSpaltenBreite.fuer(TeamAnzeige.NR, false, Formation.TRIPLETTE, 700)).isEqualTo(700);
		assertThat(TeamSpaltenBreite.fuer(TeamAnzeige.NAME, true, Formation.TRIPLETTE, 6000)).isEqualTo(6000);
	}

	@Test
	void teamnameOhneTeamnameSpalteZeigtSpielernamen() {
		assertThat(TeamSpaltenBreite.fuer(TeamAnzeige.NAME, false, Formation.TRIPLETTE, 6000)).isEqualTo(12000);
	}
}
