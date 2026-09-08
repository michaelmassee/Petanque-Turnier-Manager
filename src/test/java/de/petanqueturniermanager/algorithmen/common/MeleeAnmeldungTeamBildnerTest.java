package de.petanqueturniermanager.algorithmen.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeSpieler;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeTeam;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Testet die Team-Bildung aus Mêlée-Anmeldungen: Team-Größen je Modus, SP-Bedingung und die
 * abgeleitete Team-Setzposition.
 */
public class MeleeAnmeldungTeamBildnerTest {

	@BeforeEach
	public void seedSetzen() {
		RandomSource.setSeed(42L);
	}

	@AfterEach
	public void seedZuruecksetzen() {
		RandomSource.reset();
	}

	// ---------------------------------------------------------------
	// Grenzfälle
	// ---------------------------------------------------------------

	@Test
	public void testKeineSpieler_liefertKeineTeams() {
		assertThat(MeleeAnmeldungTeamBildner.bildeTeams(List.of(), SuperMeleeMode.Triplette)).isEmpty();
	}

	@Test
	public void testEinSpieler_liefertKeineTeams() {
		assertThat(MeleeAnmeldungTeamBildner.bildeTeams(spieler(1), SuperMeleeMode.Triplette)).isEmpty();
	}

	@Test
	public void testZweiSpieler_liefertEineDoublette() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(2), SuperMeleeMode.Triplette);
		assertThat(teams).hasSize(1);
		assertThat(teams.get(0).spieler()).hasSize(2);
	}

	// ---------------------------------------------------------------
	// Team-Größen je Modus
	// ---------------------------------------------------------------

	@Test
	public void testZwoelfSpielerTripletteModus_liefertVierTripletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(12), SuperMeleeMode.Triplette);
		assertThat(teams).hasSize(4);
		assertThat(teams).allMatch(team -> team.spieler().size() == 3);
	}

	@Test
	public void testZwoelfSpielerDoubletteModus_liefertSechsDoubletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(12), SuperMeleeMode.Doublette);
		assertThat(teams).hasSize(6);
		assertThat(teams).allMatch(team -> team.spieler().size() == 2);
	}

	@Test
	public void testUngeradeAnzahl_alleSpielerWerdenVerteilt() {
		for (int anzahl = 2; anzahl <= 30; anzahl++) {
			List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(anzahl), SuperMeleeMode.Triplette);
			int verteilt = teams.stream().mapToInt(team -> team.spieler().size()).sum();
			assertThat(verteilt).as("verteilte Spieler bei %d Anmeldungen", anzahl).isEqualTo(anzahl);
			assertThat(teams).as("Teamgroessen bei %d Anmeldungen", anzahl)
					.allMatch(team -> team.spieler().size() >= 2);
		}
	}

	@Test
	public void testSiebenSpieler_wirdAlsTriplettePlusZweiDoublettenAufgeteilt() {
		// SuperMeleeTeamRechner kann 7 nicht aufteilen – der Bildner deckt den Fall explizit ab.
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(7), SuperMeleeMode.Triplette);
		assertThat(teams).hasSize(3);
		assertThat(teams.stream().mapToInt(team -> team.spieler().size()).sum()).isEqualTo(7);
		assertThat(teams).filteredOn(team -> team.spieler().size() == 3).hasSize(1);
	}

	// ---------------------------------------------------------------
	// Setzpositionen
	// ---------------------------------------------------------------

	@Test
	public void testGleicheSetzPosition_landetNichtImSelbenTeam() {
		// 4 Spieler mit SP 1 und 8 ohne SP: bei 4 Tripletten ist die Bedingung erfüllbar.
		List<MeleeSpieler> spieler = new ArrayList<>();
		for (int nr = 1; nr <= 4; nr++) {
			spieler.add(new MeleeSpieler(nr, "Vorname" + nr, "Nachname" + nr, 1));
		}
		for (int nr = 5; nr <= 12; nr++) {
			spieler.add(new MeleeSpieler(nr, "Vorname" + nr, "Nachname" + nr, 0));
		}

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler, SuperMeleeMode.Triplette);

		assertThat(teams).allSatisfy(team -> {
			long anzGesetzte = team.spieler().stream().filter(s -> s.setzPosition() > 0).count();
			assertThat(anzGesetzte).isLessThanOrEqualTo(1);
		});
	}

	@Test
	public void testTeamSetzPosition_istNiedrigsteVonNullVerschiedeneSpielerSetzPosition() {
		List<MeleeSpieler> spieler = List.of(
				new MeleeSpieler(1, "A", "Aa", 0),
				new MeleeSpieler(2, "B", "Bb", 5),
				new MeleeSpieler(3, "C", "Cc", 3));

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler, SuperMeleeMode.Triplette);

		assertThat(teams).hasSize(1);
		assertThat(teams.get(0).setzPosition()).isEqualTo(3);
	}

	@Test
	public void testTeamOhneGesetzteSpieler_hatSetzPositionNull() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(3), SuperMeleeMode.Triplette);
		assertThat(teams).hasSize(1);
		assertThat(teams.get(0).setzPosition()).isZero();
	}

	@Test
	public void testUnerfuellbareSetzPositionen_bildetTrotzdemTeams() {
		// Alle Spieler mit derselben SP – die Bedingung ist nicht erfüllbar, die Auslosung darf
		// deshalb nicht hart scheitern.
		List<MeleeSpieler> spieler = new ArrayList<>();
		for (int nr = 1; nr <= 6; nr++) {
			spieler.add(new MeleeSpieler(nr, "Vorname" + nr, "Nachname" + nr, 7));
		}

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler, SuperMeleeMode.Triplette);

		assertThat(teams).hasSize(2);
		assertThat(teams).allMatch(team -> team.setzPosition() == 7);
	}

	// ---------------------------------------------------------------

	private static List<MeleeSpieler> spieler(int anzahl) {
		List<MeleeSpieler> spieler = new ArrayList<>(anzahl);
		for (int nr = 1; nr <= anzahl; nr++) {
			spieler.add(new MeleeSpieler(nr, "Vorname" + nr, "Nachname" + nr, 0));
		}
		return spieler;
	}
}
