/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Antwort der Sync-Anmeldeliste: die seit dem Cursor geänderten Anmeldungen und die vollständige Konfliktliste des
 * Online-Turniers (A-29).
 */
public record AnmeldungsAbruf(List<RegistrationDto> registrations, KonfliktListeDto konflikte) {

    public AnmeldungsAbruf {
        registrations = List.copyOf(registrations);
        konflikte = konflikte == null ? KonfliktListeDto.leer() : konflikte;
    }
}
