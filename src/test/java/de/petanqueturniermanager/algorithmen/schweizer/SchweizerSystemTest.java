package de.petanqueturniermanager.algorithmen.schweizer;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;

public class SchweizerSystemTest {

	SchweizerSystem schweizerSystem;

	@Test
	public void testErsteRundeOhneSetzPosUngerade() throws Exception {

		TeamMeldungen meldungen = new TeamMeldungen();

		for (int i = 1; i < 6; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde(meldungen.teams());

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(1).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(2).isFreilos()).isTrue(); // freilos

		// flatten list for validate (Freilos wird nicht mit aufgenommen)
		List<Team> teamList = new SchweizerSystem().flattenTeampaarungen(ersteRunde);

		assertThat(teamList.size()).isEqualTo(5); // ohne Freilos

		List<Team> expected = new ArrayList<>();
		for (int i = 1; i < 6; i++) {
			expected.add(Team.from(i));
		}
		assertThat(teamList).containsExactlyElementsOf(expected);
	}

	@Test
	public void testErsteRundeOhneSetzPosGerade() throws Exception {

		TeamMeldungen meldungen = new TeamMeldungen();

		for (int i = 1; i < 7; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde(meldungen.teams());

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(1).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(2).getB()).isNotNull(); // kein freilos

		// flatten list for validate
		List<Team> teamList = ersteRunde.stream()
				.flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB()))
				.sorted((t1, t2) -> Integer.compare(t1.getNr(), t2.getNr())).collect(Collectors.toList());

		assertThat(teamList.size()).isEqualTo(6);

		List<Team> expected = new ArrayList<>();
		for (int i = 1; i < 7; i++) {
			expected.add(Team.from(i));
		}
		assertThat(teamList).containsExactlyElementsOf(expected);
	}

	@Test
	public void testErsteRundeMitSetzPosGerade() throws Exception {

		TeamMeldungen meldungen = new TeamMeldungen();

		for (int i = 1; i < 4; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}
		for (int i = 4; i < 7; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i).setSetzPos(1));
		}

		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde(meldungen.teams());

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(0).getA().getSetzPos()).isEqualTo(0);
		assertThat(ersteRunde.get(0).getB().getSetzPos()).isEqualTo(1);
	}

	@Test
	public void testWeitereRundeGeradeAnzahl() throws Exception {

		// erste runde fest vorgeben
		TeamMeldungen testmeldungen = newTestmeldungen();
		schweizerSystem = new SchweizerSystem();

		// ---------------------------------------------------------------------------------------
		// Runde 2
		// ---------------------------------------------------------------------------------------
		List<TeamPaarung> resultRunde2 = schweizerSystem.weitereRunde(testmeldungen.teams(), List.of());
		assertThat(resultRunde2.size()).isEqualTo(4);

		// flatten list for validate
		List<Team> teamListresult = resultRunde2.stream()
				.flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB())).collect(Collectors.toList());
		assertThat(teamListresult.size()).isEqualTo(8);
		validateGegnerList(teamListresult, 2);

		List<Team> expected = new ArrayList<>();

		expected.add(Team.from(1));
		expected.add(Team.from(2));

		expected.add(Team.from(3));
		expected.add(Team.from(4));

		expected.add(Team.from(5));
		expected.add(Team.from(6));

		expected.add(Team.from(7));
		expected.add(Team.from(8));

		assertThat(teamListresult).containsExactlyElementsOf(expected);

		// ---------------------------------------------------------------------------------------
		// Runde 3
		// ---------------------------------------------------------------------------------------
		TeamMeldungen meldungen = new TeamMeldungen();
		// neue rangliste
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(4)); // team 5
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(5)); // team 6

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(0)); // team 1
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(7)); // team 8

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(1)); // team 2
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(6)); // team 7

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(3)); // team 4
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(2)); // team 3
		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> resultRunde3 = schweizerSystem.weitereRunde(meldungen.teams(), List.of());
		assertThat(resultRunde3.size()).isEqualTo(4);

		// flatten list for validate
		List<Team> teamListresultRunde3 = resultRunde3.stream()
				.flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB())).collect(Collectors.toList());
		assertThat(teamListresultRunde3.size()).isEqualTo(8);
		validateGegnerList(teamListresultRunde3, 3);

		assertThat(resultRunde3.get(0).getA()).isEqualTo(Team.from(5));
		assertThat(resultRunde3.get(0).getB()).isEqualTo(Team.from(1));

		assertThat(resultRunde3.get(1).getA()).isEqualTo(Team.from(6));
		assertThat(resultRunde3.get(1).getB()).isEqualTo(Team.from(2));

		assertThat(resultRunde3.get(2).getA()).isEqualTo(Team.from(8));
		assertThat(resultRunde3.get(2).getB()).isEqualTo(Team.from(3));

		assertThat(resultRunde3.get(3).getA()).isEqualTo(Team.from(7));
		assertThat(resultRunde3.get(3).getB()).isEqualTo(Team.from(4));

		// ---------------------------------------------------------------------------------------
		// Runde 4
		// ---------------------------------------------------------------------------------------
		meldungen = new TeamMeldungen();
		// neue rangliste
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(4)); // team 5
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(7)); // team 8

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(6)); // team 7
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(5)); // team 6

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(3)); // team 4
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(0)); // team 1

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(2)); // team 3
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(1)); // team 2
		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> resultRunde4 = schweizerSystem.weitereRunde(meldungen.teams(), List.of());
		assertThat(resultRunde4.size()).isEqualTo(4);

		// flatten list for validate
		List<Team> teamListresultRunde4 = resultRunde4.stream()
				.flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB())).collect(Collectors.toList());
		assertThat(teamListresultRunde4.size()).isEqualTo(8);
		validateGegnerList(teamListresultRunde4, 4);

		assertThat(resultRunde4.get(0).getA()).isEqualTo(Team.from(5));
		assertThat(resultRunde4.get(0).getB()).isEqualTo(Team.from(8));

		assertThat(resultRunde4.get(1).getA()).isEqualTo(Team.from(7));
		assertThat(resultRunde4.get(1).getB()).isEqualTo(Team.from(6));

		assertThat(resultRunde4.get(2).getA()).isEqualTo(Team.from(4));
		assertThat(resultRunde4.get(2).getB()).isEqualTo(Team.from(2));

		assertThat(resultRunde4.get(3).getA()).isEqualTo(Team.from(1));
		assertThat(resultRunde4.get(3).getB()).isEqualTo(Team.from(3));
	}

	/**
	 *
	 * @param teamListe
	 * @param anzGegner -1 = nicht prüfen
	 */

	private void validateGegnerList(List<Team> teamListe, int anzGegner) {

		for (Team team : teamListe) {
			if (anzGegner > -1) {
				assertThat(team.anzGegner()).as("Team nr %d ungueltige anzahl gegner", team.getNr())
						.isEqualTo(anzGegner);
			}

			for (Integer gegnerNr : team.getGegner()) {

				// in der liste suchen
				Team gegnerTeam = teamListe.stream().filter(tm -> tm.getNr() == gegnerNr).findFirst().orElse(null);
				assertThat(gegnerTeam).isNotNull();
				assertThat(gegnerTeam.hatAlsGegner(team)).isTrue();
			}
		}
	}

	@Test
	public void testErsteRundeMitSetzPosUnGerade() throws Exception {
		TeamMeldungen meldungen = new TeamMeldungen();

		for (int i = 1; i < 4; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}
		for (int i = 4; i < 8; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i).setSetzPos(1));
		}

		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde(meldungen.teams());

		assertThat(ersteRunde.size()).isEqualTo(4);
		assertThat(ersteRunde.get(0).getA().getSetzPos()).isEqualTo(0);
		assertThat(ersteRunde.get(0).getB().getSetzPos()).isEqualTo(1);
		assertThat(ersteRunde.get(3).isFreilos()).isTrue(); // freilos

	}

	@Test
	public void testWeitereRundeUngGeradeAnzahl() throws Exception {
		// erste runde fest vorgeben
		// runde 1
		TeamMeldungen testmeldungen9 = new9Testmeldungen();
		schweizerSystem = new SchweizerSystem();

		// Runde 2
		List<TeamPaarung> weitereRunde = schweizerSystem.weitereRunde(testmeldungen9.teams(), List.of());

		assertThat(weitereRunde.size()).isEqualTo(5); // 9 + freilos

		assertThat(weitereRunde.get(0).getA()).isEqualByComparingTo(Team.from(1));
		assertThat(weitereRunde.get(0).getB()).isEqualByComparingTo(Team.from(2));

		assertThat(weitereRunde.get(1).getA()).isEqualByComparingTo(Team.from(3));
		assertThat(weitereRunde.get(1).getB()).isEqualByComparingTo(Team.from(4));

		assertThat(weitereRunde.get(2).getA()).isEqualByComparingTo(Team.from(5));
		assertThat(weitereRunde.get(2).getB()).isEqualByComparingTo(Team.from(6));

		assertThat(weitereRunde.get(3).getA()).isEqualByComparingTo(Team.from(7));
		assertThat(weitereRunde.get(3).getB()).isEqualByComparingTo(Team.from(9));

		assertThat(weitereRunde.get(4).getA()).isEqualByComparingTo(Team.from(8));
		assertThat(weitereRunde.get(4).isFreilos()).isTrue();

		// flatten list for validate (Freilos wird nicht aufgenommen)
		List<Team> flattenTeampaarungen1 = schweizerSystem.flattenTeampaarungen(weitereRunde);
		assertThat(flattenTeampaarungen1.size()).isEqualTo(9); // ohne Freilos

		// ---------------------------------------------------------------------------------------
		// Runde 3
		// ---------------------------------------------------------------------------------------
		// neue rangliste
		TeamMeldungen meldungen = new TeamMeldungen();

		// neue rangliste
		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(1)); // team 2
		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(2)); // team 3

		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(5)); // team 6
		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(8)); // team 9

		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(6)); // team 7
		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(0)); // team 1

		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(3)); // team 4
		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(4)); // team 5

		meldungen.addTeamWennNichtVorhanden(flattenTeampaarungen1.get(7)); // team 8, hatte bereits freilos

		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> resultRunde3 = schweizerSystem.weitereRunde(meldungen.teams(), List.of()); // Runde 3
		assertThat(resultRunde3.size()).isEqualTo(5);

		assertThat(resultRunde3.get(0).getA()).isEqualByComparingTo(Team.from(2));
		assertThat(resultRunde3.get(0).getB()).isEqualByComparingTo(Team.from(3));

		assertThat(resultRunde3.get(1).getA()).isEqualByComparingTo(Team.from(6));
		assertThat(resultRunde3.get(1).getB()).isEqualByComparingTo(Team.from(9));

		assertThat(resultRunde3.get(2).getA()).isEqualByComparingTo(Team.from(7));
		assertThat(resultRunde3.get(2).getB()).isEqualByComparingTo(Team.from(1));

		assertThat(resultRunde3.get(3).getA()).isEqualByComparingTo(Team.from(4));
		assertThat(resultRunde3.get(3).getB()).isEqualByComparingTo(Team.from(8));

		// team 5 mit freilos
		assertThat(resultRunde3.get(4).getA()).isEqualByComparingTo(Team.from(5));
		assertThat(resultRunde3.get(4).isFreilos()).isTrue();
	}

	// eine runde mit 9 Teams + gegner
	private TeamMeldungen new9Testmeldungen() {
		TeamMeldungen meldungen = new TeamMeldungen();

		// erste runde fest vorgeben
		List<Team> testTeams = new ArrayList<>();
		for (int i = 1; i < 10; i++) {
			testTeams.add(Team.from(i));
		}

		testTeams.get(0).addGegner(testTeams.get(3)); // team 1-4
		testTeams.get(1).addGegner(testTeams.get(4)); // team 2-5
		testTeams.get(5).addGegner(testTeams.get(7)); // team 6-8
		testTeams.get(6).addGegner(testTeams.get(2)); // team 7-3
		testTeams.get(8).setHatteFreilos(true);

		meldungen.addTeamWennNichtVorhanden(testTeams);

		validateGegnerList(testTeams, -1);

		return meldungen;
	}

	// eine runde mit 8 Teams + gegner
	private TeamMeldungen newTestmeldungen() {
		TeamMeldungen meldungen = new TeamMeldungen();

		// erste runde fest vorgeben
		List<Team> testTeams = new ArrayList<>();
		for (int i = 1; i < 9; i++) {
			testTeams.add(Team.from(i));
		}

		testTeams.get(0).addGegner(testTeams.get(3)); // team 1-4
		testTeams.get(1).addGegner(testTeams.get(4)); // team 2-5
		testTeams.get(5).addGegner(testTeams.get(7)); // team 6-8
		testTeams.get(6).addGegner(testTeams.get(2)); // team 7-3

		meldungen.addTeamWennNichtVorhanden(testTeams);

		validateGegnerList(testTeams, 1);

		return meldungen;
	}

	/**
	 * Rechenbeispiel aus SchweizerTurnierSystem.md:
	 * 6 Teams, 3 Runden. Erwartet: A(1.), E(2.), C(3.) usw.
	 *
	 * Turnierverlauf:
	 * R1: A-B, C-D, E-F (alle Heimteams gewinnen)
	 * R2: A-C, E-B, D-F (alle Heimteams gewinnen)
	 * R3: A-E, C-F, B-D (alle Heimteams gewinnen)
	 */
	@Test
	public void testSortiereNachAuswertungskriterien() throws Exception {
		schweizerSystem = new SchweizerSystem();

		// Teamname -> teamNr: A=1, B=2, C=3, D=4, E=5, F=6
		// Siege: A=3, B=1, C=2, D=1, E=2, F=0
		// Gegner: A:[B,C,E], B:[A,E,D], C:[D,A,F], D:[C,F,B], E:[F,B,A], F:[E,D,C]
		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 3, 0, 0, List.of(2, 3, 5)), // A
				new SchweizerTeamErgebnis(2, 1, 0, 0, List.of(1, 5, 4)), // B
				new SchweizerTeamErgebnis(3, 2, 0, 0, List.of(4, 1, 6)), // C
				new SchweizerTeamErgebnis(4, 1, 0, 0, List.of(3, 6, 2)), // D
				new SchweizerTeamErgebnis(5, 2, 0, 0, List.of(6, 2, 1)), // E
				new SchweizerTeamErgebnis(6, 0, 0, 0, List.of(5, 4, 3))  // F
		);

		// BHZ prüfen: A=1+2+2=5, B=3+2+1=6, C=1+3+0=4, D=2+0+1=3, E=0+1+3=4, F=2+1+2=5
		Map<Integer, Integer> bhz = schweizerSystem.berechneBuchholz(ergebnisse);
		assertThat(bhz.get(1)).isEqualTo(5); // A
		assertThat(bhz.get(2)).isEqualTo(6); // B
		assertThat(bhz.get(3)).isEqualTo(4); // C
		assertThat(bhz.get(4)).isEqualTo(3); // D
		assertThat(bhz.get(5)).isEqualTo(4); // E
		assertThat(bhz.get(6)).isEqualTo(5); // F

		// FBHZ prüfen: C=BHZ(D)+BHZ(A)+BHZ(F)=3+5+5=13, E=BHZ(F)+BHZ(B)+BHZ(A)=5+6+5=16
		Map<Integer, Integer> fbhz = schweizerSystem.berechneFeinbuchholz(ergebnisse, bhz);
		assertThat(fbhz.get(3)).isEqualTo(13); // C
		assertThat(fbhz.get(5)).isEqualTo(16); // E

		// Sortierung prüfen: 1.A(3 Siege), 2.E(2S,4BHZ,16FBHZ), 3.C(2S,4BHZ,13FBHZ),
		// 4.B(1S,6BHZ), 5.D(1S,3BHZ), 6.F(0 Siege)
		List<SchweizerTeamErgebnis> sortiert = schweizerSystem.sortiereNachAuswertungskriterien(ergebnisse);
		assertThat(sortiert).hasSize(6);
		assertThat(sortiert.get(0).teamNr()).isEqualTo(1); // A - 1. Platz
		assertThat(sortiert.get(1).teamNr()).isEqualTo(5); // E - 2. Platz (FBHZ 16 > 13)
		assertThat(sortiert.get(2).teamNr()).isEqualTo(3); // C - 3. Platz
		assertThat(sortiert.get(3).teamNr()).isEqualTo(2); // B - 4. Platz (1 Sieg, BHZ 6)
		assertThat(sortiert.get(4).teamNr()).isEqualTo(4); // D - 5. Platz (1 Sieg, BHZ 3)
		assertThat(sortiert.get(5).teamNr()).isEqualTo(6); // F - 6. Platz
	}

	@Test
	public void testSortiereNachAuswertungskriterienPunktedifferenz() throws Exception {
		schweizerSystem = new SchweizerSystem();

		// Zwei Teams mit gleichen Siegen, BHZ, FBHZ -> Punktedifferenz entscheidet
		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 2, 5, 0, List.of(3, 4)),   // Team 1: +5 Punktediff
				new SchweizerTeamErgebnis(2, 2, 3, 0, List.of(3, 4)),   // Team 2: +3 Punktediff
				new SchweizerTeamErgebnis(3, 1, 0, 0, List.of(1, 2)),   // Team 3
				new SchweizerTeamErgebnis(4, 1, 0, 0, List.of(1, 2))    // Team 4
		);

		List<SchweizerTeamErgebnis> sortiert = schweizerSystem.sortiereNachAuswertungskriterien(ergebnisse);
		assertThat(sortiert.get(0).teamNr()).isEqualTo(1); // höhere Punktedifferenz gewinnt
		assertThat(sortiert.get(1).teamNr()).isEqualTo(2);
	}

	/**
	 * Mit Buchholz: Siege, BHZ, FBHZ und Punktedifferenz gleich -> Punkte+ entscheidet.
	 */
	@Test
	public void testSortiereNachAuswertungskriterienPunktePlusEntscheidet() throws Exception {
		schweizerSystem = new SchweizerSystem();

		// Team 1 und Team 2 haben identische Siege/Gegnerliste (=> gleiches BHZ/FBHZ) und
		// gleiche Punktedifferenz -> nur Punkte+ unterscheidet die beiden.
		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 2, 5, 30, List.of(3, 4)), // Team 1: Punkte+ = 30
				new SchweizerTeamErgebnis(2, 2, 5, 25, List.of(3, 4)), // Team 2: Punkte+ = 25
				new SchweizerTeamErgebnis(3, 1, 0, 0, List.of(1, 2)),
				new SchweizerTeamErgebnis(4, 1, 0, 0, List.of(1, 2))
		);

		List<SchweizerTeamErgebnis> sortiert = schweizerSystem.sortiereNachAuswertungskriterien(ergebnisse);
		assertThat(sortiert.get(0).teamNr()).isEqualTo(1); // höheres Punkte+ gewinnt
		assertThat(sortiert.get(1).teamNr()).isEqualTo(2);
	}

	/**
	 * MIT_BUCHHOLZ delegiert an die 1-Parameter-Überladung (Siege → BHZ → FBHZ).
	 */
	@Test
	public void testSortiereMitBuchholz_delegiertAnEinParameterUeberladung() {
		schweizerSystem = new SchweizerSystem();

		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 2, 5, 30, List.of(3, 4)),
				new SchweizerTeamErgebnis(2, 2, 5, 25, List.of(3, 4)),
				new SchweizerTeamErgebnis(3, 1, 0, 0, List.of(1, 2)),
				new SchweizerTeamErgebnis(4, 1, 0, 0, List.of(1, 2))
		);

		List<SchweizerTeamErgebnis> erwartet = schweizerSystem.sortiereNachAuswertungskriterien(ergebnisse);
		List<SchweizerTeamErgebnis> tatsaechlich = schweizerSystem.sortiereNachAuswertungskriterien(ergebnisse,
				SchweizerRankingModus.MIT_BUCHHOLZ);

		assertThat(tatsaechlich).containsExactlyElementsOf(erwartet);
	}

	/**
	 * Ohne Buchholz: Siege → Punktediff → Punkte+ (kein BHZ/FBHZ).
	 * Zwei Teams gleiche Siege, gleiche Punktediff → Punkte+ entscheidet (kein BHZ-Einfluss).
	 */
	@Test
	public void testSortiereOhneBuchholz_PunktediffEntscheidet() {
		schweizerSystem = new SchweizerSystem();

		// Team 1: 2 Siege, Diff=5, Punkte+=30 | Team 2: 2 Siege, Diff=5, Punkte+=25
		// Team 3: 2 Siege, Diff=3, Punkte+=20 | Team 4: 1 Sieg
		// Ohne BHZ → Team 1 vor Team 2 wegen Punkte+, Team 2 vor Team 3 wegen Diff
		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 2, 5, 30, List.of(3, 4)),
				new SchweizerTeamErgebnis(2, 2, 5, 25, List.of(3, 4)),
				new SchweizerTeamErgebnis(3, 2, 3, 20, List.of(1, 2)),
				new SchweizerTeamErgebnis(4, 1, 0, 10, List.of(1, 2))
		);

		List<SchweizerTeamErgebnis> sortiert = schweizerSystem.sortiereNachAuswertungskriterien(ergebnisse,
				SchweizerRankingModus.OHNE_BUCHHOLZ);

		assertThat(sortiert).hasSize(4);
		assertThat(sortiert.get(0).teamNr()).isEqualTo(1); // 2 Siege, Diff=5, Punkte+=30
		assertThat(sortiert.get(1).teamNr()).isEqualTo(2); // 2 Siege, Diff=5, Punkte+=25
		assertThat(sortiert.get(2).teamNr()).isEqualTo(3); // 2 Siege, Diff=3
		assertThat(sortiert.get(3).teamNr()).isEqualTo(4); // 1 Sieg
	}

	/**
	 * Score Group Pairing: Teams mit gleichen Siegen werden innerhalb ihrer Gruppe gepaart.
	 * 8 Teams, 4 haben je 2 Siege, 4 haben je 1 Sieg → 2 Gruppen à 4 Teams.
	 * Erwartet: Paarungen nur innerhalb der Gruppen (keine 2-Siege vs. 1-Sieg Paarung).
	 */
	@Test
	public void testWeitereRundeSiegeGruppen_PaarungNurInnerhalb() {
		// Teams 1-4 haben je 2 Siege, Teams 5-8 haben je 1 Sieg
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i <= 8; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 2, 10, 0, List.of(5, 6)),
				new SchweizerTeamErgebnis(2, 2, 8, 0, List.of(7, 8)),
				new SchweizerTeamErgebnis(3, 2, 6, 0, List.of(5, 7)),
				new SchweizerTeamErgebnis(4, 2, 4, 0, List.of(6, 8)),
				new SchweizerTeamErgebnis(5, 1, -4, 0, List.of(1, 3)),
				new SchweizerTeamErgebnis(6, 1, -6, 0, List.of(4, 1)),
				new SchweizerTeamErgebnis(7, 1, -8, 0, List.of(2, 3)),
				new SchweizerTeamErgebnis(8, 1, -10, 0, List.of(4, 2)));

		// Teams bereits mit Gegner-Geschichte
		Team t1 = meldungen.getTeam(1);
		Team t5 = meldungen.getTeam(5);
		Team t6 = meldungen.getTeam(6);
		t1.addGegner(t5);
		t1.addGegner(t6);
		meldungen.getTeam(2).addGegner(meldungen.getTeam(7));
		meldungen.getTeam(2).addGegner(meldungen.getTeam(8));
		meldungen.getTeam(3).addGegner(meldungen.getTeam(5));
		meldungen.getTeam(3).addGegner(meldungen.getTeam(7));
		meldungen.getTeam(4).addGegner(meldungen.getTeam(6));
		meldungen.getTeam(4).addGegner(meldungen.getTeam(8));

		schweizerSystem = new SchweizerSystem();
		// Teams müssen in Ranglistenreihenfolge übergeben werden (Siege absteigend)
		List<Team> sortiertTeams = List.of(
				meldungen.getTeam(1), meldungen.getTeam(2), meldungen.getTeam(3), meldungen.getTeam(4),
				meldungen.getTeam(5), meldungen.getTeam(6), meldungen.getTeam(7), meldungen.getTeam(8));

		List<TeamPaarung> paarungen = schweizerSystem.weitereRunde(sortiertTeams, ergebnisse);

		assertThat(paarungen).hasSize(4);

		// Alle Paarungen müssen innerhalb der Sieggruppe sein:
		// Gruppe A (2 Siege): Teams 1-4, Gruppe B (1 Sieg): Teams 5-8
		for (TeamPaarung paarung : paarungen) {
			assertThat(paarung.isFreilos()).isFalse();
			int siegeA = ergebnisse.stream().filter(e -> e.teamNr() == paarung.getA().getNr())
					.mapToInt(SchweizerTeamErgebnis::siege).findFirst().orElse(-1);
			int siegeB = ergebnisse.stream().filter(e -> e.teamNr() == paarung.getB().getNr())
					.mapToInt(SchweizerTeamErgebnis::siege).findFirst().orElse(-1);
			assertThat(siegeA).as("Paarung A (%d) und B (%d) müssen gleiche Siegzahl haben",
					paarung.getA().getNr(), paarung.getB().getNr()).isEqualTo(siegeB);
		}
	}

	/**
	 * Score Group Pairing: Ungerade Gruppe → Float-Team wird in nächste Gruppe verschoben.
	 * 6 Teams: 3 mit 2 Siegen (ungerade Gruppe) + 3 mit 1 Sieg.
	 * Erwartet: 1 Paarung mit Float (2 Siege vs. 1 Sieg), 2 reine Gruppen-Paarungen.
	 */
	@Test
	public void testBaueSiegeGruppen_FloatBeiUngeraderGruppe() {
		schweizerSystem = new SchweizerSystem();

		List<Team> teams = new ArrayList<>();
		for (int i = 1; i <= 6; i++) {
			teams.add(Team.from(i));
		}

		// Teams 1-3 haben 2 Siege (ungerade Gruppe), Teams 4-6 haben 1 Sieg
		Map<Integer, Integer> siegeMap = Map.of(1, 2, 2, 2, 3, 2, 4, 1, 5, 1, 6, 1);

		List<List<Team>> gruppen = schweizerSystem.baueSiegeGruppen(teams, siegeMap);

		// Nach Float: Gruppe 1 (2 Siege) hat 2 Teams, Gruppe 2 (1 Sieg + Float) hat 4 Teams
		assertThat(gruppen).hasSize(2);
		assertThat(gruppen.get(0)).hasSize(2); // Teams 1+2 (T3 floated)
		assertThat(gruppen.get(1)).hasSize(4); // T3 (Float) + Teams 4,5,6
		// Float-Team (T3) muss an erster Stelle der zweiten Gruppe stehen
		assertThat(gruppen.get(1).getFirst()).isEqualByComparingTo(Team.from(3));
	}

	/**
	 * Unvermeidbare Wiederholung: Team 1 hat bereits gegen alle anderen Teams gespielt (2, 3, 4). Erst wenn keine
	 * wiederholungsfreie Paarung existiert, lässt die Suche eine Wiederholung zu – genau eine: 1–2, die übrigen
	 * Teams 3 und 4 werden regulär neu gepaart.
	 */
	@Test
	public void testWeitereRundeDeadlock_erzwingtRematchWennKeinGegnerFrei() {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i <= 4; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		Team team1 = meldungen.getTeam(1);
		Team team2 = meldungen.getTeam(2);
		Team team3 = meldungen.getTeam(3);
		Team team4 = meldungen.getTeam(4);

		// Team 1 hat bereits gegen alle anderen Teams der Gruppe gespielt
		team1.addGegner(team2);
		team1.addGegner(team3);
		team1.addGegner(team4);

		// alle 4 Teams mit gleicher Siegzahl -> eine einzige Sieggruppe
		List<SchweizerTeamErgebnis> ergebnisse = List.of(
				new SchweizerTeamErgebnis(1, 1, 0, 0, List.of(2, 3, 4)),
				new SchweizerTeamErgebnis(2, 1, 0, 0, List.of(1)),
				new SchweizerTeamErgebnis(3, 1, 0, 0, List.of(1)),
				new SchweizerTeamErgebnis(4, 1, 0, 0, List.of(1)));

		schweizerSystem = new SchweizerSystem();
		List<TeamPaarung> paarungen = schweizerSystem.weitereRunde(
				List.of(team1, team2, team3, team4), ergebnisse);

		assertThat(paarungen).hasSize(2);

		TeamPaarung erzwungenesRematch = paarungen.get(0);
		assertThat(erzwungenesRematch.getA()).isEqualByComparingTo(team1);
		assertThat(erzwungenesRematch.getB()).isEqualByComparingTo(team2);
		// Team 1 und Team 2 waren bereits vor dieser Runde Gegner -> echtes Rematch
		assertThat(team1.hatAlsGegner(team2)).isTrue();

		TeamPaarung neuePaarung = paarungen.get(1);
		assertThat(neuePaarung.getA()).isEqualByComparingTo(team3);
		assertThat(neuePaarung.getB()).isEqualByComparingTo(team4);
	}

	/**
	 * Regression (test-schweizer.ods, 6 Teams, Runde 4): Der Tabellenführer (3 Siege) bildet mit dem Zweiten
	 * (2 Siege, schon Gegner in Runde 2) die oberste Gruppe. Die gruppenweise Paarung fand keinen anderen Gegner,
	 * der Tausch scheiterte mangels bereits gebildeter Paarungen – es entstand eine vermeidbare Gegnerwiederholung.
	 */
	@Test
	public void testKeineVermeidbareWiederholungWennObersteGruppeFestsitzt() {
		// Gegner der Runden 1–3: 3–2, 5–4, 6–1 | 5–3, 1–2, 6–4 | 4–2, 3–1, 6–5
		Map<Integer, List<Integer>> gegner = Map.of(1, List.of(6, 2, 3), 2, List.of(3, 1, 4), 3, List.of(2, 5, 1),
				4, List.of(5, 6, 2), 5, List.of(4, 3, 6), 6, List.of(1, 4, 5));
		Map<Integer, Integer> siege = Map.of(5, 3, 3, 2, 6, 1, 4, 1, 2, 1, 1, 1);
		List<Integer> rangfolge = List.of(5, 3, 6, 4, 2, 1);

		Map<Integer, Team> teams = rangfolge.stream().collect(Collectors.toMap(nr -> nr, Team::from));
		gegner.forEach((nr, gegnerNrn) -> gegnerNrn.forEach(g -> teams.get(nr).addGegner(teams.get(g))));
		List<SchweizerTeamErgebnis> ergebnisse = rangfolge.stream()
				.map(nr -> new SchweizerTeamErgebnis(nr, siege.get(nr), 0, 0, gegner.get(nr))).toList();

		List<TeamPaarung> runde4 = new SchweizerSystem().weitereRunde(rangfolge.stream().map(teams::get).toList(),
				ergebnisse);

		assertThat(runde4).hasSize(3);
		assertThat(runde4).allSatisfy(paarung -> assertThat(gegner.get(paarung.getA().getNr()))
				.as("Wiederholung %d–%d", paarung.getA().getNr(), paarung.getB().getNr())
				.doesNotContain(paarung.getB().getNr()));
		assertThat(new SchweizerSystem().flattenTeampaarungen(runde4)).extracting(Team::getNr)
				.containsExactly(1, 2, 3, 4, 5, 6);
		// Erwartet: 5 bekommt den ersten freien Gegner in Ranglistenreihenfolge (2), danach 3–6 und 4–1
		assertThat(runde4).extracting(paarung -> paarung.getA().getNr() + "–" + paarung.getB().getNr())
				.containsExactly("5–2", "3–6", "4–1");
	}

	/**
	 * Regression (Online-Review, 32 Teams, Runde 16): Das frühere Backtracking mit Schrittbudget wertete ein
	 * erschöpftes Budget als „unlösbar“ und lieferte acht Gegnerwiederholungen, obwohl eine Paarung ohne
	 * Wiederholung existiert.
	 */
	@Test
	public void testSpaeteRundeGrossesFeldOhneVermeidbareWiederholung() {
		// Gegner der Runden 1–15 und Rangfolge/Siege nach Runde 15
		String gespielt = "1-32 2-31 3-30 4-29 5-28 6-27 7-26 8-25 9-24 10-23 11-22 12-21 13-20 14-19 15-18 16-17 27-4 5-17 "
				+ "8-18 26-13 32-23 2-9 12-19 22-30 3-11 14-21 24-31 10-1 20-7 15-25 16-28 29-6 8-26 5-30 2-4 32-12 "
				+ "19-27 3-31 17-1 23-7 29-28 18-13 25-9 22-14 21-15 24-10 11-20 16-6 30-12 8-4 26-19 5-32 2-3 14-25 "
				+ "13-7 28-1 27-22 29-21 31-18 9-23 17-11 6-24 20-15 10-16 12-4 8-30 26-32 13-3 28-14 19-5 2-29 7-27 "
				+ "1-18 25-23 17-24 22-21 31-9 11-6 20-16 10-15 4-30 12-3 32-14 8-19 26-2 13-27 28-18 23-17 5-22 7-1 "
				+ "29-31 11-25 20-24 21-9 6-10 16-15 4-3 14-12 30-19 13-32 2-18 17-8 26-5 27-29 7-28 23-20 25-22 31-1 "
				+ "11-21 24-16 6-15 9-10 4-14 3-19 13-2 17-12 30-32 5-18 28-25 29-20 8-27 26-11 7-24 1-23 22-15 31-6 "
				+ "21-10 16-9 4-19 3-14 13-12 32-2 5-29 28-17 30-26 27-18 7-25 1-20 8-11 23-21 6-9 15-24 22-10 31-16 "
				+ "4-13 3-32 14-29 19-28 12-2 30-18 5-1 17-25 27-26 7-11 20-21 6-8 24-22 31-10 23-16 15-9 4-28 3-5 "
				+ "13-14 19-32 30-2 29-17 12-27 18-7 1-25 20-26 8-24 11-10 22-9 21-16 6-23 31-15 4-32 3-28 14-30 13-19 "
				+ "29-12 2-5 7-17 1-26 27-25 18-6 8-31 11-9 20-10 22-16 21-24 23-15 4-17 3-29 13-28 14-2 30-7 19-1 12-5 "
				+ "32-25 26-18 11-16 20-9 27-15 8-10 31-22 6-21 24-23 4-1 3-17 14-11 13-30 28-12 29-19 32-7 2-8 5-9 "
				+ "26-15 18-24 25-16 31-21 20-6 27-10 23-22 4-18 3-1 14-17 13-29 28-30 12-8 19-2 32-20 11-15 5-21 26-9 "
				+ "7-16 27-24 25-10 31-23 6-22";
		List<Integer> rangfolge = List.of(4, 3, 28, 14, 13, 19, 12, 30, 29, 17, 1, 32, 11, 2, 5, 18, 26, 8, 27, 20, 7, 25, 21, 31, 22, 16, 23, 6, 9, 15, 24, 10);
		List<Integer> siege = List.of(12, 10, 10, 10, 10, 9, 9, 9, 9, 9, 9, 8, 8, 7, 7, 7, 7, 7, 7, 7, 6, 6, 6, 6, 6, 6, 6, 6, 6, 6, 5, 4);

		Map<Integer, Team> teams = rangfolge.stream().collect(Collectors.toMap(nr -> nr, Team::from));
		Map<Integer, List<Integer>> gegner = new java.util.HashMap<>();
		for (String paar : gespielt.split(" ")) {
			int a = Integer.parseInt(paar.split("-")[0]);
			int b = Integer.parseInt(paar.split("-")[1]);
			teams.get(a).addGegner(teams.get(b));
			gegner.computeIfAbsent(a, k -> new ArrayList<>()).add(b);
			gegner.computeIfAbsent(b, k -> new ArrayList<>()).add(a);
		}
		List<SchweizerTeamErgebnis> ergebnisse = IntStream.range(0, rangfolge.size())
				.mapToObj(i -> new SchweizerTeamErgebnis(rangfolge.get(i), siege.get(i), 0, 0,
						gegner.get(rangfolge.get(i))))
				.toList();

		List<TeamPaarung> runde16 = new SchweizerSystem()
				.weitereRunde(rangfolge.stream().map(teams::get).toList(), ergebnisse);

		assertThat(runde16).hasSize(16);
		assertThat(new SchweizerSystem().flattenTeampaarungen(runde16)).hasSize(32);
		assertThat(runde16).allSatisfy(paarung -> assertThat(gegner.get(paarung.getA().getNr()))
				.as("Wiederholung %d–%d", paarung.getA().getNr(), paarung.getB().getNr())
				.doesNotContain(paarung.getB().getNr()));
	}

}
