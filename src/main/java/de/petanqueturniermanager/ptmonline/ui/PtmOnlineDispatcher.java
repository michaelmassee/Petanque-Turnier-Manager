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
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.TurnierSystemOnlineTypMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask;
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
        logger.info("PTM-Online: turnierVerbinden() gestartet (Thread={})", Thread.currentThread().getName());
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            logger.info("PTM-Online: nicht konfiguriert, breche ab");
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
        logger.info("PTM-Online: Turniersystem={}, Spieltag={}", ts, spieltagNr);

        if (TurnierSystemOnlineTypMapping.onlineTyp(ts).isEmpty()) {
            logger.info("PTM-Online: Turniersystem {} nicht unterstuetzt, breche ab", ts);
            zeigeFehler(ctx, I18n.get("ptmonline.turnier.verbinden.dialog.fehler.system_nicht_unterstuetzt"));
            return;
        }

        Thread worker = new Thread(
                () -> verbindenImHintergrund(ws, ctx, config, ts, spieltagNr), "PTM-Online-Verbinden");
        worker.start();
        logger.info("PTM-Online: Hintergrund-Thread gestartet, turnierVerbinden() kehrt zurueck");
    }

    private static void verbindenImHintergrund(WorkingSpreadsheet ws, XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, TurnierSystem ts, Integer spieltagNr) {
        logger.info("PTM-Online: verbindenImHintergrund() gestartet (Thread={}), lade Turnierliste von {}",
                Thread.currentThread().getName(), config.baseUrl());
        List<OnlineTournamentDto> passende;
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            passende = client.listTournaments().stream()
                    .filter(t -> TurnierSystemOnlineTypMapping.passtZu(ts, t))
                    .toList();
            logger.info("PTM-Online: {} passende Turniere geladen", passende.size());
        } catch (IOException e) {
            logger.error("PTM-Online: Turnierliste laden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        logger.info("PTM-Online: zeige Auswahldialog");
        Optional<OnlineTournamentDto> auswahl = zeigeAuswahlDialog(ws, ctx, passende);
        logger.info("PTM-Online: Auswahldialog beendet, Auswahl vorhanden={}", auswahl.isPresent());
        if (auswahl.isEmpty()) {
            return; // Abgebrochen oder keine Turniere vorhanden
        }

        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            client.connect(auswahl.get().id);
            logger.info("PTM-Online: Turnier {} verbunden (Server-Aufruf ok)", auswahl.get().id);
        } catch (IOException e) {
            logger.error("PTM-Online: Turnier verbinden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        logger.info("PTM-Online: lege Sheets fuer Verbindung an");
        try {
            new PtmOnlineRegistrationMapping(ws, ts, spieltagNr).verbinden(auswahl.get());
            logger.info("PTM-Online: Sheets angelegt, zeige Erfolg");
            LoMainThread.post(ctx, () -> zeigeErfolg(ctx, auswahl.get()));
        } catch (GenerateException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.error("PTM-Online: Sheets fuer Verbindung anlegen fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeFehler(ctx, e.getMessage()));
        }
    }

    /**
     * Läuft auf einem Hintergrund-Thread (siehe {@link #verbindenImHintergrund}), NICHT dem
     * LO-Main-Thread — die {@link ProcessBox} darf hier daher NICHT direkt angefasst werden
     * (setVisible/hide sind VCL-Aufrufe, siehe Threading-Regel in CLAUDE.md). Das eigentliche
     * Anzeigen des Dialogs marshalliert {@link PtmOnlineTurnierVerbindenDialog#zeigen} bereits
     * selbst per {@code LoMainThread.post} zurück auf den Main-Thread.
     */
    private static Optional<OnlineTournamentDto> zeigeAuswahlDialog(
            WorkingSpreadsheet ws, XComponentContext ctx, List<OnlineTournamentDto> passende) {
        try {
            return PtmOnlineTurnierVerbindenDialog.zeigen(ctx, ws.getContainerWindowPeer(), passende);
        } catch (GenerateException e) {
            logger.error("PTM-Online-Verbinden-Dialog fehlgeschlagen", e);
            return Optional.empty();
        }
    }

    private static void zeigeErfolg(XComponentContext ctx, OnlineTournamentDto turnier) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.erfolg.turnier_verbunden", turnier.name))
                .show();
    }

    /**
     * Trennt die Verbindung des Dokuments (bzw. bei Supermelee des aktiven Spieltags) zu seinem
     * PTM-Online-Turnier wieder: fragt beim Nutzer nach, hebt serverseitig die
     * Dokument-Verwaltung ({@code document_managed}) wieder auf und entfernt die beiden Sheets
     * "Turnierinformationen"/"Meldungen".
     */
    public static void verbindungTrennen(WorkingSpreadsheet ws) {
        logger.info("PTM-Online: verbindungTrennen() gestartet (Thread={})", Thread.currentThread().getName());
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        PtmOnlineRegistrationMapping mapping;
        Optional<String> tournamentId;
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            mapping = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr);
            tournamentId = mapping.getTournamentId();
        } catch (GenerateException e) {
            logger.error("PTM-Online: Verbindungsstatus ermitteln fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        if (tournamentId.isEmpty()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_verbunden"));
            return;
        }

        MessageBoxResult antwort = MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.trennen.dialog.frage"))
                .show();
        if (antwort != MessageBoxResult.YES) {
            logger.info("PTM-Online: Trennen vom Nutzer abgelehnt");
            return;
        }

        PtmOnlineRegistrationMapping finaleMapping = mapping;
        String finaleTournamentId = tournamentId.get();
        Thread worker = new Thread(
                () -> trennenImHintergrund(ctx, config, finaleMapping, finaleTournamentId), "PTM-Online-Trennen");
        worker.start();
    }

    private static void trennenImHintergrund(XComponentContext ctx,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, PtmOnlineRegistrationMapping mapping, String tournamentId) {
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            client.disconnect(tournamentId);
            logger.info("PTM-Online: Turnier {} getrennt (Server-Aufruf ok)", tournamentId);
        } catch (IOException e) {
            logger.error("PTM-Online: Verbindung trennen (Server-Aufruf) fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        LoMainThread.post(ctx, () -> sheetsEntfernenUndErfolgZeigen(ctx, mapping));
    }

    private static void sheetsEntfernenUndErfolgZeigen(XComponentContext ctx, PtmOnlineRegistrationMapping mapping) {
        try {
            mapping.trennen();
            zeigeInfo(ctx, I18n.get("ptmonline.erfolg.verbindung_getrennt"));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Sheets nach Trennen entfernen fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
        }
    }

    public static void anmeldungenImportieren(WorkingSpreadsheet ws) {
        RegistrationImportTask.starte(ws);
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

    private static void zeigeInfo(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(meldung)
                .show();
    }
}
