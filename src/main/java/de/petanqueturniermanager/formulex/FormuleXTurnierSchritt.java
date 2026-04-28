package de.petanqueturniermanager.formulex;

/**
 * Aktueller Turnierstatus eines Formule-X-Turniers für die Sidebar-Anzeige.
 */
public record FormuleXTurnierSchritt(
        boolean spielrundeVorhanden,
        int aktuelleRundeNr,
        int rundeGespielt,
        int rundeGesamt,
        int anzahlRunden,
        boolean beendet) {
}
