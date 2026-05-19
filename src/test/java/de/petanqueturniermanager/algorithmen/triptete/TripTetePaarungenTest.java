/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.model.TeamRangliste;

public class TripTetePaarungenTest {

	@Test
	public void jederGegenJedenLiefertAlleRunden() {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i <= 4; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		List<List<TeamPaarung>> runden = TripTetePaarungen.jederGegenJeden(meldungen);

		assertThat(runden).hasSize(3);
		assertThat(runden).allSatisfy(runde -> assertThat(runde).hasSize(2));
	}

	@Test
	public void koRundeErstellt() {
		TeamRangliste rangliste = new TeamRangliste();
		for (int i = 1; i <= 4; i++) {
			rangliste.add(Team.from(i));
		}

		FormeSpielrunde spielrunde = TripTetePaarungen.koRunde(rangliste);

		assertThat(spielrunde.getTeamPaarungen()).hasSize(2);
	}

	@Test
	public void bahnenProBegegnungIstZwei() {
		assertThat(TripTetePaarungen.BAHNEN_PRO_BEGEGNUNG).isEqualTo(2);
	}
}
