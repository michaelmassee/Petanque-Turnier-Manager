/**
 * Erstellung 02.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * @author Michael Massee
 *
 */
public class TeamMeldungen implements IMeldungen<TeamMeldungen, Team> {

	private final ArrayList<Team> teamList;

	public TeamMeldungen() {
		teamList = new ArrayList<>();
	}

	public boolean isValid() {
		return (teamList != null) && teamList.size() > 3;
	}

	@Override
	public TeamMeldungen addNewWennNichtVorhanden(RowData meldungZeile) {
		int teamNr = meldungZeile.get(0).getIntVal(-1);
		if (teamNr > 0) {
			this.addTeamWennNichtVorhanden(Team.from(teamNr));
		}
		return this;
	}

	public TeamMeldungen addTeamWennNichtVorhanden(List<Team> teamList) {
		checkNotNull(teamList, "teamList == null");
		checkArgument(!teamList.isEmpty(), "teamList ist leer");

		for (Team teamAusList : teamList) {
			addTeamWennNichtVorhanden(teamAusList);
		}
		return this;
	}

	public TeamMeldungen addTeamWennNichtVorhanden(Team team) {
		checkNotNull(team, "team == null");

		if (!teamList.contains(team)) {
			teamList.add(team);
		}
		return this;
	}

	public final Iterable<Team> getTeamList() {
		return teamList;
	}

	@Override
	public List<IMeldung<Team>> getMeldungen() {
		return new ArrayList<>(teamList);
	}

	public List<IMeldung<Team>> getMeldungenSortedByNr() {
		return getMeldungen().stream().sorted(Comparator.comparingInt(IMeldung::getNr)).toList();
	}

	public List<Team> teams() {
		return new ArrayList<>(teamList);
	}

	/**
	 * @param i
	 */
	public Team getTeam(int teamNr) {
		return teamList.stream().filter(tm -> tm.getNr() == teamNr).findFirst().orElse(null);
	}

}
