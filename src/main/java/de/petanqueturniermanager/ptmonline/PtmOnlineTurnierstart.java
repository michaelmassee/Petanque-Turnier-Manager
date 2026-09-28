/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holt einen verloren gegangenen Online-Turnierstart nach. Der Start wird mit der ersten Spielrunde im Hintergrund
 * gesendet; wird LibreOffice ohne Netz geschlossen, bevor er ankommt, gilt das Turnier online nie als im Dokument
 * durchgeführt und PTM-Online lehnt alle Runden ab. Nachgeholt wird nur, solange das Online-Turnier noch nicht
 * läuft: ein bereits laufendes wird tatsächlich online durchgeführt und darf nicht übernommen werden.
 */
final class PtmOnlineTurnierstart {

    private static final Logger logger = LogManager.getLogger(PtmOnlineTurnierstart.class);
    /** Status eines gestarteten Online-Turniers. */
    private static final String STATUS_LAEUFT = "running";

    private PtmOnlineTurnierstart() {}

    /** @return {@code true}, wenn der Start nachgeholt wurde und die Übertragung wiederholt werden kann */
    static boolean nachholen(TournamentSyncClient client, String tournamentId) throws IOException, InterruptedException {
        boolean nochNichtGestartet = client.listTournaments().stream()
                .filter(turnier -> tournamentId.equals(turnier.id))
                .anyMatch(turnier -> !STATUS_LAEUFT.equals(turnier.status));
        if (!nochNichtGestartet) {
            return false;
        }
        client.start(tournamentId);
        logger.info("PTM-Online: Turnierstart für {} nachgeholt (war beim Rundenstart nicht angekommen)",
                tournamentId);
        return true;
    }
}
