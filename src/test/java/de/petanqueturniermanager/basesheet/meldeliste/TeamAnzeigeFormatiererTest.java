/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TeamAnzeigeFormatiererTest {

	@Test
	void spielerMitVereinWerdenZusammengesetzt() {
		String[] triplette = { "7", "Anna", "Albatros", "BC A", "Berta", "Boules", "BC B", "Carl", "Carreau",
				"BC C" };

		assertThat(TeamAnzeigeFormatierer.formatiere(false, 3, true, triplette))
				.isEqualTo("Anna Albatros (BC A) / Berta Boules (BC B) / Carl Carreau (BC C)");
	}

	@Test
	void teamnameWirdGetrimmt() {
		assertThat(TeamAnzeigeFormatierer.formatiere(true, 2, false,
				new String[] { "4", " Les Tireurs ", "Anna", "Albatros" })).isEqualTo("Les Tireurs");
	}

	@Test
	void leereSpielerUndTeilnamenWerdenUebersprungen() {
		assertThat(TeamAnzeigeFormatierer.formatiere(false, 2, false, new String[] { "5", "Anna", "", "", "Boules" }))
				.isEqualTo("Anna / Boules");
	}

	@Test
	void vereinOhneNameErgibtKeineAnzeige() {
		assertThat(TeamAnzeigeFormatierer.formatiere(false, 1, true, new String[] { "1", "", "", "BC A" })).isEmpty();
	}

	@Test
	void fehlendeOderNullZellenErgebenLeereAnzeige() {
		assertThat(TeamAnzeigeFormatierer.formatiere(false, 2, false, null)).isEmpty();
		assertThat(TeamAnzeigeFormatierer.formatiere(true, 2, false, new String[] { "1" })).isEmpty();
		assertThat(TeamAnzeigeFormatierer.formatiere(false, 1, false, new String[] { "1", null, "Boules" }))
				.isEqualTo("Boules");
	}
}
