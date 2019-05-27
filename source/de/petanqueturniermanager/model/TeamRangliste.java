/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;

import com.google.common.collect.ImmutableList;

/**
 * @author Michael Massee
 *
 */
public class TeamRangliste {

	private final ArrayList<Team> teamListe = new ArrayList<>();

	public TeamRangliste addAll(List<Team> teamlist) {
		for (Team team : teamlist) {
			add(team);
		}
		return this;
	}

	public Team add(Team team) {
		checkNotNull(team);
		checkArgument(!teamListe.contains(team));
		teamListe.add(team);
		return team;
	}

	public ArrayList<Team> getCloneTeamListe() {
		ArrayList<Team> clone = new ArrayList<>();
		clone.addAll(teamListe);
		return clone;
	}

	public TeamRangliste ranglisteVonOben(int anzSpieler) {
		TeamRangliste retListe = new TeamRangliste();
		List<Team> sublist = teamListe.subList(0, anzSpieler);
		retListe.addAll(sublist);
		return retListe;
	}

	public TeamRangliste ranglisteVonLetzte(int anzSpieler) {
		TeamRangliste retListe = new TeamRangliste();
		List<Team> sublist = teamListe.subList(teamListe.size() - anzSpieler, teamListe.size());
		retListe.addAll(sublist);
		return retListe;
	}

	public ImmutableList<Team> getTeamListe() {
		return ImmutableList.<Team>builder().addAll(teamListe).build();
	}

	/**
	 * @return
	 */
	public boolean isEmpty() {
		return teamListe.isEmpty();
	}

	/**
	 * @return
	 */
	public int size() {
		return teamListe.size();
	}

	/**
	 * @param idx
	 * @return
	 */
	public Team get(int idx) {
		return teamListe.get(idx);
	}

}
