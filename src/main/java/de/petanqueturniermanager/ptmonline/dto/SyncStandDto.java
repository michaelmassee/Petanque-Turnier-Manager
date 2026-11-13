/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.Optional;

import org.jspecify.annotations.Nullable;

/**
 * Zustand des Online-Turniers aus Sicht des verbundenen Dokuments (Feld {@code tournament} der Sync-Anmeldeliste).
 *
 * @param status             Turnierstatus online ({@code draft}, {@code registration}, {@code running}, {@code finished})
 * @param registrationClosed Anmeldung von der Turnierleitung geschlossen
 * @param runningResetAt     Zeitpunkt, zu dem {@code running} online zurückgesetzt wurde (E-23), sonst {@code null}
 * @param roundsOnline       Anzahl der online vorhandenen Spielrunden
 * @param writeCounter       zuletzt angenommener Schreibzähler der Bindung
 * @param date               Turniertag ({@code yyyy-MM-dd}), {@code null} bei älteren Servern
 * @param playerResultReportingEnabled Spieler dürfen Ergebnisse in der Live-Ansicht melden (E-27); ältere Server
 *                                     liefern das Feld nicht, dann {@code false}
 */
public record SyncStandDto(String status, boolean registrationClosed, String runningResetAt, int roundsOnline,
        long writeCounter, @Nullable String date, boolean playerResultReportingEnabled) {

    /**
     * Derselbe Stand mit dem per Huckepack gemeldeten Status; leer, wenn kein Status gemeldet wurde oder er sich nicht
     * geändert hat.
     */
    public Optional<SyncStandDto> mitGemeldetemStatus(@Nullable String gemeldeterStatus) {
        if (gemeldeterStatus == null || gemeldeterStatus.isBlank() || gemeldeterStatus.equals(status)) {
            return Optional.empty();
        }
        return Optional.of(new SyncStandDto(gemeldeterStatus, registrationClosed, runningResetAt, roundsOnline,
                writeCounter, date, playerResultReportingEnabled));
    }

    public boolean istRunning() {
        return "running".equals(status);
    }

    /** Online-Anmeldung läuft und ist nicht von der Turnierleitung geschlossen. */
    public boolean istAnmeldungOffen() {
        return "registration".equals(status) && !registrationClosed;
    }
}
