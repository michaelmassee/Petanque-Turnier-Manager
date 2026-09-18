/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.TurnierSystemOnlineTypMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask;
import de.petanqueturniermanager.ptmonline.ResultExportTask;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;

/**
 * Bindeglied zwischen {@link de.petanqueturniermanager.comp.ProtocolHandler} und der PTM-Online-
 * REST-Anbindung. Muster analog {@link de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher}:
 * statische Methoden, Fehler werden als {@link MessageBox} gemeldet statt zu crashen. Netzwerk-I/O
 * laeuft off-thread (Dispatch-Aufrufkette darf nicht blockieren); Sheet-/Dokument-Schreibzugriffe
 * werden per {@link LoMainThread#post} zurueck auf den Main-Thread marshalliert.
 */
public final class PtmOnlineDispatcher {

    private static final Logger logger = LogManager.getLogger(PtmOnlineDispatcher.class);

    private PtmOnlineDispatcher() {}

    /**
     * Verbindet das Dokument (bzw. bei Supermelee den aktiven Spieltag) mit einem bereits
     * bestehenden PTM-Online-Turnier: laedt die zum API-Key gehoerenden, zum lokalen
     * {@link TurnierSystem} passenden Turniere, laesst den Nutzer eines auswaehlen und legt
     * anschliessend die beiden Sheets "Turnierinformationen"/"Meldungen" an.
     */
    public static void turnierVerbinden(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        TurnierSystem ts;
        Integer spieltagNr;
        try {
            ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
        } catch (GenerateException e) {
            logger.error("PTM-Online: aktiven Spieltag ermitteln fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
            return;
        }

        if (TurnierSystemOnlineTypMapping.onlineTyp(ts).isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.turnier.verbinden.dialog.fehler.system_nicht_unterstuetzt"));
            return;
        }

        Thread worker = new Thread(
                () -> verbindenImHintergrund(ws, ctx, config, ts, spieltagNr), "PTM-Online-Verbinden");
        worker.start();
    }

    private static void verbindenImHintergrund(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, TurnierSystem ts, Integer spieltagNr) {
        List<OnlineTournamentDto> passende;
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            passende = client.listTournaments().stream()
                    .filter(t -> TurnierSystemOnlineTypMapping.passtZu(ts, t))
                    .toList();
        } catch (IOException e) {
            logger.error("PTM-Online: Turnierliste laden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        Optional<OnlineTournamentDto> auswahl = zeigeAuswahlDialog(ws, ctx, passende);
        if (auswahl.isEmpty()) {
            return; // Abgebrochen oder keine Turniere vorhanden
        }

        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            client.connect(auswahl.get().id);
        } catch (IOException e) {
            logger.error("PTM-Online: Turnier verbinden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        LoMainThread.post(ctx, () -> sheetsAnlegenUndErfolgZeigen(ws, ctx, ts, spieltagNr, auswahl.get()));
    }

    private static Optional<OnlineTournamentDto> zeigeAuswahlDialog(
            WorkingSpreadsheet ws, XComponentContext ctx, List<OnlineTournamentDto> passende) {
        ProcessBox pb = ProcessBox.from();
        boolean warSichtbar = pb.istSichtbar();
        if (warSichtbar) {
            pb.hide();
        }
        try {
            return PtmOnlineTurnierVerbindenDialog.zeigen(ctx, ws.getContainerWindowPeer(), passende);
        } catch (GenerateException e) {
            logger.error("PTM-Online-Verbinden-Dialog fehlgeschlagen", e);
            return Optional.empty();
        } finally {
            if (warSichtbar) {
                pb.visibleWennAutomatisch();
            }
        }
    }

    private static void sheetsAnlegenUndErfolgZeigen(WorkingSpreadsheet ws, XComponentContext ctx,
            TurnierSystem ts, Integer spieltagNr, OnlineTournamentDto turnier) {
        try {
            new PtmOnlineRegistrationMapping(ws, ts, spieltagNr).verbinden(turnier);
            MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("ptmonline.menu.toplevel"))
                    .message(I18n.get("ptmonline.erfolg.turnier_verbunden", turnier.name))
                    .show();
        } catch (GenerateException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("PTM-Online: Sheets fuer Verbindung anlegen fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
        }
    }

    public static void anmeldungenImportieren(WorkingSpreadsheet ws) {
        RegistrationImportTask.starte(ws);
    }

    public static void ergebnisseExportieren(WorkingSpreadsheet ws) {
        ResultExportTask.starte(ws);
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        String meldung = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        String key = meldung.contains(" 401") ? "ptmonline.fehler.nicht_freigeschaltet" : "ptmonline.fehler.netzwerk";
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(key.equals("ptmonline.fehler.netzwerk") ? I18n.get(key, meldung) : I18n.get(key))
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }
}
