/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

/**
 * Ein lokal gemischtes Mêlée-Team für {@code PUT /api/sync/tournaments/{id}/melee-teams} (KP-18): die Online-IDs der
 * Einzelanmeldungen in Spielerreihenfolge der Meldeliste.
 */
public record MeleeTeamDto(String teamUuid, List<String> registrationIds) {

    public MeleeTeamDto {
        registrationIds = List.copyOf(registrationIds);
    }
}
