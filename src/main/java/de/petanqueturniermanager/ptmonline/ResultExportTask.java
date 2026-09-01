/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.dto.RegistrationResultDto;

/**
 * Exportiert die Setzposition bereits importierter Anmeldungen zurueck nach PTM-Online. Der
 * Status wird bewusst NICHT gesetzt ({@code null}): laut PTM-Online-API
 * ({@code syncPostResults} in {@code worker.js}, {@code COALESCE(?, status)}) bleibt der
 * Online-Status dann unveraendert — es gibt lokal keine verlaessliche, bereits erschlossene
 * Quelle fuer einen Registrierungs-Status (Checkin o.ae.), daher wird hier nichts geraten.
 * <p>
 * Als {@code seedingPosition} dient die beim Import in {@link PtmOnlineRegistrationMapping}
 * gemerkte Meldeliste-Zeile — kein neuer Sheet-Lesepfad noetig, da diese Zuordnung schon
 * vorliegt. Teams ohne Mapping-Eintrag (lokal manuell erfasst, nie online importiert) werden
 * nicht exportiert.
 */
public final class ResultExportTask {

    private static final Logger logger = LogManager.getLogger(ResultExportTask.class);

    private ResultExportTask() {}

    public static void starte(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(new DocumentPropertiesHelper(ws));
        Optional<String> tournamentId = mapping.getTournamentId();
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_angelegt"));
            return;
        }

        Map<Integer, String> alleMappings = mapping.getAlleMappings();
        if (alleMappings.isEmpty()) {
            zeigeInfo(ctx, I18n.get("ptmonline.erfolg.ergebnisse_exportiert", 0));
            return;
        }

        List<RegistrationResultDto> results = alleMappings.entrySet().stream()
                .map(eintrag -> new RegistrationResultDto(eintrag.getValue(), null, eintrag.getKey()))
                .toList();

        Thread worker = new Thread(
                () -> exportiereImHintergrund(ctx, config, tournamentId.get(), results), "PTM-Online-Export");
        worker.start();
    }

    private static void exportiereImHintergrund(
            XComponentContext ctx, LibreOfficePtmOnlineSpeicher.Zugangsdaten config, String tournamentId,
            List<RegistrationResultDto> results) {
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            int aktualisiert = client.pushResults(tournamentId, results);
            LoMainThread.post(ctx, () -> zeigeInfo(ctx, I18n.get("ptmonline.erfolg.ergebnisse_exportiert", aktualisiert)));
        } catch (IOException e) {
            logger.error("PTM-Online: Ergebnisse exportieren fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        boolean nichtFreigeschaltet = meldung.contains(" 401");
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(nichtFreigeschaltet
                        ? I18n.get("ptmonline.fehler.nicht_freigeschaltet")
                        : I18n.get("ptmonline.fehler.netzwerk", meldung))
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }

    private static void zeigeInfo(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(meldung)
                .show();
    }
}
