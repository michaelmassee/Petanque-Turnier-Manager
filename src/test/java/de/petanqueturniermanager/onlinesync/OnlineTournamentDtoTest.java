/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Nur nicht abgeschlossene Online-Turniere lassen sich mit einem Turnierdokument verbinden. */
class OnlineTournamentDtoTest {

	@Test
	void abgeschlossenesTurnierIstNichtVerbindbar() {
		assertThat(turnier("finished").istVerbindbar()).isFalse();
	}

	@Test
	void entwurfOffeneAnmeldungUndLaufendesTurnierSindVerbindbar() {
		assertThat(turnier("draft").istVerbindbar()).isTrue();
		assertThat(turnier("registration").istVerbindbar()).isTrue();
		assertThat(turnier("running").istVerbindbar()).isTrue();
	}

	private static OnlineTournamentDto turnier(String status) {
		OnlineTournamentDto turnier = new OnlineTournamentDto();
		turnier.status = status;
		return turnier;
	}
}
