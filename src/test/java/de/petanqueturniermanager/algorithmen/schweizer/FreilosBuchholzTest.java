package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.petanqueturniermanager.schweizer.konfiguration.SchweizerFreilosBuchholzModus;

class FreilosBuchholzTest {

	/** Tabelle aus 03_Schweizer.md, 5.1: Runden, eigene Siege → BHZ- und FBHZ-Beitrag je Freilos. */
	@ParameterizedTest
	@CsvSource({ "1, 1, 0.0, 1.0", "2, 2, 0.5, 3.0", "3, 2, 1.0, 5.0", "5, 3, 2.0, 13.0" })
	void neutralLiefertBeitragDesFiktivenDurchschnittsgegners(int anzRunden, int eigeneSiege, double bhz,
			double fbhz) {
		var freilos = new FreilosBuchholz(SchweizerFreilosBuchholzModus.NEUTRAL, anzRunden);

		assertThat(freilos.bhzBeitrag()).isEqualTo(bhz);
		assertThat(freilos.fbhzBeitrag(eigeneSiege)).isEqualTo(fbhz);
	}

	@Test
	void nullLiefertKeinenBeitrag() {
		var freilos = new FreilosBuchholz(SchweizerFreilosBuchholzModus.NULL, 5);

		assertThat(freilos.bhzBeitrag()).isZero();
		assertThat(freilos.fbhzBeitrag(3)).isZero();
	}

	@Test
	void ohneGewerteteRundeKeinBeitrag() {
		var freilos = new FreilosBuchholz(SchweizerFreilosBuchholzModus.NEUTRAL, 0);

		assertThat(freilos.bhzBeitrag()).isZero();
		assertThat(freilos.fbhzBeitrag(1)).isZero();
	}

	@Test
	void negativeRundenzahlIstUngueltig() {
		assertThatThrownBy(() -> new FreilosBuchholz(SchweizerFreilosBuchholzModus.NEUTRAL, -1))
				.isInstanceOf(IllegalArgumentException.class);
	}
}
