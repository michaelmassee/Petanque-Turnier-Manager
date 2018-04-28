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
public class SpielRunde extends NrComparable<SpielRunde> implements TurnierDaten {
	private final ArrayList<Team> teams;

	public SpielRunde(int nr) {
		super(nr);
		this.teams = new ArrayList<>();
	}

	public List<Team> teams() {
		return new ArrayList<>(this.teams);
	}

	public Team newTeam() throws AlgorithmenException {
		Team newTeam = new Team(this.teams.size() + 1);
		addTeamWennNichtVorhanden(newTeam);
		return newTeam;
	}

	public void validateSpielerTeam(Team newTeam) throws AlgorithmenException {
		HashSet<Integer> spielrNrSet = new HashSet<>();
		for (Team team : this.teams) {
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

	public SpielRunde addTeamsWennNichtVorhanden(List<Team> teams) throws AlgorithmenException {
		for (Team team : teams) {
			addTeamWennNichtVorhanden(team);
		}
		return this;
	}

	public SpielRunde addTeamWennNichtVorhanden(Team team) throws AlgorithmenException {
		if (!this.teams.contains(team)) {
			validateSpielerTeam(team);
			this.teams.add(team);
		}
		return this;
	}

	public SpielRunde deleteAllTeams() throws AlgorithmenException {
		for (Team team : this.teams) {
			team.removeAlleSpieler();
		}
		this.teams.clear();
		return this;
	}

	public SpielRunde removeSpieler(Spieler spieler) throws AlgorithmenException {
		for (Team team : this.teams) {
			team.removeSpieler(spieler);
		}
		return this;
	}

	public SpielRunde sortiereTeamsNachGroese() {
		this.teams.sort(new Comparator<Team>() {
			@Override
			public int compare(Team o1, Team o2) {
				if (o1.size() < o2.size())
					return -1;
				if (o1.size() > o2.size())
					return 1;
				return 0;
			};
		});
		return this;
	}

	@Override
	public String toString() {

		String teamsStr = "[";
		for (Team team : this.teams) {
			if (teamsStr.length() > 1) {
				teamsStr += ",";
			}
			teamsStr += team.toString();
		}
		teamsStr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Nr", this.nr)
				.add("Teams", teamsStr)
				.toString();
		// @formatter:on

	}

}
