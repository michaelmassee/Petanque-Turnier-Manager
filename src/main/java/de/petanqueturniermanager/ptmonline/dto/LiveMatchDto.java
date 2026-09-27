/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Eine Partie für {@code PUT /api/sync/tournaments/{id}/rounds/{roundNumber}}. Die Teams sind Listen von
 * Online-Registration-IDs (Formée-Team: eine ID, Mêlée/Supermêlée: je Spieler eine ID); ein leeres
 * {@code teamB} ist ein Freilos. Punkte {@code null} = Partie läuft noch. Felder mit {@code null} lässt
 * Gson beim Serialisieren weg.
 */
public record LiveMatchDto(List<String> teamA, List<String> teamB, @Nullable Integer scoreA,
        @Nullable Integer scoreB, @Nullable String court, @Nullable String stageLabel) {
}
