/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.time.Instant;
import java.time.format.DateTimeParseException;
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
 * @param highestRoundOnline höchste online vorhandene Rundennummer; {@code null} bei älteren Servern
 */
public record SyncStandDto(String status, boolean registrationClosed, String runningResetAt, int roundsOnline,
        long writeCounter, @Nullable String date, boolean playerResultReportingEnabled,
        @Nullable Integer highestRoundOnline) {

    /** Stand ohne bekannte höchste Rundennummer (älterer Server); es gilt die Anzahl der Runden. */
    public SyncStandDto(String status, boolean registrationClosed, String runningResetAt, int roundsOnline,
            long writeCounter, @Nullable String date, boolean playerResultReportingEnabled) {
        this(status, registrationClosed, runningResetAt, roundsOnline, writeCounter, date, playerResultReportingEnabled,
                null);
    }

    /**
     * Bis zu welcher Rundennummer online Runden stehen können. Nach einer abgelehnten früheren Runde hat die Nummerierung
     * Lücken, dann liegt die höchste über der Anzahl; ein älterer Server nennt nur die Anzahl.
     */
    public int hoechsteRundeOnline() {
        return highestRoundOnline == null ? roundsOnline : Math.max(highestRoundOnline, roundsOnline);
    }

    /**
     * Derselbe Stand mit dem per Huckepack gemeldeten Status; leer, wenn kein Status gemeldet wurde oder er sich nicht
     * geändert hat.
     */
    public Optional<SyncStandDto> mitGemeldetemStatus(@Nullable String gemeldeterStatus) {
        if (gemeldeterStatus == null || gemeldeterStatus.isBlank() || gemeldeterStatus.equals(status)) {
            return Optional.empty();
        }
        return Optional.of(new SyncStandDto(gemeldeterStatus, registrationClosed, runningResetAt, roundsOnline,
                writeCounter, date, playerResultReportingEnabled, highestRoundOnline));
    }

    /** Online abgeschlossen; ein Rundenstart aus dem Dokument würde das Turnier wieder aufnehmen. */
    public boolean istAbgeschlossen() {
        return "finished".equals(status);
    }

    public boolean istRunning() {
        return "running".equals(status);
    }

    /**
     * Der Turnierstart wurde in PTM-Online ausdrücklich zurückgesetzt (E-23) und seither nicht neu gesendet. Nur ein
     * neuer Rundenstart im Dokument nimmt das Turnier wieder auf, kein automatisches Nachholen.
     */
    public boolean istStartZurueckgesetzt() {
        return runningResetAt != null && !runningResetAt.isBlank() && !istRunning();
    }

    /** Zeitpunkt des online zurückgesetzten Starts; leer ohne Zurücksetzen oder bei unlesbarem Wert. */
    public Optional<Instant> zurueckgesetztAm() {
        if (!istStartZurueckgesetzt()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Instant.parse(runningResetAt.strip()));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    /** Online-Anmeldung läuft und ist nicht von der Turnierleitung geschlossen. */
    public boolean istAnmeldungOffen() {
        return "registration".equals(status) && !registrationClosed;
    }
}
