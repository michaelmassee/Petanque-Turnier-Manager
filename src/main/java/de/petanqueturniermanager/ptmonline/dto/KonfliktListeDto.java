/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Doppelbelegungen und mögliche Dubletten über alle Anmeldungen des Online-Turniers (Feld {@code conflicts} der
 * Sync-Anmeldeliste, KP-06). Wird unabhängig vom {@code since}-Cursor immer vollständig geliefert.
 *
 * @param accountConflicts   dieselbe Benutzer-ID in mehreren aktiven Anmeldungen – echter Ausschlussgrund
 * @param possibleDuplicates gleicher Name oder gleiche E-Mail ohne gemeinsames Konto – nur ein Hinweis
 */
public record KonfliktListeDto(List<KontoKonflikt> accountConflicts, List<MoeglicheDublette> possibleDuplicates) {

    public KonfliktListeDto {
        accountConflicts = accountConflicts == null ? List.of() : List.copyOf(accountConflicts);
        possibleDuplicates = possibleDuplicates == null ? List.of() : List.copyOf(possibleDuplicates);
    }

    public static KonfliktListeDto leer() {
        return new KonfliktListeDto(List.of(), List.of());
    }

    /** Ein Konto in mehreren Anmeldungen. */
    public record KontoKonflikt(String userId, List<String> registrationIds) {
        public KontoKonflikt {
            registrationIds = registrationIds == null ? List.of() : List.copyOf(registrationIds);
        }
    }

    /** Anmeldungen mit gleichem Namen ({@code kind = name}) oder gleicher Slot-E-Mail ({@code kind = email}). */
    public record MoeglicheDublette(String kind, List<String> registrationIds) {
        public MoeglicheDublette {
            registrationIds = registrationIds == null ? List.of() : List.copyOf(registrationIds);
        }
    }
}
