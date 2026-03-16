package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableList;

import de.petanqueturniermanager.model.FormeSpielrunde;
import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.model.TeamRangliste;

public class KoRundeTeamPaarungenTest {

	// ──────────────────────────────────────────────────────────
	// Konstruktor-Validierung
	// ──────────────────────────────────────────────────────────

	@Test
	public void testConstructorNull() {
		assertThrows(NullPointerException.class, () -> new KoRundeTeamPaarungen(null));
	}

	@Test
	public void testConstructorLeereRangliste() {
		assertThrows(IllegalArgumentException.class, () -> new KoRundeTeamPaarungen(new TeamRangliste()));
	}

	@Test
	public void testConstructorUngerade() {
		assertThrows(IllegalArgumentException.class, () -> {
			TeamRangliste rangliste = new TeamRangliste();
			rangliste.add(Team.from(1));
			rangliste.add(Team.from(2));
			rangliste.add(Team.from(3));
			new KoRundeTeamPaarungen(rangliste);
		});
	}

	// ──────────────────────────────────────────────────────────
	// Normalfall: keine Vorpaarungen
	// ──────────────────────────────────────────────────────────

	@Test
	public void testZweiTeams_normalPaarung() {
		var rangliste = ranglisteVon(1, 2);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();

		assertThat(spielrunde.size()).isEqualTo(1);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 2);
	}

	@Test
	public void testVierTeams_normalPaarung() {
		// 1 vs 4, 2 vs 3
		var rangliste = ranglisteVon(1, 2, 3, 4);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();

		assertThat(spielrunde.size()).isEqualTo(2);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 4);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 3);
	}

	@Test
	public void testSechsTeams_normalPaarung() {
		// 1 vs 6, 2 vs 5, 3 vs 4
		var rangliste = ranglisteVon(1, 2, 3, 4, 5, 6);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();

		assertThat(spielrunde.size()).isEqualTo(3);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 6);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 5);
		assertPaarung(spielrunde.getTeamPaarungen().get(2), 3, 4);
	}

	@Test
	public void testAchtTeams_normalPaarung() {
		// 1 vs 8, 2 vs 7, 3 vs 6, 4 vs 5
		var rangliste = ranglisteVon(1, 2, 3, 4, 5, 6, 7, 8);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();

		assertThat(spielrunde.size()).isEqualTo(4);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 8);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 7);
		assertPaarung(spielrunde.getTeamPaarungen().get(2), 3, 6);
		assertPaarung(spielrunde.getTeamPaarungen().get(3), 4, 5);
	}

	@Test
	public void testNormalfall_keineDoppeltePaarung() {
		var rangliste = ranglisteVon(1, 2, 3, 4);
		var paarungen = new KoRundeTeamPaarungen(rangliste);
		paarungen.generatSpielRunde();

		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isFalse();
		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEmpty();
	}

	// ──────────────────────────────────────────────────────────
	// Ausweich: bereits gespielter Gegner → direktes Ausweichen
	// ──────────────────────────────────────────────────────────

	@Test
	public void testVierTeams_team1HatTeam4BereitsGespielt() {
		// 1 vs 4 bereits gespielt → 1 vs 3, 2 vs 4
		var rangliste = ranglisteVon(1, 2, 3, 4);
		rangliste.get(0).addGegner(rangliste.get(3)); // 1 vs 4

		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();

		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 3);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 4);
	}

	// ──────────────────────────────────────────────────────────
	// Tausch: bereits zugeordnetes B-Team wird freigegeben
	// ──────────────────────────────────────────────────────────

	@Test
	public void testVierTeams_tausch_team2HatTeam3BereitsGespielt() {
		// Team 2 hat Team 3 bereits gespielt → Tausch: 1 vs 3, 2 vs 4
		var rangliste = ranglisteVon(1, 2, 3, 4);
		rangliste.get(1).addGegner(rangliste.get(2)); // 2 vs 3

		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();
		ImmutableList<TeamPaarung> teamPaarungen = spielrunde.getTeamPaarungen();

		assertPaarung(teamPaarungen.get(0), 1, 3);
		assertPaarung(teamPaarungen.get(1), 2, 4);
	}

	@Test
	public void testSechsTeams_tausch_team2HatAlleUnterenGespielt() {
		// Teams 1..6, Team 2 hat 3, 4, 5 gespielt (nicht 6)
		// → 1 vs 6 zuerst, dann Tausch: 2 bekommt 6, 1 bekommt 5
		// → Ergebnis: 1 vs 5, 2 vs 6, 3 vs 4
		var rangliste = ranglisteVon(1, 2, 3, 4, 5, 6);
		Team team2 = rangliste.get(1);
		team2.addGegner(rangliste.get(2)); // 2 vs 3
		team2.addGegner(rangliste.get(3)); // 2 vs 4
		team2.addGegner(rangliste.get(4)); // 2 vs 5

		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generatSpielRunde();
		ImmutableList<TeamPaarung> paarungen = spielrunde.getTeamPaarungen();

		assertThat(spielrunde.size()).isEqualTo(3);
		assertPaarung(paarungen.get(0), 1, 5);
		assertPaarung(paarungen.get(1), 2, 6);
		assertPaarung(paarungen.get(2), 3, 4);
	}

	// ──────────────────────────────────────────────────────────
	// Doppel-Paarung (alle Kombinationen bereits gespielt)
	// ──────────────────────────────────────────────────────────

	@Test
	public void testVierTeams_doppelPaarung() {
		// Team 1 hat 3 und 4 gespielt, Team 2 hat 3 gespielt → keine valide Paarung für 2 möglich
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team team1 = rangliste.get(0);
		Team team2 = rangliste.get(1);
		Team team3 = rangliste.get(2);

		team1.addGegner(team3);
		team2.addGegner(team3);

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		FormeSpielrunde spielrunde = paarungen.generatSpielRunde();

		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isTrue();
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 4);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 3);
	}

	@Test
	public void testDoppelteGespieltePaarungen_korrektesFormat_eineDoppelPaarung() {
		// Team 1 hat 3, Team 2 hat 3 → Doppel-Paarung "2:3"
		var rangliste = ranglisteVon(1, 2, 3, 4);
		rangliste.get(0).addGegner(rangliste.get(2)); // 1 vs 3
		rangliste.get(1).addGegner(rangliste.get(2)); // 2 vs 3

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		paarungen.generatSpielRunde();

		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEqualTo("2:3");
	}

	@Test
	public void testDoppelteGespieltePaarungen_korrektesFormat_zweiDoppelPaarungen() {
		// Alle Teams haben gegeneinander gespielt → zwei Doppel-Paarungen "1:4 2:3"
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team t1 = rangliste.get(0), t2 = rangliste.get(1), t3 = rangliste.get(2), t4 = rangliste.get(3);
		t1.addGegner(t2); t1.addGegner(t3); t1.addGegner(t4);
		t2.addGegner(t3); t2.addGegner(t4);

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		paarungen.generatSpielRunde();

		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEqualTo("1:4 2:3");
	}

	@Test
	public void testDoppelteReset_nachZweitenAufruf() {
		// Erster Aufruf: Doppel-Paarung entsteht. Zweiter Aufruf: Reset des Flags.
		var rangliste = ranglisteVon(1, 2, 3, 4);
		rangliste.get(0).addGegner(rangliste.get(2)); // 1 vs 3
		rangliste.get(1).addGegner(rangliste.get(2)); // 2 vs 3

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		paarungen.generatSpielRunde();
		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isTrue();

		// Zweiter Aufruf mit gleichen Daten → Doppel immer noch vorhanden, aber String wird neu aufgebaut
		paarungen.generatSpielRunde();
		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEqualTo("2:3");
	}

	// ──────────────────────────────────────────────────────────
	// sucheGegnerFuer – direkte Unit-Tests
	// ──────────────────────────────────────────────────────────

	@Test
	public void testSucheGegnerFuer_findetLetzten() {
		var rangliste = ranglisteVon(1, 2, 3, 4);
		var paarungen = new KoRundeTeamPaarungen(rangliste);
		Team team1 = rangliste.get(0);

		Team gegner = paarungen.sucheGegnerFuer(team1, rangliste.getCloneTeamListe());

		assertThat(gegner).isEqualTo(rangliste.get(3)); // Team 4
	}

	@Test
	public void testSucheGegnerFuer_ueberspringstBereitsGespielte() {
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team team1 = rangliste.get(0);
		Team team4 = rangliste.get(3);
		team1.addGegner(team4); // 1 hat 4 bereits gespielt

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		Team gegner = paarungen.sucheGegnerFuer(team1, rangliste.getCloneTeamListe());

		assertThat(gegner).isEqualTo(rangliste.get(2)); // fällt auf Team 3 zurück
	}

	@Test
	public void testSucheGegnerFuer_alleHabenGespielt_gibtNullZurueck() {
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team team1 = rangliste.get(0);
		team1.addGegner(rangliste.get(1));
		team1.addGegner(rangliste.get(2));
		team1.addGegner(rangliste.get(3));

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		Team gegner = paarungen.sucheGegnerFuer(team1, rangliste.getCloneTeamListe());

		assertThat(gegner).isNull();
	}

	@Test
	public void testSucheGegnerFuer_nurEinKandidat() {
		// Nur 2 Teams in der Arbeitsliste
		var rangliste = ranglisteVon(1, 2);
		var paarungen = new KoRundeTeamPaarungen(rangliste);
		Team team1 = rangliste.get(0);

		Team gegner = paarungen.sucheGegnerFuer(team1, rangliste.getCloneTeamListe());

		assertThat(gegner).isEqualTo(rangliste.get(1));
	}

	// ──────────────────────────────────────────────────────────
	// kanntauschenMit – direkte Unit-Tests
	// ──────────────────────────────────────────────────────────

	@Test
	public void testKanntauschenMit_findetTausch() {
		// 4 Teams: 1, 2, 3, 4. Team 2 hat 3 gespielt.
		// Spielrunde enthält bereits 1 vs 4. Verbleibende Liste: [2, 3].
		// kanntauschenMit(2, [2,3], {1vs4}) → TauschTeams(3, 1vs4)
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team t2 = rangliste.get(1), t3 = rangliste.get(2);
		t2.addGegner(t3);

		FormeSpielrunde spielrunde = new FormeSpielrunde(1);
		spielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(rangliste.get(0), rangliste.get(3)));

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		List<Team> restListe = List.of(t2, t3);

		TauschTeams tausch = paarungen.kanntauschenMit(t2, restListe, spielrunde);

		assertThat(tausch).isNotNull();
		assertThat(tausch.teamAusRangliste()).isEqualTo(t3);
	}

	@Test
	public void testKanntauschenMit_keineTauschKandidaten_gibtNullZurueck() {
		// Spielrunde ist leer → kein B-Team vorhanden, kein Tausch möglich
		var rangliste = ranglisteVon(1, 2);
		Team t1 = rangliste.get(0), t2 = rangliste.get(1);
		t1.addGegner(t2);

		FormeSpielrunde leereSpielrunde = new FormeSpielrunde(1);
		var paarungen = new KoRundeTeamPaarungen(rangliste);

		TauschTeams tausch = paarungen.kanntauschenMit(t1, rangliste.getCloneTeamListe(), leereSpielrunde);

		assertThat(tausch).isNull();
	}

	@Test
	public void testKanntauschenMit_teamAHatBTeamBereitsGespielt_gibtNullZurueck() {
		// 4 Teams: 1, 2, 3, 4. 2 hat 3 gespielt, 2 hat 4 gespielt.
		// Spielrunde enthält 1 vs 4. Rest: [2, 3].
		// kanntauschenMit(2, [2,3], {1vs4}): B=4, aber 2 hat 4 gespielt → kein Tausch
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team t2 = rangliste.get(1), t3 = rangliste.get(2), t4 = rangliste.get(3);
		t2.addGegner(t3);
		t2.addGegner(t4);

		FormeSpielrunde spielrunde = new FormeSpielrunde(1);
		spielrunde.addPaarungWennNichtVorhanden(new TeamPaarung(rangliste.get(0), t4));

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		TauschTeams tausch = paarungen.kanntauschenMit(t2, List.of(t2, t3), spielrunde);

		assertThat(tausch).isNull();
	}

	// ──────────────────────────────────────────────────────────
	// Hilfsmethoden
	// ──────────────────────────────────────────────────────────

	private static TeamRangliste ranglisteVon(int... nummern) {
		var rangliste = new TeamRangliste();
		for (int nr : nummern) {
			rangliste.add(Team.from(nr));
		}
		return rangliste;
	}

	private static void assertPaarung(TeamPaarung paarung, int expectedNrA, int expectedNrB) {
		assertThat(paarung.getA().getNr()).as("Team A").isEqualTo(expectedNrA);
		assertThat(paarung.getB().getNr()).as("Team B").isEqualTo(expectedNrB);
	}
}
