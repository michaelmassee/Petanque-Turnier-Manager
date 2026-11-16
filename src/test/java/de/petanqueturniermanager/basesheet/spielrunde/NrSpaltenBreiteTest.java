/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Die Nummernspalte der Spielrunde muss die größte Nummer ganz zeigen. Regression: Bei „Nur laufende Nr“ war sie fest
 * 5 mm breit, ab Paarung 10 zeigte Calc „###“.
 */
class NrSpaltenBreiteTest {

	private static final int NR_CHARHEIGHT = 18;

	@Test
	void einstelligeNummernBehaltenDieMindestbreite() {
		assertThat(SpielrundeHelper.nrSpaltenBreite(9, 16, SpielrundeHelper.MIN_BREITE_LAUFENDE_NR))
				.isEqualTo(SpielrundeHelper.MIN_BREITE_LAUFENDE_NR);
		assertThat(SpielrundeHelper.nrSpaltenBreite(0, 16, SpielrundeHelper.MIN_BREITE_LAUFENDE_NR))
				.isEqualTo(SpielrundeHelper.MIN_BREITE_LAUFENDE_NR);
	}

	@Test
	void zweistelligeNummernMachenDieSpalteBreiter() {
		int einstellig = SpielrundeHelper.nrSpaltenBreite(9, NR_CHARHEIGHT, SpielrundeHelper.MIN_BREITE_LAUFENDE_NR);
		int zweistellig = SpielrundeHelper.nrSpaltenBreite(10, NR_CHARHEIGHT, SpielrundeHelper.MIN_BREITE_LAUFENDE_NR);

		// Zwei Ziffern in 18 pt brauchen gut 7 mm, dazu Innenabstand und Rahmen.
		assertThat(zweistellig).isGreaterThan(einstellig).isGreaterThanOrEqualTo(850);
	}

	@Test
	void dreistelligeNummernPassenAuchInDieBahnspalte() {
		assertThat(SpielrundeHelper.nrSpaltenBreite(99, NR_CHARHEIGHT, SpielrundeHelper.MIN_BREITE_BAHN))
				.isGreaterThanOrEqualTo(SpielrundeHelper.MIN_BREITE_BAHN);
		assertThat(SpielrundeHelper.nrSpaltenBreite(100, NR_CHARHEIGHT, SpielrundeHelper.MIN_BREITE_BAHN))
				.isGreaterThan(SpielrundeHelper.nrSpaltenBreite(99, NR_CHARHEIGHT, SpielrundeHelper.MIN_BREITE_BAHN));
	}
}
