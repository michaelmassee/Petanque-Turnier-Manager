/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Antwort der Sync-Anmeldeliste: die seit dem Cursor geänderten Anmeldungen und die vollständige Konfliktliste des
 * Online-Turniers (A-29).
 *
 * @param cursor Zeitstempel des Servers für den nächsten {@code since}-Abruf (jüngstes {@code updated_at} der
 *               gelieferten Anmeldungen bzw. das angefragte {@code since}); leer, wenn der Server keinen liefert
 */
public record AnmeldungsAbruf(List<RegistrationDto> registrations, KonfliktListeDto konflikte, SyncStandDto stand,
        Optional<Instant> cursor) {

    public AnmeldungsAbruf {
        registrations = List.copyOf(registrations);
        konflikte = konflikte == null ? KonfliktListeDto.leer() : konflikte;
        cursor = cursor == null ? Optional.empty() : cursor;
    }

    /** Online läuft das Turnier bereits ({@code running}); ohne Turnierzustand in der Antwort {@code false}. */
    public boolean turnierLaeuft() {
        return stand != null && stand.istRunning();
    }
}
