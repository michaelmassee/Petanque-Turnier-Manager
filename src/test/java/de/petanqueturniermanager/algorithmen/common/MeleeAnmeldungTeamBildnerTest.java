package de.petanqueturniermanager.algorithmen.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeSpieler;
import de.petanqueturniermanager.algorithmen.common.MeleeAnmeldungTeamBildner.MeleeTeam;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Testet die Team-Bildung aus Mêlée-Anmeldungen: feste Team-Größe (Meldeliste-Formation),
 * gemischte Doubletten/Triplettes ohne Freilos, korrekter Umgang mit nicht restlos teilbaren
 * Spielerzahlen, SP-Bedingung und die abgeleitete Team-Setzposition.
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
		assertThat(MeleeAnmeldungTeamBildner.bildeTeams(List.of(), 3)).isEmpty();
	}

	@Test
	public void testEinSpieler_liefertKeineTeams() {
		assertThat(MeleeAnmeldungTeamBildner.bildeTeams(spieler(1), 2)).isEmpty();
	}

	@Test
	public void testZweiSpieler_liefertEineDoublette() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(2), 2);
		assertThat(teams).hasSize(1);
		assertThat(teams.get(0).spieler()).hasSize(2);
	}

	@Test
	public void testZuKleineTeamGroesse_wirftException() {
		assertThatThrownBy(() -> MeleeAnmeldungTeamBildner.bildeTeams(spieler(4), 1))
				.isInstanceOf(IllegalArgumentException.class);
	}

	// ---------------------------------------------------------------
	// Feste Team-Größe
	// ---------------------------------------------------------------

	@Test
	public void testZwoelfSpielerTeamGroesseDrei_liefertVierTripletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(12), 3);
		assertThat(teams).hasSize(4);
		assertThat(teams).allMatch(team -> team.spieler().size() == 3);
	}

	@Test
	public void testZwoelfSpielerTeamGroesseZwei_liefertSechsDoubletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(12), 2);
		assertThat(teams).hasSize(6);
		assertThat(teams).allMatch(team -> team.spieler().size() == 2);
	}

	/**
	 * Reicht die Spielerzahl nicht für eine ganze Anzahl Teams, bleiben die überzähligen Spieler
	 * unverteilt – es wird niemals ein Team gebildet, das von der vorgegebenen Größe abweicht
	 * (Regression: 7 Spieler bei Formation Doublette müssen 3 volle Doubletten ergeben und einen
	 * offenen Rest, statt eines ungültigen 3er-Teams).
	 */
	@Test
	public void testNichtRestlosTeilbareAnzahl_lässtRestUnverteilt() {
		for (int teamGroesse = 2; teamGroesse <= 3; teamGroesse++) {
			int finaleTeamGroesse = teamGroesse;
			for (int anzahl = 1; anzahl <= 20; anzahl++) {
				List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(anzahl), finaleTeamGroesse);
				int erwarteteAnzTeams = anzahl / finaleTeamGroesse;
				assertThat(teams).as("Teamanzahl bei %d Spielern, Teamgröße %d", anzahl, finaleTeamGroesse)
						.hasSize(erwarteteAnzTeams);
				assertThat(teams).as("Teamgrößen bei %d Spielern, Teamgröße %d", anzahl, finaleTeamGroesse)
						.allMatch(team -> team.spieler().size() == finaleTeamGroesse);
				int verteilt = teams.stream().mapToInt(team -> team.spieler().size()).sum();
				assertThat(verteilt).as("verteilte Spieler bei %d Spielern, Teamgröße %d", anzahl, finaleTeamGroesse)
						.isEqualTo(erwarteteAnzTeams * finaleTeamGroesse);
			}
		}
	}

	@Test
	public void testSiebenSpielerTeamGroesseZwei_liefertDreiDoublettenPlusOffenenRest() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(7), 2);
		assertThat(teams).hasSize(3);
		assertThat(teams).allMatch(team -> team.spieler().size() == 2);
		List<Integer> verwendeteZeilen = teams.stream().flatMap(t -> t.spieler().stream())
				.map(MeleeSpieler::zeile).toList();
		assertThat(verwendeteZeilen).hasSize(6).doesNotHaveDuplicates();
	}

	/**
	 * Regression: nicht irgendein Spieler darf offen bleiben, sondern immer die zuletzt in der
	 * Liste stehenden (bei der Mêlée-Anmeldung: die zuletzt eingecheckten). Bug-Report: bei 7
	 * Spielern (Nr. 1-7) blieb per Zufall Nr. 5 offen, obwohl Nr. 7 zuletzt in der Liste stand.
	 */
	@Test
	public void testNichtRestlosTeilbareAnzahl_lässtImmerDieLetztenOffen() {
		for (int wiederholung = 0; wiederholung < 20; wiederholung++) {
			List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(7), 2);
			List<Integer> verwendeteNrn = teams.stream().flatMap(t -> t.spieler().stream())
					.map(MeleeSpieler::nr).toList();
			assertThat(verwendeteNrn).as("Durchlauf %d: verwendet werden muessen genau die ersten 6 (Nr. 1-6)",
					wiederholung).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
		}
	}

	// ---------------------------------------------------------------
	// Gemischt ohne Freilos
	// ---------------------------------------------------------------

	@Test
	public void testOhneFreilos_liefertGeradeTeamzahlMitSupermeleeAufteilung() {
		for (SuperMeleeMode mode : SuperMeleeMode.values()) {
			for (int anzahl = 4; anzahl <= 40; anzahl++) {
				if (anzahl == 7) {
					continue;
				}
				List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(anzahl), mode);
				SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(anzahl, mode);
				assertThat(teams.size() % 2).as("gerade Teamzahl bei %d Spielern, %s", anzahl, mode).isZero();
				assertThat(teams.stream().filter(team -> team.spieler().size() == 3).count())
						.as("Triplettes bei %d Spielern, %s", anzahl, mode).isEqualTo(rechner.getAnzTriplette());
				assertThat(teams.stream().filter(team -> team.spieler().size() == 2).count())
						.as("Doubletten bei %d Spielern, %s", anzahl, mode).isEqualTo(rechner.getAnzDoublette());
				assertThat(alleNummern(teams)).as("alle Spieler verteilt bei %d Spielern, %s", anzahl, mode)
						.hasSize(anzahl).doesNotHaveDuplicates();
			}
		}
	}

	@Test
	public void testOhneFreilosDoublette_zehnSpieler_liefertZweiTriplettesUndZweiDoubletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(10), SuperMeleeMode.Doublette);
		assertThat(teams).extracting(team -> team.spieler().size()).containsExactlyInAnyOrder(3, 3, 2, 2);
	}

	@Test
	public void testOhneFreilosTriplette_zehnSpieler_liefertZweiTriplettesUndZweiDoubletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(10), SuperMeleeMode.Triplette);
		assertThat(teams).extracting(team -> team.spieler().size()).containsExactlyInAnyOrder(3, 3, 2, 2);
	}

	@Test
	public void testOhneFreilosTriplette_zwoelfSpieler_liefertVierTriplettes() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(12), SuperMeleeMode.Triplette);
		assertThat(teams).hasSize(4).allMatch(team -> team.spieler().size() == 3);
	}

	@Test
	public void testOhneFreilosDoublette_zwoelfSpieler_liefertSechsDoubletten() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(12), SuperMeleeMode.Doublette);
		assertThat(teams).hasSize(6).allMatch(team -> team.spieler().size() == 2);
	}

	/** 7 Spieler lassen sich nicht ohne Freilos aufteilen: 6 werden verteilt, der zuletzt gemeldete bleibt offen. */
	@Test
	public void testOhneFreilos_siebenSpieler_lässtDenLetztenOffen() {
		for (SuperMeleeMode mode : SuperMeleeMode.values()) {
			List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(7), mode);
			assertThat(teams.size() % 2).as(mode.name()).isZero();
			assertThat(alleNummern(teams)).as(mode.name()).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6);
		}
	}

	@Test
	public void testOhneFreilos_wenigerAlsVierSpieler_liefertKeineTeams() {
		for (int anzahl = 0; anzahl < 4; anzahl++) {
			assertThat(MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler(anzahl), SuperMeleeMode.Doublette))
					.as("%d Spieler", anzahl).isEmpty();
		}
	}

	@Test
	public void testAnzahlVerwendbarOhneFreilos() {
		assertThat(MeleeAnmeldungTeamBildner.anzahlVerwendbarOhneFreilos(3)).isZero();
		assertThat(MeleeAnmeldungTeamBildner.anzahlVerwendbarOhneFreilos(4)).isEqualTo(4);
		assertThat(MeleeAnmeldungTeamBildner.anzahlVerwendbarOhneFreilos(7)).isEqualTo(6);
		assertThat(MeleeAnmeldungTeamBildner.anzahlVerwendbarOhneFreilos(9)).isEqualTo(9);
	}

	@Test
	public void testOhneFreilos_gleicheSetzPositionLandetNichtImSelbenTeam() {
		// 10 Spieler Doublette ohne Freilos → 2 Triplettes + 2 Doubletten; 4 Spieler mit SP 1.
		List<MeleeSpieler> spieler = new ArrayList<>();
		for (int nr = 1; nr <= 10; nr++) {
			spieler.add(new MeleeSpieler(nr, nr, "Vorname" + nr, "Nachname" + nr, nr <= 4 ? 1 : 0));
		}

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeamsOhneFreilos(spieler, SuperMeleeMode.Doublette);

		assertThat(teams).hasSize(4).allSatisfy(team -> {
			long anzGesetzte = team.spieler().stream().filter(s -> s.setzPosition() > 0).count();
			assertThat(anzGesetzte).isLessThanOrEqualTo(1);
		});
	}

	// ---------------------------------------------------------------
	// Setzpositionen
	// ---------------------------------------------------------------

	@Test
	public void testGleicheSetzPosition_landetNichtImSelbenTeam() {
		// 4 Spieler mit SP 1 und 8 ohne SP: bei 4 Tripletten ist die Bedingung erfüllbar.
		List<MeleeSpieler> spieler = new ArrayList<>();
		for (int nr = 1; nr <= 4; nr++) {
			spieler.add(new MeleeSpieler(nr, nr, "Vorname" + nr, "Nachname" + nr, 1));
		}
		for (int nr = 5; nr <= 12; nr++) {
			spieler.add(new MeleeSpieler(nr, nr, "Vorname" + nr, "Nachname" + nr, 0));
		}

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler, 3);

		assertThat(teams).allSatisfy(team -> {
			long anzGesetzte = team.spieler().stream().filter(s -> s.setzPosition() > 0).count();
			assertThat(anzGesetzte).isLessThanOrEqualTo(1);
		});
	}

	@Test
	public void testTeamSetzPosition_istNiedrigsteVonNullVerschiedeneSpielerSetzPosition() {
		List<MeleeSpieler> spieler = List.of(
				new MeleeSpieler(1, 1, "A", "Aa", 0),
				new MeleeSpieler(2, 2, "B", "Bb", 5),
				new MeleeSpieler(3, 3, "C", "Cc", 3));

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler, 3);

		assertThat(teams).hasSize(1);
		assertThat(teams.get(0).setzPosition()).isEqualTo(3);
	}

	@Test
	public void testTeamOhneGesetzteSpieler_hatSetzPositionNull() {
		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler(3), 3);
		assertThat(teams).hasSize(1);
		assertThat(teams.get(0).setzPosition()).isZero();
	}

	@Test
	public void testUnerfuellbareSetzPositionen_bildetTrotzdemTeams() {
		// Alle Spieler mit derselben SP – die Bedingung ist nicht erfüllbar, die Auslosung darf
		// deshalb nicht hart scheitern.
		List<MeleeSpieler> spieler = new ArrayList<>();
		for (int nr = 1; nr <= 6; nr++) {
			spieler.add(new MeleeSpieler(nr, nr, "Vorname" + nr, "Nachname" + nr, 7));
		}

		List<MeleeTeam> teams = MeleeAnmeldungTeamBildner.bildeTeams(spieler, 3);

		assertThat(teams).hasSize(2);
		assertThat(teams).allMatch(team -> team.setzPosition() == 7);
	}

	// ---------------------------------------------------------------

	private static List<Integer> alleNummern(List<MeleeTeam> teams) {
		return teams.stream().flatMap(t -> t.spieler().stream()).map(MeleeSpieler::nr).toList();
	}

	private static List<MeleeSpieler> spieler(int anzahl) {
		List<MeleeSpieler> spieler = new ArrayList<>(anzahl);
		for (int nr = 1; nr <= anzahl; nr++) {
			spieler.add(new MeleeSpieler(nr, nr, "Vorname" + nr, "Nachname" + nr, 0));
		}
		return spieler;
	}
}
