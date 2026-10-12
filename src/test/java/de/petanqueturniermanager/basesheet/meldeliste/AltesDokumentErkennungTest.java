/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AltesDokumentErkennungTest {

	private static final String UUID = "0b8c1f8e-3c55-4b7a-9d1e-2f6a7c9e1a01";

	@Test
	void nurReineZahlenIstAlt() {
		assertThat(AltesDokumentErkennung.istAlt(new String[][] { { "Nr" }, { "1" }, { "2" } }, new int[] { 0, 1, 2 }))
				.isTrue();
	}

	@Test
	void eineUuidMachtEsNeu() {
		String[][] formeln = { { "Nr" }, { NrZellFormel.team(UUID).formel(1) }, { "2" } };
		assertThat(AltesDokumentErkennung.istAlt(formeln, new int[] { 0, 1, 2 })).isFalse();
	}

	@Test
	void leereMeldelisteIstNichtAlt() {
		assertThat(AltesDokumentErkennung.istAlt(new String[][] { { "Nr" }, { "" } }, new int[] { 0, 0 })).isFalse();
		assertThat(AltesDokumentErkennung.istAlt(new String[0][0], new int[0])).isFalse();
	}
}
