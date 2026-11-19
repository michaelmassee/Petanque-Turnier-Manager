/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Ein Ranglisten-Eintrag für {@code PUT /api/sync/tournaments/{id}/ranking}. Bei Gleichstand dürfen mehrere
 * Einträge denselben Platz haben. Wertungskriterien und Zusatzspalten, die das Dokument nicht zeigt, bleiben
 * {@code null} und werden nicht übertragen.
 *
 * @param group      Gruppe (Spalte „Gr“ der Maastrichter-Vorrunden-Rangliste), sobald zugeteilt
 * @param byeRounds  Runden mit Freilos (Spalte „FL“), nur wenn das Dokument die Spalte zeigt
 * @param teamNumber Nr des Teams bzw. Spielers im Dokument (Spalte „Nr“); {@code null} bei mehreren Nummern
 */
public record LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
        @Nullable Integer pointsFor, @Nullable Integer pointsAgainst, @Nullable Integer losses,
        @Nullable Integer gameDiff, @Nullable Integer bhz, @Nullable Integer fbhz, @Nullable Integer wertung,
        @Nullable String group, @Nullable String byeRounds, @Nullable Integer teamNumber) {

    public LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
            @Nullable Integer pointsFor, @Nullable Integer pointsAgainst, @Nullable Integer losses,
            @Nullable Integer gameDiff, @Nullable Integer bhz, @Nullable Integer fbhz, @Nullable Integer wertung,
            @Nullable String group, @Nullable String byeRounds) {
        this(place, registrationIds, wins, pointsFor, pointsAgainst, losses, gameDiff, bhz, fbhz, wertung, group,
                byeRounds, null);
    }

    public LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
            @Nullable Integer pointsFor, @Nullable Integer pointsAgainst, @Nullable Integer losses,
            @Nullable Integer gameDiff, @Nullable Integer bhz, @Nullable Integer fbhz, @Nullable Integer wertung) {
        this(place, registrationIds, wins, pointsFor, pointsAgainst, losses, gameDiff, bhz, fbhz, wertung, null, null);
    }

    public LiveRankingEntryDto(int place, List<String> registrationIds, @Nullable Integer wins,
            @Nullable Integer pointsFor, @Nullable Integer pointsAgainst) {
        this(place, registrationIds, wins, pointsFor, pointsAgainst, null, null, null, null, null, null, null, null);
    }
}
