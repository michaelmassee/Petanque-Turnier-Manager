package de.petanqueturniermanager.ko;

/**
 * Aktueller Turnierstatus eines KO-Turniers für die Sidebar-Anzeige.
 */
public record KoTurnierSchritt(
        boolean turnierbaumVorhanden,
        boolean beendet) {
}
