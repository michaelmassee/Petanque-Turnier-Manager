/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OnlineTournamentDtoTest {

	@Test
	void nurBeendetesTurnierIstAbgeschlossen() {
		assertThat(turnier("finished").istAbgeschlossen()).isTrue();
		assertThat(turnier("draft").istAbgeschlossen()).isFalse();
		assertThat(turnier("registration").istAbgeschlossen()).isFalse();
		assertThat(turnier("running").istAbgeschlossen()).isFalse();
	}

	@Test
	void andereFormationErsetztGespeicherteTete() {
		OnlineTournamentDto turnier = turnier("registration");
		turnier.formation = "tete";

		assertThat(turnier.formationsWert()).isEqualTo("tete");
		turnier.formationOther = true;
		assertThat(turnier.formationsWert()).isEqualTo(OnlineTournamentDto.FORMATION_ANDERE);
	}

	private static OnlineTournamentDto turnier(String status) {
		OnlineTournamentDto turnier = new OnlineTournamentDto();
		turnier.status = status;
		return turnier;
	}
}
