/*
* Erstellung : 31.08.2017 / Michael Massee
**/

package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class SpielerTest {

	Spieler spieler;

	@BeforeEach
	public void setup() {
		spieler = Spieler.from(1);
	}

	@Test
	public void testEquals() throws Exception {
		assertTrue(Spieler.from(1).equals(Spieler.from(1)));
	}

	@Test
	public void testWarImTeamsetzPos() throws Exception {
		spieler.setSetzPos(1);

		Spieler spieler2 = Spieler.from(2).setSetzPos(1);
		boolean result = spieler.warImTeamMit(spieler2);
		assertThat(result).isTrue();

		// gleiche setz pos
		Spieler spieler3 = Spieler.from(3).setSetzPos(2);
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

	@Test
	public void testGleicheSetzPos() throws Exception {
		Spieler spieler2 = Spieler.from(2);
		spieler2.setSetzPos(5);
		spieler.setSetzPos(5);
		assertThat(spieler.gleicheSetzPos(spieler2)).isTrue();
	}

	@Test
	public void testSetUndGetTeam() throws Exception {
		Team team = Team.from(1);

		assertThat(spieler.isIstInTeam()).isFalse();
		assertThat(spieler.getTeam()).isNull();

		spieler.setTeam(team);
		assertThat(spieler.isIstInTeam()).isTrue();
		assertThat(spieler.getTeam()).isEqualTo(team);

		spieler.deleteTeam();
		assertThat(spieler.isIstInTeam()).isFalse();
		assertThat(spieler.getTeam()).isNull();
	}

	@Test
	public void testMitSpielerStr() throws Exception {
		assertThat(spieler.mitSpielerStr()).isEqualTo("(");

		spieler.addWarImTeamMitWennNichtVorhanden(Spieler.from(2));
		spieler.addWarImTeamMitWennNichtVorhanden(Spieler.from(3));

		assertThat(spieler.mitSpielerStr()).startsWith("(").contains("2").contains("3");
	}

	@Test
	public void testToString() throws Exception {
		spieler.addWarImTeamMitWennNichtVorhanden(Spieler.from(2));
		assertThat(spieler.toString()).contains("nr=1").contains("2");
	}

	@Test
	public void testAnzMalAusnahmeTeam() throws Exception {
		assertThat(spieler.getAnzMalAusnahmeTeam()).isZero();

		spieler.incAnzMalAusnahmeTeam();
		spieler.incAnzMalAusnahmeTeam();
		assertThat(spieler.getAnzMalAusnahmeTeam()).isEqualTo(2);

		spieler.resetAnzMalAusnahmeTeam();
		assertThat(spieler.getAnzMalAusnahmeTeam()).isZero();
	}

	@Test
	public void testResetHistorie() throws Exception {
		Spieler spieler2 = Spieler.from(2);
		spieler.addWarImTeamMitWennNichtVorhanden(spieler2);
		spieler.addGegner(spieler2);
		spieler.addWarImSpielMit(spieler2);
		spieler.incAnzMalAusnahmeTeam();

		spieler.resetHistorie();

		assertThat(spieler.warImTeamMit(spieler2)).isFalse();
		assertThat(spieler.warGegnerVon(spieler2)).isFalse();
		assertThat(spieler.warImSpielMit(spieler2)).isFalse();
		assertThat(spieler.getAnzMalAusnahmeTeam()).isZero();
	}

	@Test
	public void testHatteFreilos() throws Exception {
		assertThat(spieler.isHatteFreilos()).isFalse();
		spieler.setHatteFreilos(true);
		assertThat(spieler.isHatteFreilos()).isTrue();
	}
}
