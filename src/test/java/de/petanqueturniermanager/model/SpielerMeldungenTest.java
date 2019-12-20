/**
* Erstellung : 06.09.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

public class SpielerMeldungenTest {

	SpielerMeldungen meldungen;

	@Before
	public void setup() {
		meldungen = new SpielerMeldungen();
	}

	@Test
	public void testSpielerOhneTeam() throws Exception {

		for (int i = 1; i < 11; i++) {
			meldungen.addSpielerWennNichtVorhanden(Spieler.from(i));
		}
		assertEquals(10, meldungen.spieler().size());

		Team testTeam = Team.from(1);
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

	@Test
	public void testAddSpielerWennNichtVorhandenSpielerList() throws Exception {

		ArrayList<Spieler> testSpielerlist = new ArrayList<>();
		testSpielerlist.add(Spieler.from(2));
		testSpielerlist.add(Spieler.from(3));
		testSpielerlist.add(Spieler.from(2)); // doppelt
		testSpielerlist.add(Spieler.from(7));

		meldungen.addSpielerWennNichtVorhanden(testSpielerlist);
		assertThat(meldungen.spieler()).hasSize(3);
	}

	@Test
	public void testResetTeam() throws Exception {

		ArrayList<Spieler> testSpielerlist = new ArrayList<>();
		Team testTeam = Team.from(3);
		testSpielerlist.add(Spieler.from(2).setTeam(testTeam));
		testSpielerlist.add(Spieler.from(3).setTeam(testTeam));
		meldungen.addSpielerWennNichtVorhanden(testSpielerlist);

		for (Spieler testSpieler : meldungen.getSpielerList()) {
			assertThat(testSpieler.isIstInTeam()).isTrue();
		}

		meldungen.resetTeam();

		for (Spieler testSpieler : meldungen.getSpielerList()) {
			assertThat(testSpieler.isIstInTeam()).isFalse();
		}
	}

	@Test
	public void testRemoveSpieler() throws Exception {
		Spieler testSpieler2 = Spieler.from(2);
		meldungen.addSpielerWennNichtVorhanden(testSpieler2);
		assertThat(meldungen.size()).isEqualTo(1);
		meldungen.removeSpieler(Spieler.from(2)); // new instance, aber gleiche spieler nummer
		assertThat(meldungen.size()).isEqualTo(0);
	}
}
