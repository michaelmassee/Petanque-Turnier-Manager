/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;

/**
 * Löst die Bindung eines Turnierdokuments (bzw. Supermelee-Spieltags) an sein Online-Turnier serverseitig: hebt
 * {@code document_managed} auf und damit die Bearbeitungssperre in der Web-UI. Gemeinsam genutzt von „Verbindung
 * trennen“ und dem Supermelee-Spieltagwechsel. Das Trennen ist ein gezählter Auftrag (Auftragsmatrix); noch offene
 * Aufträge der Bindung werden dabei verworfen. Läuft synchron im SheetRunner.
 */
public final class PtmOnlineTrennung {

    private static final Logger logger = LogManager.getLogger(PtmOnlineTrennung.class);

    private PtmOnlineTrennung() {}

    /**
     * Trennt online. Hält inzwischen ein anderes Dokument das Online-Turnier (oder niemand) oder wurde es online
     * gelöscht, gilt die Bindung als bereits gelöst – kein Fehler.
     *
     * @throws GenerateException wenn die Verbindung lokal nicht vollständig ist oder PTM-Online das Trennen ablehnt
     * @throws IOException       wenn PTM-Online nicht erreichbar ist
     */
    public static void online(WorkingSpreadsheet ws, @Nullable Integer spieltagNr,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping)
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
        AuftragsBestand bestand = PtmOnlineAuftraege.bestand(ws, mapping, spieltagNr);
        PtmOnlineAuftraege.trennen(bestand, tournamentId.get(), I18n.get("ptmonline.auftrag.verworfen.trennen"));
        PtmOnlineAuftraege.SynchronerVersand versand = PtmOnlineAuftraege.sendeSynchron(bestand, mapping,
                new TournamentSyncClient(config.baseUrl(), config.apiKey(), documentId.get(), leaseToken.get()),
                false);
        switch (versand.stopp()) {
            case FERTIG -> pruefeAblehnung(versand.anwendung().abgelehnt(AuftragsArt.TRENNEN));
            case BINDUNG_ABGELOEST, TURNIER_GELOESCHT -> logger.info(
                    "PTM-Online: Turnier {} online nicht mehr an dieses Dokument gebunden oder gelöscht, trenne nur lokal",
                    tournamentId.get());
            case DOKUMENT_GEFORKT -> throw new GenerateException(I18n.get("ptmonline.fehler.dokument_geforkt"));
            case NETZ, SERVERFEHLER, NICHT_BERECHTIGT -> throw new IOException(
                    I18n.get("ptmonline.fehler.trennen_nicht_gesendet", versand.stopp()));
        }
        logger.info("PTM-Online: Turnier {} getrennt", tournamentId.get());
    }

    private static void pruefeAblehnung(Optional<VersandErgebnis> ablehnung) throws GenerateException {
        if (ablehnung.isPresent()) {
            throw new GenerateException(PtmOnlineFehlerText.fuer(new PtmOnlineHttpException(
                    ablehnung.get().antwort().status(), ablehnung.get().antwort().body())));
        }
    }
}
