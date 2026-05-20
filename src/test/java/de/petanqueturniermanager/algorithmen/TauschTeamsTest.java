package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;

public class TauschTeamsTest {

	@Test
	public void testKonstruktorUndAccessor() {
		Team teamA = Team.from(1);
		TeamPaarung paarung = new TeamPaarung(Team.from(3), Team.from(5));

		TauschTeams tausch = new TauschTeams(teamA, paarung);

		assertThat(tausch.teamAusRangliste()).isSameAs(teamA);
		assertThat(tausch.teamPaarungTausch()).isSameAs(paarung);
	}

	@Test
	public void testEqualsUndHashCode_gleicheReferenzen_sindEqual() {
		Team teamA = Team.from(2);
		TeamPaarung paarung = new TeamPaarung(Team.from(7), Team.from(9));

		TauschTeams a = new TauschTeams(teamA, paarung);
		TauschTeams b = new TauschTeams(teamA, paarung);

		assertThat(a).isEqualTo(b);
		assertThat(a.hashCode()).isEqualTo(b.hashCode());
	}

	@Test
	public void testEquals_unterschiedlicheTeams_nichtEqual() {
		TeamPaarung paarung = new TeamPaarung(Team.from(7), Team.from(9));

		TauschTeams a = new TauschTeams(Team.from(1), paarung);
		TauschTeams b = new TauschTeams(Team.from(2), paarung);

		assertThat(a).isNotEqualTo(b);
	}
}
