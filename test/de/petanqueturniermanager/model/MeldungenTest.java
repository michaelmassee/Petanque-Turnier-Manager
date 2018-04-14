/**
* Erstellung : 06.09.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;

public class MeldungenTest {

	Meldungen meldungen;

	@Before
	public void setup() {
		this.meldungen = new Meldungen();
	}

	@Test
	public void testSpielerOhneTeam() throws Exception {

		for (int i = 1; i < 11; i++) {
			this.meldungen.addSpielerWennNichtVorhanden(new Spieler(i));
		}
		assertEquals(10, this.meldungen.spieler().size());

		Team testTeam = new Team(1);
		this.meldungen.findSpielerByNr(1).setTeam(testTeam);
		this.meldungen.findSpielerByNr(2).setTeam(testTeam);

		List<Spieler> result = this.meldungen.spielerOhneTeam();
		assertNotNull(result);
		assertEquals(8, result.size());
	}

	@Test
	public void testShifLeft() throws Exception {
		for (int i = 1; i < 4; i++) {
			this.meldungen.addSpielerWennNichtVorhanden(new Spieler(i));
		}

		assertEquals(1, this.meldungen.spieler().get(0).getNr());
		assertEquals(2, this.meldungen.spieler().get(1).getNr());
		assertEquals(3, this.meldungen.spieler().get(2).getNr());

		this.meldungen.shifLeft();

		assertEquals(2, this.meldungen.spieler().get(0).getNr());
		assertEquals(3, this.meldungen.spieler().get(1).getNr());
		assertEquals(1, this.meldungen.spieler().get(2).getNr());
	}

	@Test
	public void testAddSpielerWennNichtVorhandenSpieler() throws Exception {
		this.meldungen.addSpielerWennNichtVorhanden(new Spieler(1));
		assertThat(this.meldungen.spieler()).hasSize(1);
		this.meldungen.addSpielerWennNichtVorhanden(new Spieler(1));
		assertThat(this.meldungen.spieler()).hasSize(1);

		for (int cnt = 10; cnt < 20; cnt++) {
			this.meldungen.addSpielerWennNichtVorhanden(new Spieler(cnt));
		}
		assertThat(this.meldungen.spieler()).hasSize(11);

		this.meldungen.shuffle();
		assertThat(this.meldungen.spieler()).hasSize(11);
		this.meldungen.addSpielerWennNichtVorhanden(new Spieler(22));
		assertThat(this.meldungen.spieler()).hasSize(12);
	}
}
