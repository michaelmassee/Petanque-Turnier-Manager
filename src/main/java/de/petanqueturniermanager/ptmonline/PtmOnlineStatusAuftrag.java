/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.util.List;

import org.jspecify.annotations.Nullable;

/**
 * Teilnahme und Setzposition der lokalen Meldungen einer Verbindung, im Dokument-Kontext gelesen. Daraus entsteht
 * ein Teilnahme-Auftrag ({@link PtmOnlineAuftraege#teilnahme}); Meldungen ohne Online-Zuordnung werden dabei
 * übergangen.
 */
record PtmOnlineStatusAuftrag(String tournamentId, List<Eintrag> eintraege) {

    PtmOnlineStatusAuftrag {
        eintraege = List.copyOf(eintraege);
    }

    /**
     * @param lokaleUuid      dauerhafte Kennung der Meldelistenzeile
     * @param seedingPosition lokale Setzposition, {@code null} = keine (löscht die online gepflegte)
     */
    record Eintrag(String lokaleUuid, OnlineTeilnahme teilnahme, @Nullable Integer seedingPosition) {
    }
}
