package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class CadrageRechnerTest {

	@Test
	public void testKonstruktor_zuWenigeTeams_wirftIllegalArgumentException() {
		assertThatThrownBy(() -> new CadrageRechner(1)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new CadrageRechner(0)).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testGrenzfall_zweiTeams_keineCadrage() {
		// 2 ist eine Zweierpotenz → keine Cadrage nötig
		var rechner = new CadrageRechner(2);
		assertThat(rechner.zielAnzahlTeams()).isEqualTo(2);
		assertThat(rechner.anzTeams()).isEqualTo(0);
		assertThat(rechner.anzFreilose()).isEqualTo(2);
	}

	// ---------------------------------------------------------------
	// zielAnzahlTeams
	// ---------------------------------------------------------------

	@Test
	public void testZielAnzahlTeams_exakteZweierpotenzen_keineAenderung() {
		assertThat(new CadrageRechner(4).zielAnzahlTeams()).isEqualTo(4);
		assertThat(new CadrageRechner(8).zielAnzahlTeams()).isEqualTo(8);
		assertThat(new CadrageRechner(16).zielAnzahlTeams()).isEqualTo(16);
		assertThat(new CadrageRechner(32).zielAnzahlTeams()).isEqualTo(32);
		assertThat(new CadrageRechner(64).zielAnzahlTeams()).isEqualTo(64);
		assertThat(new CadrageRechner(128).zielAnzahlTeams()).isEqualTo(128);
		assertThat(new CadrageRechner(256).zielAnzahlTeams()).isEqualTo(256);
	}

	@Test
	public void testZielAnzahlTeams_nichtZweierpotenzen_naechsteKleinere() {
		assertThat(new CadrageRechner(3).zielAnzahlTeams()).isEqualTo(2);
		assertThat(new CadrageRechner(5).zielAnzahlTeams()).isEqualTo(4);
		assertThat(new CadrageRechner(9).zielAnzahlTeams()).isEqualTo(8);
		assertThat(new CadrageRechner(15).zielAnzahlTeams()).isEqualTo(8);
		assertThat(new CadrageRechner(17).zielAnzahlTeams()).isEqualTo(16);
		assertThat(new CadrageRechner(129).zielAnzahlTeams()).isEqualTo(128);
	}

	@Test
	public void testZielAnzahlTeams_grosseWerte_ueber128() {
		// Fruehere Implementierung war auf max 128 (2^7) begrenzt
		assertThat(new CadrageRechner(200).zielAnzahlTeams()).isEqualTo(128);
		assertThat(new CadrageRechner(256).zielAnzahlTeams()).isEqualTo(256);
		assertThat(new CadrageRechner(257).zielAnzahlTeams()).isEqualTo(256);
		assertThat(new CadrageRechner(512).zielAnzahlTeams()).isEqualTo(512);
		assertThat(new CadrageRechner(1000).zielAnzahlTeams()).isEqualTo(512);
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

	// ---------------------------------------------------------------
	// anzTeams
	// ---------------------------------------------------------------

	@Test
	public void testAnzTeams_exakteZweierpotenzen_keineCadrage() {
		assertThat(new CadrageRechner(4).anzTeams()).isEqualTo(0);
		assertThat(new CadrageRechner(8).anzTeams()).isEqualTo(0);
		assertThat(new CadrageRechner(16).anzTeams()).isEqualTo(0);
		assertThat(new CadrageRechner(32).anzTeams()).isEqualTo(0);
		assertThat(new CadrageRechner(64).anzTeams()).isEqualTo(0);
		assertThat(new CadrageRechner(128).anzTeams()).isEqualTo(0);
	}

	@Test
	public void testAnzTeams_nichtZweierpotenzen() {
		// 5 Teams: (5-4)*2 = 2
		assertThat(new CadrageRechner(5).anzTeams()).isEqualTo(2);
		// 10 Teams: (10-8)*2 = 4
		assertThat(new CadrageRechner(10).anzTeams()).isEqualTo(4);
		// 17 Teams: (17-16)*2 = 2
		assertThat(new CadrageRechner(17).anzTeams()).isEqualTo(2);
		// 19 Teams: (19-16)*2 = 6
		assertThat(new CadrageRechner(19).anzTeams()).isEqualTo(6);
		// 15 Teams: (15-8)*2 = 14
		assertThat(new CadrageRechner(15).anzTeams()).isEqualTo(14);
	}

	@Test
	public void testAnzTeams_grosseWerte_ueber128() {
		// 200 Teams: (200-128)*2 = 144
		assertThat(new CadrageRechner(200).anzTeams()).isEqualTo(144);
		// 257 Teams: (257-256)*2 = 2
		assertThat(new CadrageRechner(257).anzTeams()).isEqualTo(2);
		// 1000 Teams: (1000-512)*2 = 976
		assertThat(new CadrageRechner(1000).anzTeams()).isEqualTo(976);
	}

	// ---------------------------------------------------------------
	// anzFreilose
	// ---------------------------------------------------------------

	@Test
	public void testAnzFreilose() {
		// exakte Zweierpotenz: alle haben Freilos (keine Cadrage)
		assertThat(new CadrageRechner(8).anzFreilose()).isEqualTo(8);
		assertThat(new CadrageRechner(16).anzFreilose()).isEqualTo(16);
		// 5 Teams: ziel=4, cadrage=2 → freilose=4-1=3
		assertThat(new CadrageRechner(5).anzFreilose()).isEqualTo(3);
		// 10 Teams: ziel=8, cadrage=4 → freilose=8-2=6
		assertThat(new CadrageRechner(10).anzFreilose()).isEqualTo(6);
		// 17 Teams: ziel=16, cadrage=2 → freilose=16-1=15
		assertThat(new CadrageRechner(17).anzFreilose()).isEqualTo(15);
	}

	@Test
	public void testAnzFreilose_plusCadrageGewinnerGleichZielAnzahl() {
		for (int anzTeams = 2; anzTeams <= 130; anzTeams++) {
			var rechner = new CadrageRechner(anzTeams);
			int cadrageGewinner = rechner.anzTeams() / 2;
			assertThat(rechner.anzFreilose() + cadrageGewinner)
					.as("freilose + cadrageGewinner muss zielAnzahlTeams ergeben fuer %d Teams", anzTeams)
					.isEqualTo(rechner.zielAnzahlTeams());
		}
	}

	@Test
	public void testAnzTeams_ergebnisIstImmerGerade() {
		for (int anzTeams = 2; anzTeams <= 130; anzTeams++) {
			assertThat(new CadrageRechner(anzTeams).anzTeams() % 2)
					.as("anzTeams() muss gerade sein fuer %d Teams", anzTeams)
					.isEqualTo(0);
		}
	}
}
