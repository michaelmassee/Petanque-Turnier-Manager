/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Testet das Umrechnen relativer Zeilenbezüge einer Regel-Formel auf eine andere Zeile. */
class FehlerregelAuswertungTest {

	@Test
	void verschiebtNurRelativeZeilenbezuege() {
		assertThat(FehlerregelAuswertung.fuerZeile("AND($B2<>\"\";SUMPRODUCT(($B$2:$B$1000=$B2)*($AA2=1))>CHAR(160))", 3))
				.isEqualTo("AND($B5<>\"\";SUMPRODUCT(($B$2:$B$1000=$B5)*($AA5=1))>CHAR(160))");
	}

	@Test
	void ohneVersatzUnveraendert() {
		assertThat(FehlerregelAuswertung.fuerZeile("$C4=$C$4", 0)).isEqualTo("$C4=$C$4");
	}
}
