package de.petanqueturniermanager.maastrichter;

/**
 * Aktueller Turnierstatus eines Maastrichter-Turniers für die Sidebar-Anzeige.
 */
public record MaastrichterTurnierSchritt(
        boolean vorrundeVorhanden,
        int aktuelleVorrundeNr,
        int vorrundeGespielt,
        int vorrundeGesamt,
        boolean finalrundeVorhanden,
        boolean beendet) {
}
