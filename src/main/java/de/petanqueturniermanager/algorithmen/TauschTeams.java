package de.petanqueturniermanager.algorithmen;

import de.petanqueturniermanager.model.Team;
import de.petanqueturniermanager.model.TeamPaarung;

/** Kapselt einen gefundenen Tausch: das Team aus der Rangliste und die Paarung, deren B getauscht wird. */
public record TauschTeams(Team teamAusRangliste, TeamPaarung teamPaarungTausch) {}
