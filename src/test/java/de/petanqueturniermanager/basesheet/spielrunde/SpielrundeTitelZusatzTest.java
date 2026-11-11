/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Unit-Tests für {@link SpielrundeTitelZusatz}.
 */
class SpielrundeTitelZusatzTest {

	private static final String ZUSATZ = " – NICHT GEWERTET";
	private static final String TITEL = "3. Spielrunde";

	@Test
	void mitZusatz_haengtAn() {
		assertThat(SpielrundeTitelZusatz.mitZusatz(TITEL, ZUSATZ)).isEqualTo(TITEL + ZUSATZ);
	}

	@Test
	void mitZusatz_istIdempotent() {
		assertThat(SpielrundeTitelZusatz.mitZusatz(TITEL + ZUSATZ, ZUSATZ)).isEqualTo(TITEL + ZUSATZ);
	}

	@Test
	void ohneZusatz_entferntZusatzAmEnde() {
		assertThat(SpielrundeTitelZusatz.ohneZusatz(TITEL + ZUSATZ, ZUSATZ)).isEqualTo(TITEL);
	}

	@Test
	void ohneZusatz_laesstTitelOhneZusatzUnveraendert() {
		assertThat(SpielrundeTitelZusatz.ohneZusatz(TITEL, ZUSATZ)).isEqualTo(TITEL);
	}

	@Test
	void ohneZusatz_leererZusatzAendertNichts() {
		assertThat(SpielrundeTitelZusatz.ohneZusatz(TITEL, "")).isEqualTo(TITEL);
	}
}
