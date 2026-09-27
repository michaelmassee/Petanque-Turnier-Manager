/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Ein Ranglisten-Eintrag für {@code PUT /api/sync/tournaments/{id}/ranking}. Bei Gleichstand dürfen mehrere
 * Einträge denselben Platz haben.
 */
public record LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
        @Nullable Integer pointsFor, @Nullable Integer pointsAgainst) {
}
