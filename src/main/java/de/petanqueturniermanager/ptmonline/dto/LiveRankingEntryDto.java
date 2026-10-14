/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Ein Ranglisten-Eintrag für {@code PUT /api/sync/tournaments/{id}/ranking}. Bei Gleichstand dürfen mehrere
 * Einträge denselben Platz haben. Wertungskriterien, die das System nicht führt, bleiben {@code null} und werden
 * nicht übertragen.
 */
public record LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
        @Nullable Integer pointsFor, @Nullable Integer pointsAgainst, @Nullable Integer losses,
        @Nullable Integer gameDiff, @Nullable Integer bhz, @Nullable Integer fbhz, @Nullable Integer wertung) {

    public LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
            @Nullable Integer pointsFor, @Nullable Integer pointsAgainst) {
        this(place, registrationIds, wins, pointsFor, pointsAgainst, null, null, null, null, null);
    }
}
