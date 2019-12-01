/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * @author Michael Massee
 *
 */
public class FormeSpielrunde extends NrComparable {

	final private ArrayList<TeamPaarung> teamPaarungen = new ArrayList<>();

	/**
	 * @param nr
	 */
	public FormeSpielrunde(int nr) {
		super(nr);
	}

	public FormeSpielrunde addPaarungWennNichtVorhanden(final TeamPaarung teamPaarung) {
		checkNotNull(teamPaarung);
		// Team teamA_AusListe = teamList.stream().filter(team -> teamA.equals(team)).findFirst().orElse(teamA);
		TeamPaarung isInList = teamPaarungen.stream().filter(teamPaarungausList -> teamPaarungausList.equals(teamPaarung)).findFirst().orElse(null);
		if (null == isInList) {
			teamPaarungen.add(teamPaarung);
		}
		return this;
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		for (TeamPaarung teamPaarung : teamPaarungen) {
			if (teamsStr.length() > 1) {
				teamsStr += ",";
			}
			teamsStr += teamPaarung.toString();
		}
		teamsStr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Nr", nr)
				.add("Teams", teamsStr)
				.toString();
		// @formatter:on

	}

	public ImmutableList<TeamPaarung> getTeamPaarungen() {
		return ImmutableList.<TeamPaarung>builder().addAll(teamPaarungen).build();
	}

	/**
	 * @return
	 */
	public int size() {
		return teamPaarungen.size();
	}

	/**
	 * suche in der liste entweder nach team A ode B
	 *
	 * @param team
	 * @return
	 */
	public TeamPaarung findTeamPaarung(final Team team) {
		// @formatter:off
		return teamPaarungen.stream()
				.filter(teamPaarungausList -> teamPaarungausList.getA().equals(team) || teamPaarungausList.getB().equals(team))
				.findFirst()
				.orElse(null);
		// @formatter:on
	}

	public ImmutableList<Team> getATeams() {
		List<Team> aTeams = teamPaarungen.stream().map(TeamPaarung::getA).collect(Collectors.toList());
		return ImmutableList.<Team>builder().addAll(aTeams).build();
	}

	public ImmutableList<Team> getBTeams() {
		List<Team> bTeams = teamPaarungen.stream().map(TeamPaarung::getB).collect(Collectors.toList());
		return ImmutableList.<Team>builder().addAll(bTeams).build();
	}

	/**
	 * beide teams in eine liste A+B
	 *
	 * @return
	 */
	public ImmutableList<Team> getAundBTeams() {
		return ImmutableList.<Team>builder().addAll(getATeams()).addAll(getBTeams()).build();
	}

	/**
	 * @param teamPaarungenWork
	 */
	public FormeSpielrunde addAll(List<TeamPaarung> teamPaarungenen) {
		for (TeamPaarung teamPaarung : teamPaarungenen) {
			addPaarungWennNichtVorhanden(teamPaarung);
		}
		return this;

	}

}
