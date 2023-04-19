package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.mockito.Mockito;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;

public class SchweizerSystemTest {

	SchweizerSystem schweizerSystem;
	TeamMeldungen meldungenMock;

	@Test
	public void testErsteRundeOhneSetzPosUngerade() throws Exception {

		TeamMeldungen meldungen = new TeamMeldungen();

		for (int i = 1; i < 6; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde();

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(1).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(2).getB()).isNull(); // freilos

		// flatten list for validate
		List<Team> teamList = ersteRunde.stream()
				.flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB()))
				.sorted((t1, t2) -> Integer.compare((t1 != null) ? t1.getNr() : 9999, (t2 != null) ? t2.getNr() : 9999))
				.collect(Collectors.toList());

		assertThat(teamList.size()).isEqualTo(6); // mit null (freilos)

		List<Team> expected = new ArrayList<>();
		for (int i = 1; i < 6; i++) {
			expected.add(Team.from(i));
		}
		expected.add(null);
		assertThat(teamList).containsExactlyElementsOf(expected);
	}

	@Test
	public void testErsteRundeOhneSetzPosGerade() throws Exception {

		TeamMeldungen meldungen = new TeamMeldungen();

		for (int i = 1; i < 7; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}

		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde();

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

		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde();

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(0).getA().getSetzPos()).isEqualTo(0);
		assertThat(ersteRunde.get(0).getB().getSetzPos()).isEqualTo(1);
	}

	@Test
	public void testFindeGegner() throws Exception {
		meldungenMock = Mockito.mock(TeamMeldungen.class);
		schweizerSystem = new SchweizerSystem(meldungenMock);

		List<Team> restTeams = new ArrayList<>();

		for (int i = 1; i < 6; i++) {
			restTeams.add(Team.from(i));
		}
		restTeams.get(0).addGegner(Team.from(3));
		Team result = schweizerSystem.findeGegner(Team.from(3), restTeams);
		assertThat(result).isNotNull();
		assertThat(result.getNr()).isEqualTo(2);
	}

	@Test
	public void testWeitereRundeGeradeAnzahl() throws Exception {

		TeamMeldungen meldungen = new TeamMeldungen();

		// erste runde fest vorgeben
		schweizerSystem = new SchweizerSystem(newTestmeldungen());

		// ---------------------------------------------------------------------------------------
		// Runde 2
		// ---------------------------------------------------------------------------------------
		List<TeamPaarung> resultRunde2 = schweizerSystem.weitereRunde();
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
		meldungen = new TeamMeldungen();
		// neue rangliste
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(4)); // team 5
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(5)); // team 6

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(0)); // team 1
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(7)); // team 8

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(1)); // team 2
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(6)); // team 7

		meldungen.addTeamWennNichtVorhanden(teamListresult.get(3)); // team 4
		meldungen.addTeamWennNichtVorhanden(teamListresult.get(2)); // team 3
		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> resultRunde3 = schweizerSystem.weitereRunde();
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
		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> resultRunde4 = schweizerSystem.weitereRunde();
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
	public void testFindGegnerAusTeamPaarungen() throws Exception {
		meldungenMock = Mockito.mock(TeamMeldungen.class);
		schweizerSystem = new SchweizerSystem(meldungenMock);

		List<TeamPaarung> paarungen = new ArrayList<>();

		paarungen.add(new TeamPaarung(1, 2).addGegner());
		paarungen.add(new TeamPaarung(6, 8).addGegner());
		paarungen.add(new TeamPaarung(9).addGegner());
		paarungen.add(new TeamPaarung(10, 11).addGegner());

		Team result = schweizerSystem.findGegnerAusTeamPaarungen(Team.from(8), paarungen);
		assertThat(result).isNotNull().isEqualTo(Team.from(6));

		Team result2 = schweizerSystem.findGegnerAusTeamPaarungen(Team.from(9), paarungen);
		assertThat(result2).isNull();

		Team result3 = schweizerSystem.findGegnerAusTeamPaarungen(Team.from(23), paarungen);
		assertThat(result3).isNull();
	}

	@Test
	public void testKannTauschenMit() throws Exception {

		TeamMeldungen testmeldungen = newTestmeldungen();

		schweizerSystem = new SchweizerSystem(testmeldungen);

		// neue Team Paarungen 2 Runde fest vorgeben zum testen
		List<TeamPaarung> paarungen = new ArrayList<>();

		paarungen.add(new TeamPaarung(testmeldungen.getTeam(1), testmeldungen.getTeam(2)).addGegner().setHatGegner());
		paarungen.add(new TeamPaarung(testmeldungen.getTeam(6), testmeldungen.getTeam(4)).addGegner().setHatGegner());
		paarungen.add(new TeamPaarung(testmeldungen.getTeam(5), testmeldungen.getTeam(8)).addGegner().setHatGegner());

		// letzt paarung wäre 3:7 haben aber bereits gegeneinander gespielt !
		// geht nicht, also suchen paarung zum tauschen für 3 und 7
		TeamPaarung invalidTeamP = new TeamPaarung(testmeldungen.getTeam(3), testmeldungen.getTeam(7));
		TeamPaarung kannTauschenMit = schweizerSystem.kannTauschenMit(invalidTeamP, paarungen);
		assertThat(kannTauschenMit).isNotNull();
		assertThat(kannTauschenMit.getA()).isNotNull().isEqualTo(Team.from(5));
		assertThat(kannTauschenMit.getB()).isNotNull().isEqualTo(Team.from(8));
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

		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde();

		assertThat(ersteRunde.size()).isEqualTo(4);
		assertThat(ersteRunde.get(0).getA().getSetzPos()).isEqualTo(0);
		assertThat(ersteRunde.get(0).getB().getSetzPos()).isEqualTo(1);
		assertThat(ersteRunde.get(3).getB()).isNull(); // freilos

	}

	@Test
	public void testWeitereRundeUngGeradeAnzahl() throws Exception {
		// erste runde fest vorgeben
		// runde 1
		schweizerSystem = new SchweizerSystem(new9Testmeldungen());

		// Runde 2
		List<TeamPaarung> weitereRunde = schweizerSystem.weitereRunde();

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
		assertThat(weitereRunde.get(4).getB()).isNull();

		// flatten list for validate
		List<Team> flattenTeampaarungen1 = schweizerSystem.flattenTeampaarungen(weitereRunde);
		assertThat(flattenTeampaarungen1.size()).isEqualTo(10); // 9 + freilos

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

		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> resultRunde3 = schweizerSystem.weitereRunde(); // Runde 3
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
		assertThat(resultRunde3.get(4).getB()).isNull();
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

	@Test
	public void testTauschenTeamsInPaarung() throws Exception {
		meldungenMock = Mockito.mock(TeamMeldungen.class);
		schweizerSystem = new SchweizerSystem(meldungenMock);

		TeamPaarung paarA = new TeamPaarung(Team.from(2), Team.from(3));
		TeamPaarung paarB = new TeamPaarung(Team.from(7), Team.from(8));

		// A1:B2 <-> A2:B1
		boolean result = schweizerSystem.tauschenTeamsInPaarung(paarA, paarB);
		assertThat(result).isTrue();
		assertThat(paarA.getA()).isEqualTo(Team.from(2));
		assertThat(paarA.getB()).isEqualTo(Team.from(8));
		assertThat(paarB.getA()).isEqualTo(Team.from(7));
		assertThat(paarB.getB()).isEqualTo(Team.from(3));
	}

	@Test
	public void testTauschenTeamsInPaarungHatGegner() throws Exception {
		meldungenMock = Mockito.mock(TeamMeldungen.class);
		schweizerSystem = new SchweizerSystem(meldungenMock);

		Team teamAA = Team.from(2);
		Team teamBB = Team.from(8);

		teamAA.addGegner(teamBB);

		TeamPaarung paarA = new TeamPaarung(teamAA, Team.from(3));
		TeamPaarung paarB = new TeamPaarung(Team.from(7), teamBB);

		// A1:A2 <-> B1:B2
		boolean result = schweizerSystem.tauschenTeamsInPaarung(paarA, paarB);
		assertThat(result).isTrue();
		assertThat(paarA.getA()).isEqualTo(Team.from(2));
		assertThat(paarA.getB()).isEqualTo(Team.from(7));
		assertThat(paarB.getA()).isEqualTo(Team.from(3));
		assertThat(paarB.getB()).isEqualTo(Team.from(8));
	}

}
