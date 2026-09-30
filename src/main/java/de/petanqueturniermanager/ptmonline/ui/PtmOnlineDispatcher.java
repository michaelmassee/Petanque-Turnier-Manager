/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
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
import de.petanqueturniermanager.ptmonline.PtmOnlineFehlerText;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineZuordnungWiederherstellRunner;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;

/**
 * Bindeglied zwischen {@link de.petanqueturniermanager.comp.ProtocolHandler} und der PTM-Online-
 * REST-Anbindung. Muster analog {@link de.petanqueturniermanager.spielerdb.ui.SpielerDbDispatcher}:
 * statische Methoden, Fehler werden als {@link MessageBox} gemeldet statt zu crashen. Netzwerk-I/O
 * läuft off-thread (Dispatch-Aufrufkette darf nicht blockieren); Sheet-/Dokument-Schreibzugriffe laufen in
 * SheetRunnern, Meldungen aus reinen Hintergrund-Threads per {@link LoMainThread#post} auf dem Main-Thread.
 */
public final class PtmOnlineDispatcher {

    private static final Logger logger = LogManager.getLogger(PtmOnlineDispatcher.class);

    private PtmOnlineDispatcher() {}

    /**
     * Verbindet das Dokument (bzw. bei Supermelee den aktiven Spieltag) mit einem bereits
     * bestehenden PTM-Online-Turnier: laedt die zum API-Key gehoerenden, zum lokalen
     * {@link TurnierSystem} passenden Turniere, laesst den Nutzer eines auswaehlen und legt
     * anschliessend das Blatt "PTMOnline Sync" an.
     * <p>
     * Alles, was aus dem Dokument gebraucht wird (Turniersystem, Spieltag, bisherige Verbindung, Fenster für den
     * Dialog), wird hier vor dem Thread-Start gelesen; der Hintergrund-Thread lädt nur noch übers Netz und zeigt
     * den Dialog per {@code LoMainThread.post}.
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

        String eigeneTurnierId;
        try {
            eigeneTurnierId = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr).getTournamentId().orElse(null);
        } catch (GenerateException e) {
            logger.error("PTM-Online: bisherige Verbindung lesen fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
            return;
        }
        var auftrag = new VerbindenAuftrag(ws, config, ts, spieltagNr, MeleeAnmeldungKonfiguration.istAktiv(ws),
                eigeneTurnierId, ws.getContainerWindowPeer());

        Thread worker = new Thread(() -> verbindenImHintergrund(auftrag), "PTM-Online-Verbinden");
        worker.start();
        logger.info("PTM-Online: Hintergrund-Thread gestartet, turnierVerbinden() kehrt zurueck");
    }

    /** Vor dem Thread-Start aus dem Dokument gelesene Eingaben für {@link #verbindenImHintergrund}. */
    private record VerbindenAuftrag(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            TurnierSystem ts, Integer spieltagNr, boolean meleeAnmeldung, String eigeneTurnierId,
            XWindowPeer parentPeer) {

        XComponentContext ctx() {
            return ws.getxContext();
        }
    }

    private static void verbindenImHintergrund(VerbindenAuftrag auftrag) {
        XComponentContext ctx = auftrag.ctx();
        LibreOfficePtmOnlineSpeicher.Zugangsdaten config = auftrag.config();
        logger.info("PTM-Online: verbindenImHintergrund() gestartet (Thread={}), lade Turnierliste von {}",
                Thread.currentThread().getName(), config.baseUrl());
        List<OnlineTournamentDto> passende;
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            passende = client.listTournaments().stream()
                    .filter(OnlineTournamentDto::istVerbindbar)
                    .filter(t -> TurnierSystemOnlineTypMapping.passtZu(auftrag.ts(), auftrag.meleeAnmeldung(), t))
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
        Optional<OnlineTournamentDto> auswahl = zeigeAuswahlDialog(auftrag, passende);
        logger.info("PTM-Online: Auswahldialog beendet, Auswahl vorhanden={}", auswahl.isPresent());
        // Server-Bindung und Sync-Blatt in einem Runner: läuft schon einer, bleibt auch der Server unberührt.
        auswahl.ifPresent(turnier -> new PtmOnlineVerbindenRunner(auftrag.ws(), auftrag.ts(), auftrag.spieltagNr(),
                config, turnier).start());
    }

    /**
     * Läuft auf einem Hintergrund-Thread (siehe {@link #verbindenImHintergrund}), NICHT dem
     * LO-Main-Thread — die {@link ProcessBox} darf hier daher NICHT direkt angefasst werden
     * (setVisible/hide sind VCL-Aufrufe, siehe Threading-Regel in CLAUDE.md). Das eigentliche
     * Anzeigen des Dialogs marshalliert {@link PtmOnlineTurnierVerbindenDialog#zeigen} bereits
     * selbst per {@code LoMainThread.post} zurück auf den Main-Thread.
     */
    private static Optional<OnlineTournamentDto> zeigeAuswahlDialog(VerbindenAuftrag auftrag,
            List<OnlineTournamentDto> passende) {
        try {
            return PtmOnlineTurnierVerbindenDialog.zeigen(auftrag.ctx(), auftrag.parentPeer(), passende,
                    auftrag.eigeneTurnierId());
        } catch (GenerateException e) {
            logger.error("PTM-Online-Verbinden-Dialog fehlgeschlagen", e);
            return Optional.empty();
        }
    }

    /**
     * Trennt die Verbindung des Dokuments (bzw. bei Supermelee des aktiven Spieltags) zu seinem
     * PTM-Online-Turnier wieder: fragt beim Nutzer nach, hebt serverseitig die
     * Dokument-Verwaltung ({@code document_managed}) wieder auf und entfernt das Blatt
     * "PTMOnline Sync".
     */
    public static void verbindungTrennen(WorkingSpreadsheet ws) {
        logger.info("PTM-Online: verbindungTrennen() gestartet (Thread={})", Thread.currentThread().getName());
        XComponentContext ctx = ws.getxContext();
        var config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }

        TurnierSystem ts;
        Integer spieltagNr;
        Optional<String> tournamentId;
        try {
            ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            tournamentId = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr).getTournamentId();
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

        // Server-Trennung und Entfernen des Blatts in einem Runner: läuft schon einer, bleibt der Server unberührt.
        new PtmOnlineVerbindungsRunner(ws, ts, spieltagNr, PtmOnlineVerbindungsRunner.Aktion.TRENNEN).start();
    }

    /**
     * Hält den Sync der Verbindung an (bzw. setzt ihn fort) – ohne Server-Aufruf, jederzeit auch offline. Die
     * Verbindung selbst bleibt bestehen; solange pausiert, laufen weder Meldungsabgleich noch Rundenstart-Sync.
     */
    public static void syncPausieren(WorkingSpreadsheet ws) {
        starteVerbindungsAktion(ws, PtmOnlineVerbindungsRunner.Aktion.PAUSIEREN);
    }

    public static void syncFortsetzen(WorkingSpreadsheet ws) {
        starteVerbindungsAktion(ws, PtmOnlineVerbindungsRunner.Aktion.FORTSETZEN);
    }

    private static void starteVerbindungsAktion(WorkingSpreadsheet ws, PtmOnlineVerbindungsRunner.Aktion aktion) {
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            new PtmOnlineVerbindungsRunner(ws, ts, spieltagNr, aktion).start();
        } catch (GenerateException e) {
            logger.error("PTM-Online: {} fehlgeschlagen", aktion, e);
            zeigeFehler(ws.getxContext(), e.getMessage());
        }
    }

    public static void anmeldungenImportieren(WorkingSpreadsheet ws) {
        RegistrationImportTask.starte(ws);
    }

    /** Stellt nach beschädigtem Sync-Blatt die vom Server bestätigten Zuordnungen wieder her (T-21). */
    public static void zuordnungWiederherstellen(WorkingSpreadsheet ws) {
        XComponentContext ctx = ws.getxContext();
        LibreOfficePtmOnlineSpeicher.Zugangsdaten config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            new PtmOnlineZuordnungWiederherstellRunner(ws, ts, spieltagNr, config).start();
        } catch (GenerateException e) {
            logger.error("PTM-Online: Zuordnung wiederherstellen fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
        }
    }

    private static void zeigeNetzwerkFehler(XComponentContext ctx, IOException e) {
        zeigeFehler(ctx, PtmOnlineFehlerText.fuer(e));
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }
}
