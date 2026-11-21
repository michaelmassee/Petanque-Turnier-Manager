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
 * @param liveViewEnabled      Live-Ansicht freigeschaltet; {@code null} nur bei Systemen ohne Online-Gegenstück
 * @param playerResultReportingEnabled Spieler dürfen Ergebnisse melden; {@code null} bei Systemen ohne Spielermeldung
 *                             (Trip-Tête): Online-Einstellung beibehalten
 */
public record SpieleinstellungenDto(@Nullable String formation, @Nullable String schweizerRankingMode,
        @Nullable Integer formuleXRounds, @Nullable Boolean koPlatz3, @Nullable Boolean liveViewEnabled,
        @Nullable Boolean playerResultReportingEnabled) {

    public static final SpieleinstellungenDto KEINE = new SpieleinstellungenDto(null, null, null, null);

    /** Ohne Angaben zu Live-Ansicht und Ergebnismeldung: Online bleiben sie unverändert. */
    public SpieleinstellungenDto(@Nullable String formation, @Nullable String schweizerRankingMode,
            @Nullable Integer formuleXRounds, @Nullable Boolean koPlatz3) {
        this(formation, schweizerRankingMode, formuleXRounds, koPlatz3, null, null);
    }

    /** Dieselben Spieleinstellungen mit den Schaltern für Live-Ansicht und Ergebnismeldung aus dem Dokument. */
    public SpieleinstellungenDto mitLive(@Nullable Boolean live, @Nullable Boolean ergebnismeldung) {
        return new SpieleinstellungenDto(formation, schweizerRankingMode, formuleXRounds, koPlatz3, live, ergebnismeldung);
    }

    public boolean istLeer() {
        return equals(KEINE);
    }
}
