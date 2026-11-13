/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Eine Partie für {@code PUT /api/sync/tournaments/{id}/rounds/{roundNumber}}. Die Teams sind Listen von
 * Online-Registration-IDs (Formée-Team: eine ID, Mêlée/Supermêlée: je Spieler eine ID); ein leeres
 * {@code teamB} ist ein Freilos. Punkte {@code null} = Partie läuft noch. Die Teamnummern sind die Nr der
 * Meldeliste, {@code null} bei Supermêlée. Felder mit {@code null} lässt Gson beim Serialisieren weg.
 */
public record LiveMatchDto(List<String> teamA, List<String> teamB, @Nullable Integer scoreA,
        @Nullable Integer scoreB, @Nullable String court, @Nullable String stageLabel, @Nullable Integer teamANumber,
        @Nullable Integer teamBNumber) {

    /** Partie ohne Teamnummern (Supermêlée). */
    public LiveMatchDto(List<String> teamA, List<String> teamB, @Nullable Integer scoreA, @Nullable Integer scoreB,
            @Nullable String court, @Nullable String stageLabel) {
        this(teamA, teamB, scoreA, scoreB, court, stageLabel, null, null);
    }
}
