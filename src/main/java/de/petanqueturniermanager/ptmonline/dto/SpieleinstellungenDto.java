/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import org.jspecify.annotations.Nullable;

/**
 * Spieleinstellungen des Turnierdokuments für {@code PUT /api/sync/tournaments/{id}/settings}. Das Dokument ist
 * Master: PTM-Online übernimmt die Werte und zeigt die Live-Ansicht danach. Was das System nicht kennt, bleibt
 * {@code null} und wird nicht übertragen.
 *
 * @param formation            {@code tete}, {@code doublette} oder {@code triplette}
 * @param schweizerRankingMode {@code mit_buchholz} oder {@code ohne_buchholz}
 * @param formuleXRounds       wird nicht mehr gesendet; Formule X hat keine feste Rundenzahl mehr
 * @param koPlatz3             Spiel um Platz 3 im KO-Turnierbaum
 */
public record SpieleinstellungenDto(@Nullable String formation, @Nullable String schweizerRankingMode,
        @Nullable Integer formuleXRounds, @Nullable Boolean koPlatz3) {

    public static final SpieleinstellungenDto KEINE = new SpieleinstellungenDto(null, null, null, null);

    public boolean istLeer() {
        return equals(KEINE);
    }
}
