/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.List;

/**
 * Ergebnis des Einlesens der Spielrunden eines Schweizer-basierten Turniers.
 *
 * @param ergebnisse         Auswertungsdaten je aktivem Team
 * @param anzGewerteteRunden Anzahl gewerteter Runden mit mindestens einem eingetragenen Ergebnis (Basis für den
 *                           Freilos-Buchholz, siehe {@link FreilosBuchholz})
 */
public record SchweizerAuswertung(List<SchweizerTeamErgebnis> ergebnisse, int anzGewerteteRunden) {

	public SchweizerAuswertung {
		ergebnisse = List.copyOf(ergebnisse);
	}
}
