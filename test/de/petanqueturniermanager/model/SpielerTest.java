/**
* Erstellung : 31.08.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class SpielerTest {

	Spieler spieler;

	@Before
	public void setup() {
		spieler = Spieler.from(1);
	}

	@Test
	public void testEquals() throws Exception {
		assertTrue(Spieler.from(1).equals(Spieler.from(1)));
	}

	@Test
	public void testWarImTeam() throws Exception {
		spieler.setSetzPos(1);

		Spieler spieler2 = Spieler.from(spieler).setNr(2);
		boolean result = spieler.warImTeamMit(spieler2);
		assertThat(result).isTrue();

		// gleiche setz pos
		Spieler spieler3 = Spieler.from(spieler).setNr(3).setSetzPos(2);
		assertThat(spieler.warImTeamMit(spieler3)).isFalse();

		spieler3.addWarImTeamMitWennNichtVorhanden(spieler);
		assertThat(spieler.warImTeamMit(spieler3)).isTrue();
	}

	@Test
	public void testDeleteWarImTeam() throws Exception {
		Spieler spieler2 = Spieler.from(2);
		Spieler spieler3 = Spieler.from(3);
		Spieler spieler4 = Spieler.from(4);

		spieler.addWarImTeamMitWennNichtVorhanden(spieler2);
		spieler.addWarImTeamMitWennNichtVorhanden(spieler3);
		spieler.addWarImTeamMitWennNichtVorhanden(spieler4);
		assertThat(spieler.warImTeamMit(spieler3)).isTrue();
		assertThat(spieler.warImTeamMit(spieler2)).isTrue();
		assertThat(spieler.warImTeamMit(spieler4)).isTrue();

		spieler.deleteWarImTeam(spieler3);
		assertThat(spieler.warImTeamMit(spieler3)).isFalse();
		assertThat(spieler.warImTeamMit(spieler2)).isTrue();
		assertThat(spieler.warImTeamMit(spieler4)).isTrue();
	}
}
