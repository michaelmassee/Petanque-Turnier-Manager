package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TeamTest {

	private Team team;
	Spieler a;
	Spieler b;
	Spieler c;
	Spieler d;

	@BeforeEach
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
		Spieler nichtimTeamHatzusamengespieltmitNichtImTeamB = Spieler.from(7)
				.addWarImTeamMitWennNichtVorhanden(nichtimTeamHatzusamengespieltmitNichtImTeamA);
		boolean result = team.hatZusammenGespieltMit(nichtimTeamHatzusamengespieltmitNichtImTeamA);
		assertThat(result).isFalse();
		result = team.hatZusammenGespieltMit(nichtimTeamHatzusamengespieltmitNichtImTeamB);
		assertThat(result).isFalse();
	}

	@Test
	public void testRemoveGegner() throws Exception {
		Team team2 = Team.from(2);
		team.addGegner(team2);
		assertThat(team.getGegner()).hasSize(1);
		assertThat(team2.getGegner()).hasSize(1);
		team.removeGegner(team2);
		assertThat(team.getGegner()).isEmpty();
		assertThat(team2.getGegner()).isEmpty();
	}

	@Test
	public void testAddGegner() throws Exception {
		Team team2 = Team.from(2);
		team.addGegner(team2);
		team.addGegner(Team.from(2));
		team.addGegner(Team.from(1)); // sich selbst
		assertThat(team.getGegner()).hasSize(1);
		assertThat(team.getGegner()).contains(2);

		assertThat(team2.getGegner()).hasSize(1);
		assertThat(team2.getGegner()).contains(1);
	}

	@Test
	public void testHatAlsGegner_sichSelbst_liefertFalse() throws Exception {
		assertThat(team.hatAlsGegner(team)).isFalse();
	}

	@Test
	public void testAddSpielerWennNichtVorhanden_liste() throws Exception {
		Team neuesTeam = Team.from(9);
		Spieler s1 = Spieler.from(21);
		Spieler s2 = Spieler.from(22);

		neuesTeam.addSpielerWennNichtVorhanden(List.of(s1, s2));

		assertThat(neuesTeam.spieler()).containsExactly(s1, s2);
	}

	@Test
	public void testRemoveAlleSpieler() throws Exception {
		team.removeAlleSpieler();
		assertThat(team.spieler()).isEmpty();
	}

	@Test
	public void testIstSpielerImTeam() throws Exception {
		assertThat(team.istSpielerImTeam(a)).isTrue();
		assertThat(team.istSpielerImTeam(Spieler.from(99))).isFalse();
	}

	@Test
	public void testListeVonSpielerOhneSpieler() throws Exception {
		List<Spieler> ohneA = team.listeVonSpielerOhneSpieler(a);
		assertThat(ohneA).containsExactly(b, c);
	}

	@Test
	public void testToString() throws Exception {
		assertThat(team.toString()).contains("nr=1").contains("Spieler=[").contains("SetzPos=0");
	}
}
