package de.petanqueturniermanager.algorithmen.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

	@Test
	public void testGetTeamRangListe_liefertUebergebeneRangliste() {
		TeamRangliste rangliste = new TeamRangliste();
		rangliste.add(Team.from(1));
		rangliste.add(Team.from(2));

		KoRundeTeamPaarungen koRunde = new KoRundeTeamPaarungen(rangliste);

		assertThat(koRunde.getTeamRangListe()).isEqualTo(rangliste);
	}

	// ──────────────────────────────────────────────────────────
	// Normalfall: keine Vorpaarungen
	// ──────────────────────────────────────────────────────────

	@Test
	public void testZweiTeams_normalPaarung() {
		var rangliste = ranglisteVon(1, 2);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();

		assertThat(spielrunde.size()).isEqualTo(1);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 2);
	}

	@Test
	public void testVierTeams_normalPaarung() {
		// 1 vs 4, 2 vs 3
		var rangliste = ranglisteVon(1, 2, 3, 4);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();

		assertThat(spielrunde.size()).isEqualTo(2);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 4);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 3);
	}

	@Test
	public void testSechsTeams_normalPaarung() {
		// 1 vs 6, 2 vs 5, 3 vs 4
		var rangliste = ranglisteVon(1, 2, 3, 4, 5, 6);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();

		assertThat(spielrunde.size()).isEqualTo(3);
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 6);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 5);
		assertPaarung(spielrunde.getTeamPaarungen().get(2), 3, 4);
	}

	@Test
	public void testAchtTeams_normalPaarung() {
		// 1 vs 8, 2 vs 7, 3 vs 6, 4 vs 5
		var rangliste = ranglisteVon(1, 2, 3, 4, 5, 6, 7, 8);
		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();

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
		paarungen.generiereSpielrunde();

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

		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();

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

		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();
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

		FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();
		ImmutableList<TeamPaarung> paarungen = spielrunde.getTeamPaarungen();

		assertThat(spielrunde.size()).isEqualTo(3);
		assertPaarung(paarungen.get(0), 1, 5);
		assertPaarung(paarungen.get(1), 2, 6);
		assertPaarung(paarungen.get(2), 3, 4);
	}

	// ──────────────────────────────────────────────────────────
	// Wiederholungen nur, wenn unvermeidbar
	// ──────────────────────────────────────────────────────────

	@Test
	public void testVierTeams_vermeidbareWiederholung_wirdVermieden() {
		// 1 und 2 haben beide schon gegen 3 gespielt. 1–4 ließe nur noch 2–3 übrig (Wiederholung);
		// ohne Wiederholung geht es mit 1–2, 3–4.
		var rangliste = ranglisteVon(1, 2, 3, 4);
		rangliste.get(0).addGegner(rangliste.get(2)); // 1 vs 3
		rangliste.get(1).addGegner(rangliste.get(2)); // 2 vs 3

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		FormeSpielrunde spielrunde = paarungen.generiereSpielrunde();

		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isFalse();
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 2);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 3, 4);
	}

	@Test
	public void testSechsTeams_tauschUeberMehrerePaare() {
		// 1 nimmt 6, 2 nimmt 5; dann bleiben 3 und 4, die schon gespielt haben. Ein einzelner Tausch mit 1–6 oder
		// 2–5 hilft nicht, weil 3 und 4 auch gegen 5 und 6 gespielt haben – ohne Wiederholung geht es nur, wenn
		// 1 und 2 die Teams 3 und 4 nehmen: 1–4, 2–3, 5–6.
		var rangliste = ranglisteVon(1, 2, 3, 4, 5, 6);
		Team t3 = rangliste.get(2);
		Team t4 = rangliste.get(3);
		t3.addGegner(t4);
		t3.addGegner(rangliste.get(4));
		t3.addGegner(rangliste.get(5));
		t4.addGegner(rangliste.get(4));
		t4.addGegner(rangliste.get(5));

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		FormeSpielrunde spielrunde = paarungen.generiereSpielrunde();

		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isFalse();
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 4);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 3);
		assertPaarung(spielrunde.getTeamPaarungen().get(2), 5, 6);
	}

	@Test
	public void testZufaelligeHistorien_nurUnvermeidbareWiederholungen() {
		Random random = new Random(20261029L);
		for (int fall = 0; fall < 400; fall++) {
			int anzahl = 4 + 2 * random.nextInt(4);
			int runden = 1 + random.nextInt(anzahl - 2);
			boolean[][] gespielt = WiederholungsMinimum.zufaelligeHistorie(anzahl, runden, random);
			var rangliste = ranglisteVon(IntStream.rangeClosed(1, anzahl).toArray());
			for (int a = 0; a < anzahl; a++) {
				for (int b = a + 1; b < anzahl; b++) {
					if (gespielt[a][b]) {
						rangliste.get(a).addGegner(rangliste.get(b));
					}
				}
			}

			FormeSpielrunde spielrunde = new KoRundeTeamPaarungen(rangliste).generiereSpielrunde();

			List<TeamPaarung> teamPaarungen = spielrunde.getTeamPaarungen();
			assertThat(teamPaarungen.stream().flatMap(p -> Stream.of(p.getA().getNr(), p.getB().getNr())).sorted())
					.as("Fall %d: jedes Team genau einmal", fall)
					.containsExactlyElementsOf(IntStream.rangeClosed(1, anzahl).boxed().toList());
			assertThat(teamPaarungen.stream().filter(p -> gespielt[p.getA().getNr() - 1][p.getB().getNr() - 1]).count())
					.as("Fall %d: %d Teams nach %d Runden", fall, anzahl, runden)
					.isEqualTo(WiederholungsMinimum.berechne(anzahl, (a, b) -> gespielt[a][b]));
		}
	}

	// ──────────────────────────────────────────────────────────
	// Doppel-Paarung (unvermeidbar)
	// ──────────────────────────────────────────────────────────

	@Test
	public void testDoppelteGespieltePaarungen_korrektesFormat_eineDoppelPaarung() {
		// Team 1 hat gegen alle gespielt → genau eine Wiederholung, Team 1 bekommt das letzte Team: "1:4"
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team t1 = rangliste.get(0);
		t1.addGegner(rangliste.get(1));
		t1.addGegner(rangliste.get(2));
		t1.addGegner(rangliste.get(3));

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		FormeSpielrunde spielrunde = paarungen.generiereSpielrunde();

		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isTrue();
		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEqualTo("1:4");
		assertPaarung(spielrunde.getTeamPaarungen().get(0), 1, 4);
		assertPaarung(spielrunde.getTeamPaarungen().get(1), 2, 3);
	}

	@Test
	public void testDoppelteGespieltePaarungen_korrektesFormat_zweiDoppelPaarungen() {
		// Alle Teams haben gegeneinander gespielt → zwei Doppel-Paarungen "1:4 2:3"
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team t1 = rangliste.get(0);
		Team t2 = rangliste.get(1);
		Team t3 = rangliste.get(2);
		Team t4 = rangliste.get(3);
		t1.addGegner(t2);
		t1.addGegner(t3);
		t1.addGegner(t4);
		t2.addGegner(t3);
		t2.addGegner(t4);
		t3.addGegner(t4);

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		paarungen.generiereSpielrunde();

		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEqualTo("1:4 2:3");
	}

	@Test
	public void testDoppelteReset_nachZweitenAufruf() {
		// Team 1 hat gegen alle gespielt → Doppel-Paarung; der zweite Aufruf baut die Meldung neu auf statt anzuhängen
		var rangliste = ranglisteVon(1, 2, 3, 4);
		Team t1 = rangliste.get(0);
		t1.addGegner(rangliste.get(1));
		t1.addGegner(rangliste.get(2));
		t1.addGegner(rangliste.get(3));

		var paarungen = new KoRundeTeamPaarungen(rangliste);
		paarungen.generiereSpielrunde();
		assertThat(paarungen.isDoppelteGespieltePaarungenVorhanden()).isTrue();

		paarungen.generiereSpielrunde();
		assertThat(paarungen.getDoppelteGespieltePaarungen()).isEqualTo("1:4");
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
