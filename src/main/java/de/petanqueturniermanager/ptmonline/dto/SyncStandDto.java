/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

/**
 * Zustand des Online-Turniers aus Sicht des verbundenen Dokuments (Feld {@code tournament} der Sync-Anmeldeliste).
 *
 * @param status             Turnierstatus online ({@code draft}, {@code registration}, {@code running}, {@code finished})
 * @param registrationClosed Anmeldung von der Turnierleitung geschlossen
 * @param runningResetAt     Zeitpunkt, zu dem {@code running} online zurückgesetzt wurde (E-23), sonst {@code null}
 * @param roundsOnline       Anzahl der online vorhandenen Spielrunden
 * @param writeCounter       zuletzt angenommener Schreibzähler der Bindung
 */
public record SyncStandDto(String status, boolean registrationClosed, String runningResetAt, int roundsOnline,
        long writeCounter) {

    public boolean istRunning() {
        return "running".equals(status);
    }
}
