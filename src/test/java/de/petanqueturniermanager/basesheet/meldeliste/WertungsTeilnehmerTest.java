/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;

class WertungsTeilnehmerTest {

	private static TeamMeldungen meldungen(int... teamNrn) {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int nr : teamNrn) {
			meldungen.addTeamWennNichtVorhanden(Team.from(nr));
		}
		return meldungen;
	}

	@Test
	void gelistetSindTeamsMitAktivWertUndTeamsMitPartie() {
		assertThat(WertungsTeilnehmer.gelistet(meldungen(1, 2, 3), Set.of(2, 3, 4)))
				.containsExactlyInAnyOrder(1, 2, 3, 4);
	}

	@Test
	void ohneAktivWertMitPartieSindNurTeamsMitPartieOhneAktivWert() {
		assertThat(WertungsTeilnehmer.ohneAktivWertMitPartie(meldungen(1, 2, 3), Set.of(5, 2, 4)))
				.containsExactly(4, 5);
	}

	@Test
	void ohnePartieGibtEsKeinenHinweis() {
		assertThat(WertungsTeilnehmer.ohneAktivWertMitPartie(meldungen(1, 2), Set.of())).isEmpty();
	}
}
