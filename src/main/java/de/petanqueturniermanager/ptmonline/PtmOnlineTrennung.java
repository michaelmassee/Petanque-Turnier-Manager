/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Löst die Bindung eines Turnierdokuments (bzw. Supermelee-Spieltags) an sein Online-Turnier serverseitig: hebt
 * {@code document_managed} auf und damit die Bearbeitungssperre in der Web-UI. Gemeinsam genutzt von „Verbindung
 * trennen“ und dem Supermelee-Spieltagwechsel.
 */
public final class PtmOnlineTrennung {

    private static final Logger logger = LogManager.getLogger(PtmOnlineTrennung.class);

    private PtmOnlineTrennung() {}

    /**
     * Trennt online. Hält inzwischen ein anderes Dokument das Online-Turnier (oder niemand) oder wurde es online
     * gelöscht, gilt die Bindung als bereits gelöst – kein Fehler.
     *
     * @throws GenerateException wenn die Verbindung lokal nicht vollständig ist
     * @throws IOException       wenn der Server nicht erreichbar ist oder die Trennung ablehnt
     */
    public static void online(LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping)
            throws GenerateException, IOException, InterruptedException {
        Optional<String> tournamentId = mapping.getTournamentId();
        if (tournamentId.isEmpty()) {
            throw new GenerateException(I18n.get("ptmonline.fehler.turnier_nicht_verbunden"));
        }
        Optional<String> documentId = mapping.getSyncDocumentId();
        Optional<String> leaseToken = mapping.getLeaseToken();
        if (documentId.isEmpty() || leaseToken.isEmpty()) {
            throw new GenerateException(I18n.get("ptmonline.fehler.dokumentbindung_unvollstaendig"));
        }
        try {
            new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId.get(), leaseToken.get())
                    .disconnect(tournamentId.get());
            logger.info("PTM-Online: Turnier {} getrennt (Server-Aufruf ok)", tournamentId.get());
        } catch (PtmOnlineHttpException e) {
            if (!e.istBindungAbgeloest() && !e.istTurnierGeloescht()) {
                throw e;
            }
            logger.info("PTM-Online: Turnier {} online nicht mehr an dieses Dokument gebunden oder gelöscht, trenne nur lokal",
                    tournamentId.get(), e);
        }
    }
}
