/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Unit-Tests für die Schlüssel-Auswertung in {@link SupermeleeSpielrundeWertungUmschalten}.
 */
class SupermeleeSpielrundeWertungUmschaltenTest {

	@Test
	void spieltagAusSchluessel_einstellig() {
		assertThat(SupermeleeSpielrundeWertungUmschalten
				.spieltagAusSchluessel(SheetMetadataHelper.schluesselSupermeleeSpielrunde(2, 3)).getNr())
				.isEqualTo(2);
	}

	@Test
	void spieltagAusSchluessel_mehrstellig() {
		assertThat(SupermeleeSpielrundeWertungUmschalten
				.spieltagAusSchluessel(SheetMetadataHelper.schluesselSupermeleeSpielrunde(12, 10)).getNr())
				.isEqualTo(12);
	}
}
