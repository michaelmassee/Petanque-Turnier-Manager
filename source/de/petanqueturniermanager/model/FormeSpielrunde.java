/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;

import com.google.common.base.MoreObjects;

/**
 * @author Michael Massee
 *
 */
public class FormeSpielrunde extends NrComparable<FormeSpielrunde> implements TurnierDaten {

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
		TeamPaarung isInList = getTeamPaarungen().stream().filter(teamPaarungausList -> teamPaarungausList.equals(teamPaarung)).findFirst().orElse(null);
		if (null == isInList) {
			getTeamPaarungen().add(teamPaarung);
		}
		return this;
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		for (TeamPaarung teamPaarung : getTeamPaarungen()) {
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

	public ArrayList<TeamPaarung> getTeamPaarungen() {
		return teamPaarungen;
	}

}
