/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Antwort der Sync-Anmeldeliste: die seit dem Cursor geänderten Anmeldungen und die vollständige Konfliktliste des
 * Online-Turniers (A-29).
 */
public record AnmeldungsAbruf(List<RegistrationDto> registrations, KonfliktListeDto konflikte, SyncStandDto stand) {

    public AnmeldungsAbruf {
        registrations = List.copyOf(registrations);
        konflikte = konflikte == null ? KonfliktListeDto.leer() : konflikte;
    }

    /** Online läuft das Turnier bereits ({@code running}); ohne Turnierzustand in der Antwort {@code false}. */
    public boolean turnierLaeuft() {
        return stand != null && stand.istRunning();
    }
}
