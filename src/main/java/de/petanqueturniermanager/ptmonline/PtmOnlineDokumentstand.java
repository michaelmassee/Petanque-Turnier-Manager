/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;

/**
 * Das Desktop-Dokument ist maßgeblich: PTM-Online zeigt nur, was im Turnierdokument steht. Meldet PTM-Online
 * {@code document_forked} – es hat schon Aufträge dieser Bindung mit höherem Zählerstand angenommen, meist weil ein
 * älterer, nicht gespeicherter Stand der Datei geöffnet wurde –, übernimmt das Dokument nach einmaliger Rückfrage den
 * Zählerstand des Servers und überträgt danach seinen vollständigen Stand.
 * <p>
 * Die Rückfrage bleibt, weil derselbe Befund auch bei zwei gleichzeitig benutzten Kopien entsteht; ohne sie würden sich
 * die Kopien online gegenseitig überschreiben.
 */
final class PtmOnlineDokumentstand {

    private static final Logger logger = LogManager.getLogger(PtmOnlineDokumentstand.class);

    private PtmOnlineDokumentstand() {}

    /** Fragt, ob der Stand dieses Dokuments online gelten soll. Nur auf dem Main-Thread oder in einem SheetRunner. */
    static boolean frageUebernahme(XComponentContext xContext) {
        return MessageBox.from(xContext, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.frage.dokumentstand_uebernehmen.titel"))
                .message(I18n.get("ptmonline.frage.dokumentstand_uebernehmen"))
                .show() == MessageBoxResult.YES;
    }

    /**
     * Übernimmt den Zählerstand von PTM-Online in den Bestand; offene Aufträge gehen danach mit neuen Zählerständen
     * hinaus. Läuft nie auf dem Main-Thread (Netzaufruf). Gespeichert wird der Bestand vor dem nächsten Versand.
     */
    static void uebernehme(AuftragsBestand bestand, TournamentSyncClient client, String tournamentId)
            throws IOException, InterruptedException {
        long serverZaehler = client.fetchSyncStand(tournamentId).writeCounter();
        bestand.versandSperre().lockInterruptibly();
        try {
            bestand.uebernehmeServerZaehler(serverZaehler);
        } finally {
            bestand.versandSperre().unlock();
        }
        logger.info("PTM-Online: Stand des Dokuments gilt für Turnier {}, Server-Zähler {} übernommen", tournamentId,
                serverZaehler);
    }
}
