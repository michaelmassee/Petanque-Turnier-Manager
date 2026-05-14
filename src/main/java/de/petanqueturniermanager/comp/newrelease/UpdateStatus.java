/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.newrelease;

/**
 * Status des Update-Checks – publizierter Zustand des
 * {@link ReleaseUpdateService}.
 */
public enum UpdateStatus {
    /** Noch keine Prüfung durchgeführt – Initialzustand. */
    UNBEKANNT,
    /** Prüfung läuft (Hintergrund-Task aktiv). */
    LAEUFT,
    /** Prüfung abgeschlossen, installierte Version ist aktuell. */
    KEIN_UPDATE,
    /** Prüfung abgeschlossen, eine neuere Version steht bereit. */
    UPDATE_VERFUEGBAR,
    /** Prüfung scheiterte (Netz/Cache/Parsing) – Anzeige fällt auf installierte Version zurück. */
    NICHT_VERFUEGBAR
}
