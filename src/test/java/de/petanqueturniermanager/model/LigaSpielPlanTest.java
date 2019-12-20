package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

public class LigaSpielPlanTest {

	@Test
	public void testFlipTeamsMitGerade() throws Exception {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i < 7; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}
		LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(meldungen);

		// clone erstellen ! zum prüfen
		// List<TeamPaarung> tpListFlatOrg = ligaSpielPlan.getSpielPlan().stream().flatMap(Collection::stream).map(tp -> {
		// return new TeamPaarung(tp.getA(), tp.getB());
		// }).collect(Collectors.toList());

		List<TeamPaarung> tpListFlatExpected = ligaSpielPlan.getSpielPlan().stream().flatMap(Collection::stream).map(tp -> {
			return new TeamPaarung(tp.getB(), tp.getA()); // Teams tauschen
		}).collect(Collectors.toList());

		ligaSpielPlan.flipTeams();

		List<TeamPaarung> tpListFlat = ligaSpielPlan.getSpielPlan().stream().flatMap(Collection::stream).collect(Collectors.toList());
		assertThat(tpListFlat).containsAll(tpListFlatExpected);
	}

	@Test
	public void testFlipTeamsMitUnGerade() throws Exception {
		TeamMeldungen meldungen = new TeamMeldungen();
		for (int i = 1; i < 8; i++) {
			meldungen.addTeamWennNichtVorhanden(Team.from(i));
		}
		LigaSpielPlan ligaSpielPlan = new LigaSpielPlan(meldungen);

		// clone erstellen ! zum prüfen
		// List<TeamPaarung> tpListFlatOrg = ligaSpielPlan.getSpielPlan().stream().flatMap(Collection::stream).map(tp -> {
		// return new TeamPaarung(tp.getA(), tp.getB());
		// }).collect(Collectors.toList());

		List<TeamPaarung> tpListFlatExpected = ligaSpielPlan.getSpielPlan().stream().flatMap(Collection::stream).map(tp -> {
			if (tp.getOptionalB().isPresent()) {
				return new TeamPaarung(tp.getB(), tp.getA()); // Teams tauschen
			}
			return new TeamPaarung(tp.getA(), tp.getB()); // Teams Nicht ! Tauschen
		}).collect(Collectors.toList());

		ligaSpielPlan.flipTeams();

		List<TeamPaarung> tpListFlat = ligaSpielPlan.getSpielPlan().stream().flatMap(Collection::stream).collect(Collectors.toList());
		assertThat(tpListFlat).containsAll(tpListFlatExpected);
	}

}
