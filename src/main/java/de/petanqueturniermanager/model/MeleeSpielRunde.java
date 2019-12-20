package de.petanqueturniermanager.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.AlgorithmenException;

/*
* SpielRunde.java
*
* Erstellung     : 07.09.2017 / Michael Massee
*
*/
public class MeleeSpielRunde extends NrComparable {
	private final ArrayList<Team> teams;

	public MeleeSpielRunde(int nr) {
		super(nr);
		teams = new ArrayList<>();
	}

	public List<Team> teams() {
		return new ArrayList<>(teams);
	}

	public Team newTeam() throws AlgorithmenException {
		Team newTeam = Team.from(teams.size() + 1);
		addTeamWennNichtVorhanden(newTeam);
		return newTeam;
	}

	public void validateSpielerTeam(Team newTeam) throws AlgorithmenException {
		HashSet<Integer> spielrNrSet = new HashSet<>();
		for (Team team : teams) {
			for (Spieler spieler : team.spieler()) {
				int spielrNr = spieler.getNr();
				if (spielrNrSet.contains(spielrNr)) {
					throw new AlgorithmenException("Spieler bereits vorhanden " + spielrNr);
				}
				spielrNrSet.add(spielrNr);
			}
		}

		// neue Team pruefen
		if (newTeam != null) {
			for (Spieler spieler : newTeam.spieler()) {
				int spielrNr = spieler.getNr();
				if (spielrNrSet.contains(spielrNr)) {
					throw new AlgorithmenException("Spieler bereits vorhanden " + spielrNr);
				}
				spielrNrSet.add(spielrNr);
			}
		}
	}

	public MeleeSpielRunde addTeamsWennNichtVorhanden(List<Team> teams) throws AlgorithmenException {
		for (Team team : teams) {
			addTeamWennNichtVorhanden(team);
		}
		return this;
	}

	public MeleeSpielRunde addTeamWennNichtVorhanden(Team team) throws AlgorithmenException {
		if (!teams.contains(team)) {
			validateSpielerTeam(team);
			teams.add(team);
		}
		return this;
	}

	public MeleeSpielRunde deleteAllTeams() throws AlgorithmenException {
		for (Team team : teams) {
			team.removeAlleSpieler();
		}
		teams.clear();
		return this;
	}

	public MeleeSpielRunde removeSpieler(Spieler spieler) throws AlgorithmenException {
		for (Team team : teams) {
			team.removeSpieler(spieler);
		}
		return this;
	}

	public MeleeSpielRunde sortiereTeamsNachGroese() {
		teams.sort(new Comparator<Team>() {
			@Override
			public int compare(Team o1, Team o2) {
				if (o1.size() < o2.size())
					return -1;
				if (o1.size() > o2.size())
					return 1;
				return 0;
			}
		});
		return this;
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		for (Team team : teams) {
			if (teamsStr.length() > 1) {
				teamsStr += ",";
			}
			teamsStr += team.toString();
		}
		teamsStr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Nr", nr)
				.add("Teams", teamsStr)
				.toString();
		// @formatter:on

	}

}
