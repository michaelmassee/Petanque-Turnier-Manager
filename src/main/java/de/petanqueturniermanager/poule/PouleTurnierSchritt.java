package de.petanqueturniermanager.poule;

/**
 * Ergebnis der Poule-Status-Auswertung.
 */
public record PouleTurnierSchritt(
        boolean vorrundeVorhanden,
        int vorrundeGespielt,
        int vorrundeGesamt,
        boolean koVorhanden,
        boolean beendet) {
}
