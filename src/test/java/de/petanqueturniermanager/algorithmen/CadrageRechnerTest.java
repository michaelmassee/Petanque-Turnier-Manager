package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class CadrageRechnerTest {

	@Test
	public void testKonstruktor_zuWenigeTeams_wirftIllegalArgumentException() {
		assertThatThrownBy(() -> new CadrageRechner(1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CadrageRechner(2)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testZielAnzahlTeams_einzelneWerte() {
		assertThat(new CadrageRechner(3).zielAnzahlTeams()).isEqualTo(2);
		assertThat(new CadrageRechner(4).zielAnzahlTeams()).isEqualTo(4);
		assertThat(new CadrageRechner(5).zielAnzahlTeams()).isEqualTo(4);
		assertThat(new CadrageRechner(8).zielAnzahlTeams()).isEqualTo(8);
		assertThat(new CadrageRechner(9).zielAnzahlTeams()).isEqualTo(8);
		assertThat(new CadrageRechner(16).zielAnzahlTeams()).isEqualTo(16);
		assertThat(new CadrageRechner(17).zielAnzahlTeams()).isEqualTo(16);
	}

	@Test
	public void testZielAnzahlTeams_bereich8bis15() {
		for (int anzTeams = 8; anzTeams < 16; anzTeams++) {
			assertThat(new CadrageRechner(anzTeams).zielAnzahlTeams())
					.as("zielAnzahlTeams fuer %d Teams", anzTeams)
					.isEqualTo(8);
		}
	}

	@Test
	public void testZielAnzahlTeams_bereich32bis63() {
		for (int anzTeams = 32; anzTeams < 64; anzTeams++) {
			assertThat(new CadrageRechner(anzTeams).zielAnzahlTeams())
					.as("zielAnzahlTeams fuer %d Teams", anzTeams)
					.isEqualTo(32);
		}
	}

	@Test
	public void testAnzTeams() {
		// 4 = 2^2: kein Cadrage noetig
		assertThat(new CadrageRechner(4).anzTeams()).isEqualTo(0);
		// 5 Teams: (5-4)*2 = 2
		assertThat(new CadrageRechner(5).anzTeams()).isEqualTo(2);
		// 10 Teams: (10-8)*2 = 4
		assertThat(new CadrageRechner(10).anzTeams()).isEqualTo(4);
		// 17 Teams: (17-16)*2 = 2
		assertThat(new CadrageRechner(17).anzTeams()).isEqualTo(2);
		// 16 = 2^4: kein Cadrage noetig
		assertThat(new CadrageRechner(16).anzTeams()).isEqualTo(0);
		// weitere Werte aus bestehendem Test
		assertThat(new CadrageRechner(19).anzTeams()).isEqualTo(6);
		assertThat(new CadrageRechner(15).anzTeams()).isEqualTo(14);
	}
}
