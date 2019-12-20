package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class TeamTest {

	private Team team;

	@Before
	public void init() throws Exception {
		team = Team.from(1);
		Spieler a = Spieler.from(1);
		Spieler b = Spieler.from(2);
		Spieler c = Spieler.from(3);

		team.addSpielerWennNichtVorhanden(a);
		team.addSpielerWennNichtVorhanden(b);
		team.addSpielerWennNichtVorhanden(c);
	}

	@Test
	public void testHatNichtZusammenGespieltMitTeam() throws Exception {
		Spieler nichtimTeam = Spieler.from(4);
		boolean result = team.hatZusammenGespieltMit(nichtimTeam);
		assertThat(result).isFalse();
	}

	@Test
	public void testHatZusammenGespieltMitTeam() throws Exception {
		Spieler a = team.findSpielerByNr(1);
		Spieler nichtimTeamHatzusamengespieltmita = Spieler.from(5).addWarImTeamMitWennNichtVorhanden(a);
		boolean result = team.hatZusammenGespieltMit(nichtimTeamHatzusamengespieltmita);
		assertThat(result).isTrue();
	}

	@Test
	public void testHatZusammenGespieltAberNichtMitTeam() throws Exception {
		Spieler nichtimTeamHatzusamengespieltmitNichtImTeamA = Spieler.from(6);
		Spieler nichtimTeamHatzusamengespieltmitNichtImTeamB = Spieler.from(7).addWarImTeamMitWennNichtVorhanden(nichtimTeamHatzusamengespieltmitNichtImTeamA);
		boolean result = team.hatZusammenGespieltMit(nichtimTeamHatzusamengespieltmitNichtImTeamA);
		assertThat(result).isFalse();
		result = team.hatZusammenGespieltMit(nichtimTeamHatzusamengespieltmitNichtImTeamB);
		assertThat(result).isFalse();
	}
}
