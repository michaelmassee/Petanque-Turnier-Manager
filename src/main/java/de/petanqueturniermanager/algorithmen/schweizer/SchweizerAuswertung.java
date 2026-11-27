/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.List;
import java.util.Set;

/**
 * Ergebnis des Einlesens der Spielrunden eines Schweizer-basierten Turniers.
 *
 * @param ergebnisse         Auswertungsdaten je Team mit Historie (alle Meldungen, unabhängig vom Aktiv-Wert, damit
 *                           Buchholz/Feinbuchholz der Gegner vollständig bleiben)
 * @param anzGewerteteRunden Anzahl gewerteter Runden mit mindestens einem eingetragenen Ergebnis (Basis für den
 *                           Freilos-Buchholz, siehe {@link FreilosBuchholz})
 * @param teamsMitPartie     Teams, die in einer gewerteten Runde vorkommen (Partie oder Freilos)
 */
public record SchweizerAuswertung(List<SchweizerTeamErgebnis> ergebnisse, int anzGewerteteRunden,
		Set<Integer> teamsMitPartie) {

	public SchweizerAuswertung {
		ergebnisse = List.copyOf(ergebnisse);
		teamsMitPartie = Set.copyOf(teamsMitPartie);
	}
}
