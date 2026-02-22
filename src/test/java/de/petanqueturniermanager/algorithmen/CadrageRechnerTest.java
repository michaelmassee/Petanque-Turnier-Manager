package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CadrageRechnerTest {

	@Test
	public void testZielAnzahlTeams() throws Exception {
		for (int anzTeams = 8; anzTeams < 16; anzTeams++) {
			System.out.println("anzTeams = " + anzTeams + " Expected = 8");
			assertThat(new CadrageRechner(anzTeams).zielAnzahlTeams()).isEqualTo(8);
		}

		for (int anzTeams = 32; anzTeams < 64; anzTeams++) {
			System.out.println("anzTeams = " + anzTeams + " Expected = 32");
			assertThat(new CadrageRechner(anzTeams).zielAnzahlTeams()).isEqualTo(32);
		}
	}

	@Test
	public void testAnzTeams() throws Exception {
		// Anzahl Cadrage Teams
		assertThat(new CadrageRechner(19).anzTeams()).isEqualTo(6);
		assertThat(new CadrageRechner(16).anzTeams()).isEqualTo(0);
		assertThat(new CadrageRechner(15).anzTeams()).isEqualTo(14); // 2*7
		assertThat(new CadrageRechner(8).anzTeams()).isEqualTo(0);
	}
}
