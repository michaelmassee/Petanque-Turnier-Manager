package de.petanqueturniermanager.kaskade;

/**
 * Aktueller Turnierstatus eines Kaskaden-KO-Turniers für die Sidebar-Anzeige.
 */
public record KaskadeTurnierSchritt(
        boolean rundeVorhanden,
        int aktuelleRundeNr,
        int rundeGespielt,
        int rundeGesamt,
        boolean koPhaseVorhanden,
        boolean beendet) {
}
