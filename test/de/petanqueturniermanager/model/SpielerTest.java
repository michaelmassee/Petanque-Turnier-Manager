/**
* Erstellung : 31.08.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.model.Spieler;

public class SpielerTest {

	Spieler spieler;

	@Before
	public void setup() {
		spieler = new Spieler(1);
	}

	@Test
	public void testEquals() throws Exception {
		assertTrue(new Spieler(1).equals(new Spieler(1)));
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
}
