package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class TeamTest {

	private Team team;
	Spieler a;
	Spieler b;
	Spieler c;
	Spieler d;

	@Before
	public void init() throws Exception {
		team = Team.from(1);
		a = Spieler.from(1);
		b = Spieler.from(2);
		c = Spieler.from(3);

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

	@Test
	public void testRemoveGegner() throws Exception {
		Team team2 = Team.from(2);
		team.addGegner(team2);
		assertThat(team.getGegner().size()).isEqualTo(1);
		assertThat(team2.getGegner().size()).isEqualTo(1);
		team.removeGegner(team2);
		assertThat(team.getGegner().size()).isEqualTo(0);
		assertThat(team2.getGegner().size()).isEqualTo(0);
	}

	@Test
	public void testAddGegner() throws Exception {
		Team team2 = Team.from(2);
		team.addGegner(team2);
		team.addGegner(Team.from(2));
		team.addGegner(Team.from(1)); // sich selbst
		assertThat(team.getGegner().size()).isEqualTo(1);
		assertThat(team.getGegner().contains(2));

		assertThat(team2.getGegner().size()).isEqualTo(1);
		assertThat(team2.getGegner().contains(1));
	}
}
