/*
 * Erstellung 10.11.2026 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Formeln des Liga-Spielplans: in der Liga gibt es kein Unentschieden, gleich viele Siege sind ein
 * Eingabefehler und dürfen nicht als 0 : 0 gewertet werden (Issue #27).
 */
class LigaSpielPlanPunkteFormelTest {

	@Test
	void punkteBleibenBeiGleichstandLeer() {
		assertThat(LigaSpielPlanSheet.formelPunkte("K3", "L3"))
				.isEqualTo("IF(OR(AND(ISBLANK(K3);ISBLANK(L3));K3=L3);\"\";IF(K3>L3;1;0))");
	}

	@Test
	void gleichstandRegelHatAbsoluteSpaltenUndRelativeZeile() {
		int zeile = LigaSpielPlanSheet.ERSTE_SPIELTAG_DATEN_ZEILE;
		assertThat(LigaSpielPlanSheet.formelSiegeGleichstand(zeile))
				.isEqualTo("AND(ISNUMBER($K3);ISNUMBER($L3);$K3=$L3)");
	}
}
