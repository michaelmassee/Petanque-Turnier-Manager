/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

/**
 * Disziplinen einer Trip-Tête-Begegnung. Jede Partie zählt für die Begegnungswertung gleich (1 Punkt).
 *
 * @author Michael Massee
 */
public enum TripTetePartie {
	TRIPLETTE,
	DOUBLETTE,
	TETE;

	/** Begegnungspunkte pro Partie (gemäß Trip-Tête-Regelwerk: jede Partie = 1 Punkt). */
	public int punkte() {
		return 1;
	}
}
