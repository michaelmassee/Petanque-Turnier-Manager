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

/**
 * Testet die Team-Bildung aus Mêlée-Anmeldungen: feste Team-Größe (Meldeliste-Formation),
 * korrekter Umgang mit nicht restlos teilbaren Spielerzahlen, SP-Bedingung und die abgeleitete
 * Team-Setzposition.
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

	private static List<MeleeSpieler> spieler(int anzahl) {
		List<MeleeSpieler> spieler = new ArrayList<>(anzahl);
		for (int nr = 1; nr <= anzahl; nr++) {
			spieler.add(new MeleeSpieler(nr, nr, "Vorname" + nr, "Nachname" + nr, 0));
		}
		return spieler;
	}
}
