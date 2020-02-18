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
		List<Team> teamList = ersteRunde.stream().flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB()))
				.sorted((t1, t2) -> Integer.compare((t1 != null) ? t1.getNr() : 9999, (t2 != null) ? t2.getNr() : 9999)).collect(Collectors.toList());

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
		List<Team> teamList = ersteRunde.stream().flatMap(teamPaarung -> Stream.of(teamPaarung.getA(), teamPaarung.getB()))
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
			meldungen.addTeamWennNichtVorhanden(Team.from(i).setSetzpos(1));
		}

		schweizerSystem = new SchweizerSystem(meldungen);
		List<TeamPaarung> ersteRunde = schweizerSystem.ersteRunde();

		assertThat(ersteRunde.size()).isEqualTo(3);
		assertThat(ersteRunde.get(0).getB()).isNotNull(); // kein freilos
		assertThat(ersteRunde.get(0).getA().getSetzpos()).isEqualTo(0);
		assertThat(ersteRunde.get(0).getB().getSetzpos()).isEqualTo(1);
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
}
