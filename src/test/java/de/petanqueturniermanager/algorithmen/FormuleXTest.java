package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;

/**
 * Unit-Tests für den Formule X Algorithmus.
 */
public class FormuleXTest {

	FormuleX formuleX;

	@BeforeEach
	void setUp() {
		formuleX = new FormuleX();
	}

	// ========================================================================
	// Tests: Wertungsberechnung
	// ========================================================================

	@Test
	public void testWertungsscoreSieger() {
		// 13:7 → Sieger: 100 + 13 + 6 = 119
		FormuleXErgebnis ergebnis = new FormuleXErgebnis(1, 13, 7, List.of(2), false);
		int score = formuleX.berechneWertung(ergebnis, 4);
		assertThat(score).isEqualTo(119);
	}

	@Test
	public void testWertungsscoreVerlierer() {
		// 7:13 → Verlierer: nur eigene Punkte
		FormuleXErgebnis ergebnis = new FormuleXErgebnis(1, 7, 13, List.of(2), false);
		int score = formuleX.berechneWertung(ergebnis, 4);
		assertThat(score).isEqualTo(7);
	}

	@Test
	public void testWertungsscoreZeitlimit() {
		// 10:4 → Sieger: 100 + 10 + 6 = 116 (Zeitlimit-Spiel)
		FormuleXErgebnis ergebnis = new FormuleXErgebnis(1, 10, 4, List.of(2), false);
		int score = formuleX.berechneWertung(ergebnis, 4);
		assertThat(score).isEqualTo(116);
	}

	@Test
	public void testWertungsscoreFreilos() {
		// Freilos: immer 126 Punkte (fix)
		FormuleXErgebnis ergebnis = new FormuleXErgebnis(1, 0, 0, List.of(), true);
		int score = formuleX.berechneWertung(ergebnis, 4);
		assertThat(score).isEqualTo(126);
	}

	@Test
	public void testSiegaufschlag4Runden() {
		assertThat(formuleX.getSiegaufschlag(1)).isEqualTo(100);
		assertThat(formuleX.getSiegaufschlag(2)).isEqualTo(100);
		assertThat(formuleX.getSiegaufschlag(3)).isEqualTo(100);
		assertThat(formuleX.getSiegaufschlag(4)).isEqualTo(100);
	}

	@Test
	public void testSiegaufschlag5bis8Runden() {
		assertThat(formuleX.getSiegaufschlag(5)).isEqualTo(200);
		assertThat(formuleX.getSiegaufschlag(6)).isEqualTo(200);
		assertThat(formuleX.getSiegaufschlag(7)).isEqualTo(200);
		assertThat(formuleX.getSiegaufschlag(8)).isEqualTo(200);
	}

	@Test
	public void testSiegaufschlag9bis12Runden() {
		assertThat(formuleX.getSiegaufschlag(9)).isEqualTo(300);
		assertThat(formuleX.getSiegaufschlag(10)).isEqualTo(300);
		assertThat(formuleX.getSiegaufschlag(11)).isEqualTo(300);
		assertThat(formuleX.getSiegaufschlag(12)).isEqualTo(300);
	}

	// ========================================================================
	// Tests: Sortierung
	// ========================================================================

	@Test
	public void testSortiereNachWertung() {
		List<FormuleXErgebnis> ergebnisse = List.of(
				new FormuleXErgebnis(1, 7, 13, List.of(2), false), // Verlierer: 7 Punkte
				new FormuleXErgebnis(2, 13, 7, List.of(1), false), // Sieger: 119 Punkte
				new FormuleXErgebnis(3, 10, 4, List.of(4), false)  // Sieger: 116 Punkte
		);

		List<FormuleXErgebnis> sortiert = formuleX.sortiereNachWertung(ergebnisse, 4);

		assertThat(sortiert).extracting(FormuleXErgebnis::teamNr)
				.containsExactly(2, 3, 1); // 119, 116, 7
	}

	@Test
	public void testSortiereNachWertungMitTieBreaker() {
		// Gleicher Score → Punktedifferenz entscheidet
		List<FormuleXErgebnis> ergebnisse = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(2), false), // 119, Diff +6
				new FormuleXErgebnis(2, 13, 5, List.of(1), false)  // 119, Diff +8 → sollte zuerst kommen
		);

		List<FormuleXErgebnis> sortiert = formuleX.sortiereNachWertung(ergebnisse, 4);

		assertThat(sortiert).extracting(FormuleXErgebnis::teamNr)
				.containsExactly(2, 1); // gleiche Punkte, aber 2 hat bessere Differenz
	}

	@Test
	public void testSortiereNachWertungStabil() {
		// Gleicher Score + gleiche Differenz → TeamNr entscheidet (aufsteigend)
		List<FormuleXErgebnis> ergebnisse = List.of(
				new FormuleXErgebnis(5, 13, 7, List.of(3), false), // 119, Diff +6, Nr 5
				new FormuleXErgebnis(3, 13, 7, List.of(5), false)  // 119, Diff +6, Nr 3 → zuerst
		);

		List<FormuleXErgebnis> sortiert = formuleX.sortiereNachWertung(ergebnisse, 4);

		assertThat(sortiert).extracting(FormuleXErgebnis::teamNr)
				.containsExactly(3, 5); // gleiche Wertung, niedrigere Nr zuerst
	}

	// ========================================================================
	// Tests: Erste Runde
	// ========================================================================

	@Test
	public void testErsteRundeGerade() {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i < 7; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		List<TeamPaarung> ersteRunde = formuleX.ersteRunde(meldungen.teams());

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.stream().allMatch(p -> p.getB() != null)).isTrue(); // kein Freilos

		// Alle Teams sollten enthalten sein
		List<Team> teamList = ersteRunde.stream()
				.flatMap(p -> Stream.of(p.getA(), p.getB()))
				.sorted((t1, t2) -> Integer.compare(t1.getNr(), t2.getNr()))
				.collect(Collectors.toList());

		assertThat(teamList).hasSize(6);
		assertThat(teamList).extracting(Team::getNr).containsExactly(1, 2, 3, 4, 5, 6);
	}

	@Test
	public void testErsteRundeUngerade() {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i < 6; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		List<TeamPaarung> ersteRunde = formuleX.ersteRunde(meldungen.teams());

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull();
		assertThat(ersteRunde.get(1).getB()).isNotNull();
		assertThat(ersteRunde.get(2).isFreilos()).isTrue(); // Freilos am Ende

		// Flatten liefert nur gekoppelte Teams (Freilos-Paarung wird übersprungen)
		List<Team> teamList = formuleX.flattenTeampaarungen(ersteRunde);
		assertThat(teamList).hasSize(4);
		// Das Freilos-Team ist separat über die letzte Paarung prüfbar
		assertThat(ersteRunde.get(2).getA()).isNotNull();
	}

	// ========================================================================
	// Tests: Weitere Runde - Paarung
	// ========================================================================

	@Test
	public void testWeitereRunde1vs2() {
		// Rangliste: Team 1 (119), Team 2 (116), Team 3 (110), Team 4 (104)
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), false), // 119
				new FormuleXErgebnis(2, 10, 4, List.of(), false), // 116
				new FormuleXErgebnis(3, 9, 5, List.of(), false),  // 109
				new FormuleXErgebnis(4, 8, 6, List.of(), false)   // 102
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		assertThat(paarungen).hasSize(2);
		// 1vs2, 3vs4
		assertThat(paarungen.get(0).getA().getNr()).isEqualTo(1);
		assertThat(paarungen.get(0).getB().getNr()).isEqualTo(2);
		assertThat(paarungen.get(1).getA().getNr()).isEqualTo(3);
		assertThat(paarungen.get(1).getB().getNr()).isEqualTo(4);
	}

	// ========================================================================
	// Tests: Rematch-Vermeidung
	// ========================================================================

	@Test
	public void testRematchVermeidung() {
		// Team 1 und 2 haben schon gegeneinander gespielt
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(2), false), // 119, schon gegen 2
				new FormuleXErgebnis(2, 10, 4, List.of(1), false), // 116, schon gegen 1
				new FormuleXErgebnis(3, 9, 5, List.of(), false),   // 109
				new FormuleXErgebnis(4, 8, 6, List.of(), false)    // 102
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		assertThat(paarungen).hasSize(2);
		// Sollte 1vs3 und 2vs4 sein (Swap)
		assertThat(paarungen.get(0).getA().getNr()).isEqualTo(1);
		assertThat(paarungen.get(0).getB().getNr()).isEqualTo(3);
		assertThat(paarungen.get(1).getA().getNr()).isEqualTo(2);
		assertThat(paarungen.get(1).getB().getNr()).isEqualTo(4);
	}

	@Test
	public void testRematchKette() {
		// Komplexere Rematch-Situation: mehrere Paare betroffen
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(2, 3), false), // 119, gegen 2+3
				new FormuleXErgebnis(2, 10, 4, List.of(1, 4), false), // 116, gegen 1+4
				new FormuleXErgebnis(3, 9, 5, List.of(1), false),     // 109, gegen 1
				new FormuleXErgebnis(4, 8, 6, List.of(2), false),     // 102, gegen 2
				new FormuleXErgebnis(5, 7, 7, List.of(), false),      // 107
				new FormuleXErgebnis(6, 6, 8, List.of(), false)       // 106
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		// Sollte gültige Paarungen ohne Rematch produzieren (oder Rematch akzeptieren)
		assertThat(paarungen).hasSize(3);
		// Keine Paarung sollte doppelt vorkommen
		for (TeamPaarung paarung : paarungen) {
			if (paarung.getB() != null) {
				assertThat(paarung.getA().hatAlsGegner(paarung.getB())).isFalse();
			}
		}
	}

	// ========================================================================
	// Tests: BYE (Freilos)
	// ========================================================================

	@Test
	public void testUngeradeTeamsMitBYE() {
		// 5 Teams → BYE für schlechtest platziertes Team ohne BYE
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), false), // 119
				new FormuleXErgebnis(2, 10, 4, List.of(), false), // 116
				new FormuleXErgebnis(3, 9, 5, List.of(), false),  // 109
				new FormuleXErgebnis(4, 8, 6, List.of(), false),  // 102
				new FormuleXErgebnis(5, 7, 13, List.of(), false)  // 7 (schlechtest)
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		assertThat(paarungen).hasSize(3);
		// Team 5 sollte BYE haben
		assertThat(paarungen.get(2).isFreilos()).isTrue();
		assertThat(paarungen.get(2).getA().getNr()).isEqualTo(5);
	}

	@Test
	public void testBYENichtZweimalHintereinander() {
		// Team 5 hatte schon BYE → sollte nicht wieder BYE bekommen
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), false), // 119
				new FormuleXErgebnis(2, 10, 4, List.of(), false), // 116
				new FormuleXErgebnis(3, 9, 5, List.of(), false),  // 109
				new FormuleXErgebnis(4, 8, 6, List.of(), false),  // 102
				new FormuleXErgebnis(5, 7, 13, List.of(), true)   // 7, hatte schon BYE
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		// Team 4 sollte jetzt BYE bekommen (nächstes ohne BYE)
		assertThat(paarungen).hasSize(3);
		assertThat(paarungen.get(2).isFreilos()).isTrue();
		assertThat(paarungen.get(2).getA().getNr()).isEqualTo(4);
	}

	@Test
	public void testAlleHattenSchonBYE() {
		// Alle hatten schon BYE → Fallback: letztes Team
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), true), // 126
				new FormuleXErgebnis(2, 10, 4, List.of(), true), // 126
				new FormuleXErgebnis(3, 9, 5, List.of(), true),  // 126
				new FormuleXErgebnis(4, 8, 6, List.of(), true),  // 126
				new FormuleXErgebnis(5, 7, 13, List.of(), true)  // 126, letztes
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		// Fallback: letztes Team bekommt BYE
		assertThat(paarungen).hasSize(3);
		assertThat(paarungen.get(2).isFreilos()).isTrue();
		assertThat(paarungen.get(2).getA().getNr()).isEqualTo(5);
	}

	// ========================================================================
	// Tests: Wertungsberechnung – weitere Edge Cases
	// ========================================================================

	@Test
	public void testUnentschieden_istVerlierer() {
		// 5:5 → kein Sieg → nur eigene Punkte
		FormuleXErgebnis ergebnis = new FormuleXErgebnis(1, 5, 5, List.of(2), false);
		assertThat(ergebnis.istSieger()).isFalse();
		assertThat(formuleX.berechneWertung(ergebnis, 4)).isEqualTo(5);
	}

	@Test
	public void testFreilosIgnoriertPunkte() {
		// Freilos: immer 126, egal welche Punkte gesetzt sind
		FormuleXErgebnis ergebnis = new FormuleXErgebnis(1, 13, 0, List.of(), true);
		assertThat(formuleX.berechneWertung(ergebnis, 4)).isEqualTo(126);
	}

	@Test
	public void testSiegaufschlagGrenzwertVierZuFuenf() {
		assertThat(formuleX.getSiegaufschlag(4)).isEqualTo(100);
		assertThat(formuleX.getSiegaufschlag(5)).isEqualTo(200);
	}

	@Test
	public void testSiegaufschlagGrenzwertAchtZuNeun() {
		assertThat(formuleX.getSiegaufschlag(8)).isEqualTo(200);
		assertThat(formuleX.getSiegaufschlag(9)).isEqualTo(300);
	}

	@Test
	public void testSiegaufschlagUeberZwoelf() {
		// Mehr als 12 Runden → immer 300
		assertThat(formuleX.getSiegaufschlag(13)).isEqualTo(300);
		assertThat(formuleX.getSiegaufschlag(20)).isEqualTo(300);
	}

	// ========================================================================
	// Tests: Erste Runde – weitere Edge Cases
	// ========================================================================

	@Test
	public void testErsteRundeZweiTeams() {
		// Minimalfall: 2 Teams → 1 Paarung, kein Freilos
		TeamMeldungen meldungen = new TeamMeldungen();
		meldungen.addTeamWennNichtVorhanden(Team.from(1));
		meldungen.addTeamWennNichtVorhanden(Team.from(2));

		List<TeamPaarung> ersteRunde = formuleX.ersteRunde(meldungen.teams());

		assertThat(ersteRunde).hasSize(1);
		assertThat(ersteRunde.get(0).isFreilos()).isFalse();
	}

	// ========================================================================
	// Tests: Weitere Runde – weitere Edge Cases
	// ========================================================================

	@Test
	public void testWeitereRundeZweiTeams() {
		// Minimalfall: 2 Teams ohne Rematch → 1 Paarung
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), false), // 119
				new FormuleXErgebnis(2, 10, 4, List.of(), false)  // 116
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		assertThat(paarungen).hasSize(1);
		assertThat(paarungen.get(0).getA().getNr()).isEqualTo(1);
		assertThat(paarungen.get(0).getB().getNr()).isEqualTo(2);
	}

	@Test
	public void testWeitereRundeZweiTeamsRematchWirdAkzeptiert() {
		// Kein Swap möglich bei 2 Teams → Rematch wird als Fail-Safe akzeptiert
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(2), false), // schon gegen 2
				new FormuleXErgebnis(2, 10, 4, List.of(1), false)  // schon gegen 1
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		assertThat(paarungen).hasSize(1);
		assertThat(paarungen.get(0).getA().getNr()).isEqualTo(1);
		assertThat(paarungen.get(0).getB().getNr()).isEqualTo(2);
	}

	// ========================================================================
	// Tests: Swap-Strategie – zweiter Pfad (A,D) und (C,B)
	// ========================================================================

	@Test
	public void testSwapZweiterPfadAD_CB() {
		// Team 1 vs 2: Rematch → Swap wird versucht
		// Team 1 vs 3: gespielt → erster Swap-Pfad (A,C)+(B,D) schlägt fehl
		// Team 1 vs 4: nicht gespielt, Team 3 vs 2: nicht gespielt
		// → zweiter Swap-Pfad (A,D)+(C,B) greift: (1,4) und (3,2)
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(2, 3), false), // Rang 1: gegen 2+3
				new FormuleXErgebnis(2, 10, 4, List.of(1), false),    // Rang 2: gegen 1
				new FormuleXErgebnis(3, 9, 5, List.of(1), false),     // Rang 3: gegen 1
				new FormuleXErgebnis(4, 8, 6, List.of(), false)       // Rang 4: niemanden
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		assertThat(paarungen).hasSize(2);
		assertThat(paarungen.get(0).getA().getNr()).isEqualTo(1);
		assertThat(paarungen.get(0).getB().getNr()).isEqualTo(4);
		assertThat(paarungen.get(1).getA().getNr()).isEqualTo(3);
		assertThat(paarungen.get(1).getB().getNr()).isEqualTo(2);
	}

	// ========================================================================
	// Tests: BYE – weitere Edge Cases
	// ========================================================================

	@Test
	public void testFindeByeTeamNurLetzterHatBYE() {
		// Letztes Team hatte BYE → vorletztes bekommt BYE
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), false),
				new FormuleXErgebnis(2, 10, 4, List.of(), false),
				new FormuleXErgebnis(3, 9, 5, List.of(), false),
				new FormuleXErgebnis(4, 8, 6, List.of(), true)  // hatte BYE
		);
		List<Team> teams = rangliste.stream().map(e -> Team.from(e.teamNr())).toList();

		Team byeTeam = formuleX.findeByeTeam(rangliste, teams);

		assertThat(byeTeam.getNr()).isEqualTo(3);
	}

	// ========================================================================
	// Tests: Edge Cases
	// ========================================================================

	@Test
	public void testAlleHabenGegeneinanderGespielt() {
		// Edge Case: Alle Teams haben schon gegeneinander gespielt
		// → Rematch ist unvermeidbar, sollte akzeptiert werden
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(2, 3, 4), false),
				new FormuleXErgebnis(2, 10, 4, List.of(1, 3, 4), false),
				new FormuleXErgebnis(3, 9, 5, List.of(1, 2, 4), false),
				new FormuleXErgebnis(4, 8, 6, List.of(1, 2, 3), false)
		);

		List<TeamPaarung> paarungen = formuleX.weitereRunde(rangliste);

		// Sollte trotzdem Paarungen produzieren (mit Rematch)
		assertThat(paarungen).hasSize(2);
		// Keine Exception, keine Endlosschleife
	}

	@Test
	public void testFindeByeTeamAlleOhneBYE() {
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), false),
				new FormuleXErgebnis(2, 10, 4, List.of(), false),
				new FormuleXErgebnis(3, 9, 5, List.of(), false)
		);
		List<Team> teams = rangliste.stream().map(e -> Team.from(e.teamNr())).toList();

		Team byeTeam = formuleX.findeByeTeam(rangliste, teams);

		// Schlechtest platziertes Team (Nr 3) ohne BYE
		assertThat(byeTeam.getNr()).isEqualTo(3);
	}

	@Test
	public void testFindeByeTeamEinigeMitBYE() {
		List<FormuleXErgebnis> rangliste = List.of(
				new FormuleXErgebnis(1, 13, 7, List.of(), true),  // hatte BYE
				new FormuleXErgebnis(2, 10, 4, List.of(), false), // kein BYE
				new FormuleXErgebnis(3, 9, 5, List.of(), true),   // hatte BYE
				new FormuleXErgebnis(4, 8, 6, List.of(), false),  // kein BYE
				new FormuleXErgebnis(5, 7, 13, List.of(), false)  // kein BYE, schlechtest
		);
		List<Team> teams = rangliste.stream().map(e -> Team.from(e.teamNr())).toList();

		Team byeTeam = formuleX.findeByeTeam(rangliste, teams);

		// Schlechtest platziertes Team ohne BYE (Nr 5)
		assertThat(byeTeam.getNr()).isEqualTo(5);
	}

	// ========================================================================
	// Tests: Helper-Methoden
	// ========================================================================

	@Test
	public void testFlattenTeampaarungen() {
		List<TeamPaarung> paarungen = List.of(
				new TeamPaarung(1, 2),
				new TeamPaarung(3, 4),
				new TeamPaarung(5) // Freilos
		);

		List<Team> teams = formuleX.flattenTeampaarungen(paarungen);

		assertThat(teams).hasSize(4);
		assertThat(teams).extracting(Team::getNr).containsExactly(1, 2, 3, 4);
	}

	@Test
	public void testFlattenTeampaarungenLeereEingabe() {
		List<Team> teams = formuleX.flattenTeampaarungen(List.of());

		assertThat(teams).isEmpty();
	}

	@Test
	public void testFlattenTeampaarungenNurFreilos() {
		// Nur Freilos → leere Liste (Freilos-Paarungen werden übersprungen)
		List<TeamPaarung> paarungen = List.of(new TeamPaarung(1));

		List<Team> teams = formuleX.flattenTeampaarungen(paarungen);

		assertThat(teams).isEmpty();
	}

	@Test
	public void testHatGegeneinanderGespielt() {
		Map<Integer, FormuleXErgebnis> ergebnisMap = Map.of(
				1, new FormuleXErgebnis(1, 13, 7, List.of(2, 3), false),
				2, new FormuleXErgebnis(2, 10, 4, List.of(1), false),
				3, new FormuleXErgebnis(3, 9, 5, List.of(), false)
		);

		assertThat(formuleX.hatGegeneinanderGespielt(Team.from(1), Team.from(2), ergebnisMap)).isTrue();
		assertThat(formuleX.hatGegeneinanderGespielt(Team.from(1), Team.from(3), ergebnisMap)).isTrue();
		assertThat(formuleX.hatGegeneinanderGespielt(Team.from(2), Team.from(3), ergebnisMap)).isFalse();
	}

	@Test
	public void testHatGegeneinanderGespieltTeamNichtInMap() {
		// Team A nicht in der Map → false (kein Spiel bekannt)
		Map<Integer, FormuleXErgebnis> ergebnisMap = Map.of(
				2, new FormuleXErgebnis(2, 10, 4, List.of(3), false)
		);

		assertThat(formuleX.hatGegeneinanderGespielt(Team.from(1), Team.from(2), ergebnisMap)).isFalse();
	}

	@Test
	public void testHatGegeneinanderGespieltLeereGegnerliste() {
		// Team A in Map, aber ohne bisherige Gegner → false
		Map<Integer, FormuleXErgebnis> ergebnisMap = Map.of(
				1, new FormuleXErgebnis(1, 13, 7, List.of(), false)
		);

		assertThat(formuleX.hatGegeneinanderGespielt(Team.from(1), Team.from(2), ergebnisMap)).isFalse();
	}
}
