/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Holt einen verloren gegangenen Online-Turnierstart nach. Der Start wird mit der ersten Spielrunde im Hintergrund
 * gesendet; wird LibreOffice ohne Netz geschlossen, bevor er ankommt (oder wurde das Dokument nach dem Trennen neu
 * verbunden), gilt das Turnier online nicht als im Dokument durchgeführt und PTM-Online lehnt alle Runden ab. Ob das
 * Dokument übernehmen darf, entscheidet PTM-Online: ein bereits mit Online-Runden durchgeführtes Turnier lehnt der
 * Start ebenfalls ab.
 */
final class PtmOnlineTurnierstart {

    private static final Logger logger = LogManager.getLogger(PtmOnlineTurnierstart.class);

    private PtmOnlineTurnierstart() {}

    /** @return {@code true}, wenn der Start nachgeholt wurde und die Übertragung wiederholt werden kann */
    static boolean nachholen(TournamentSyncClient client, String tournamentId) throws IOException, InterruptedException {
        try {
            client.start(tournamentId);
        } catch (PtmOnlineHttpException e) {
            if (!e.istOnlineDurchgefuehrt()) {
                throw e;
            }
            return false;
        }
        logger.info("PTM-Online: Turnierstart für {} nachgeholt (war online nicht als Dokument-Durchführung markiert)",
                tournamentId);
        return true;
    }
}
