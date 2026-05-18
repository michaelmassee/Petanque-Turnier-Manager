/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;

public class TripTeteAufstellungTest {

	@Test
	public void fuerTeamWaehltTeteUndDoublette() throws AlgorithmenException {
		Team team = teamMit3Spielern(1, 10, 11, 12);

		TripTeteAufstellung aufstellung = TripTeteAufstellung.fuerTeam(team, 11);

		assertThat(aufstellung.tete().getNr()).isEqualTo(11);
		assertThat(aufstellung.doublette()).extracting(Spieler::getNr).containsExactlyInAnyOrder(10, 12);
	}

	@Test
	public void fuerTeamMitFalscherSpielerNrWirftException() throws AlgorithmenException {
		Team team = teamMit3Spielern(1, 10, 11, 12);

		assertThatThrownBy(() -> TripTeteAufstellung.fuerTeam(team, 99))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("99");
	}

	@Test
	public void fuerTeamMitFalscherSpielerAnzahlWirftException() throws AlgorithmenException {
		Team team = Team.from(1);
		team.addSpielerWennNichtVorhanden(Spieler.from(10));
		team.addSpielerWennNichtVorhanden(Spieler.from(11));

		assertThatThrownBy(() -> TripTeteAufstellung.fuerTeam(team, 10))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("3 Spieler");
	}

	@Test
	public void direkteKonstruktionMitDuplikatWirft() {
		Spieler a = Spieler.from(1);
		Spieler b = Spieler.from(2);

		assertThatThrownBy(() -> new TripTeteAufstellung(a, a, b))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TripTeteAufstellung(a, b, a))
				.isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> new TripTeteAufstellung(a, b, b))
				.isInstanceOf(IllegalArgumentException.class);
	}

	private Team teamMit3Spielern(int teamNr, int s1, int s2, int s3) throws AlgorithmenException {
		Team team = Team.from(teamNr);
		team.addSpielerWennNichtVorhanden(Spieler.from(s1));
		team.addSpielerWennNichtVorhanden(Spieler.from(s2));
		team.addSpielerWennNichtVorhanden(Spieler.from(s3));
		return team;
	}
}
