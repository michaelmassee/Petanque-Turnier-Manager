/**
* Erstellung : 06.09.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class MeldungenTest {

	Meldungen meldungen;

	@Before
	public void setup() {
		meldungen = new Meldungen();
	}

	@Test
	public void testSpielerOhneTeam() throws Exception {

		for (int i = 1; i < 11; i++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(i));
		}
		assertEquals(10, meldungen.spieler().size());

		Team testTeam = new Team(1);
		meldungen.findSpielerByNr(1).setTeam(testTeam);
		meldungen.findSpielerByNr(2).setTeam(testTeam);

		List<Spieler> result = meldungen.spielerOhneTeam();
		assertNotNull(result);
		assertEquals(8, result.size());
	}

	@Test
	public void testShifLeft() throws Exception {
		for (int i = 1; i < 4; i++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(i));
		}

		assertEquals(1, meldungen.spieler().get(0).getNr());
		assertEquals(2, meldungen.spieler().get(1).getNr());
		assertEquals(3, meldungen.spieler().get(2).getNr());

		meldungen.shifLeft();

		assertEquals(2, meldungen.spieler().get(0).getNr());
		assertEquals(3, meldungen.spieler().get(1).getNr());
		assertEquals(1, meldungen.spieler().get(2).getNr());
	}

	@Test
	public void testAddSpielerWennNichtVorhandenSpieler() throws Exception {
		meldungen.addSpielerWennNichtVorhanden(Spieler.from(1));
		assertThat(meldungen.spieler()).hasSize(1);
		meldungen.addSpielerWennNichtVorhanden(Spieler.from(1));
		assertThat(meldungen.spieler()).hasSize(1);

		for (int cnt = 10; cnt < 20; cnt++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(cnt));
		}
		assertThat(meldungen.spieler()).hasSize(11);

		meldungen.shuffle();
		assertThat(meldungen.spieler()).hasSize(11);
		meldungen.addSpielerWennNichtVorhanden(Spieler.from(22));
		assertThat(meldungen.spieler()).hasSize(12);
	}
}
