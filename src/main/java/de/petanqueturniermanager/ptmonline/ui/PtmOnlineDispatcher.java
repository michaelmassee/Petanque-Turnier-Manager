/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindowPeer;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.system.SystemShellExecuteFlags;
import com.sun.star.system.XSystemShellExecute;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.OnlineTurnierAnlage;
import de.petanqueturniermanager.onlinesync.OnlineVerbindbarkeit;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.TurnierSystemOnlineTypMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineErgebnismeldungenRunner;
import de.petanqueturniermanager.ptmonline.PtmOnlineFehlerText;
import de.petanqueturniermanager.ptmonline.PtmOnlineFormation;
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

    private static final Duration MAX_WARTEZEIT_VERARBEITUNG = Duration.ofSeconds(30);
    private static final long WARTEINTERVALL_MS = 200;

    private PtmOnlineDispatcher() {}

    /**
     * Verbindet das Dokument (bzw. bei Supermelee den aktiven Spieltag) mit einem bereits
     * bestehenden PTM-Online-Turnier: lädt alle zum API-Key gehörenden Turniere, bewertet sie gegen
     * Turniersystem, Anmeldeart und Formation des Dokuments ({@link OnlineVerbindbarkeit}), lässt den
     * Nutzer ein verbindbares auswählen und legt anschließend das Blatt "PTMOnline Sync" an.
     * <p>
     * Ein leeres Dokument (ohne Turniersystem) kann sich mit jedem freien Online-Turnier verbinden: Die Meldeliste
     * entsteht dann ohne Start-Dialog aus dem gewählten Turnier, danach werden die Online-Meldungen übernommen.
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

        TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
        Optional<VerbindenAuftrag> auftrag = ts == null || ts == TurnierSystem.KEIN
                ? Optional.of(auftragFuerLeeresDokument(ws, config))
                : auftragFuerTurnierDokument(ws, config, ts);
        auftrag.ifPresent(PtmOnlineDispatcher::starteVerbinden);
    }

    /**
     * Leeres Dokument: Jedes freie, nicht abgeschlossene Online-Turnier ist wählbar; Turniersystem, Formation und
     * Anmeldeart der Meldeliste kommen aus dem gewählten Turnier.
     */
    private static VerbindenAuftrag auftragFuerLeeresDokument(WorkingSpreadsheet ws,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config) {
        logger.info("PTM-Online: leeres Dokument, Meldeliste wird aus dem Online-Turnier angelegt");
        return new VerbindenAuftrag(ws, config, TurnierSystem.KEIN, null, OnlineVerbindbarkeit.fuerLeeresDokument(),
                null, ws.getContainerWindowPeer());
    }

    /** @return leer, wenn der Fehler schon gemeldet wurde */
    private static Optional<VerbindenAuftrag> auftragFuerTurnierDokument(WorkingSpreadsheet ws,
            LibreOfficePtmOnlineSpeicher.Zugangsdaten config, TurnierSystem ts) {
        XComponentContext ctx = ws.getxContext();
        if (TurnierSystemOnlineTypMapping.onlineTyp(ts).isEmpty()) {
            logger.info("PTM-Online: Turniersystem {} nicht unterstuetzt, breche ab", ts);
            zeigeFehler(ctx, I18n.get("ptmonline.turnier.verbinden.dialog.fehler.system_nicht_unterstuetzt"));
            return Optional.empty();
        }
        try {
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            logger.info("PTM-Online: Turniersystem={}, Spieltag={}", ts, spieltagNr);
            String eigeneTurnierId = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr).getTournamentId()
                    .orElse(null);
            var verbindbarkeit = OnlineVerbindbarkeit.fuer(ts, MeleeAnmeldungKonfiguration.istAktiv(ws),
                    PtmOnlineFormation.vergleichswertAusDokument(ws, ts));
            return Optional.of(new VerbindenAuftrag(ws, config, ts, spieltagNr, verbindbarkeit, eigeneTurnierId,
                    ws.getContainerWindowPeer()));
        } catch (GenerateException e) {
            logger.error("PTM-Online: Spieltag oder bisherige Verbindung ermitteln fehlgeschlagen", e);
            zeigeFehler(ctx, e.getMessage());
            return Optional.empty();
        }
    }

    private static void starteVerbinden(VerbindenAuftrag auftrag) {
        Thread worker = new Thread(() -> verbindenImHintergrund(auftrag), "PTM-Online-Verbinden");
        worker.start();
        logger.info("PTM-Online: Hintergrund-Thread gestartet, turnierVerbinden() kehrt zurueck");
    }

    /** Vor dem Thread-Start aus dem Dokument gelesene Eingaben für {@link #verbindenImHintergrund}. */
    private record VerbindenAuftrag(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            TurnierSystem ts, Integer spieltagNr, OnlineVerbindbarkeit verbindbarkeit, String eigeneTurnierId,
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
        List<OnlineVerbindbarkeit.Bewertung> bewertungen;
        try {
            TournamentSyncClient client = new TournamentSyncClient(config.baseUrl(), config.apiKey());
            bewertungen = auftrag.verbindbarkeit().bewerte(client.listTournaments());
            logger.info("PTM-Online: {} Turniere geladen, davon {} verbindbar", bewertungen.size(),
                    bewertungen.stream().filter(OnlineVerbindbarkeit.Bewertung::istVerbindbar).count());
        } catch (IOException e) {
            logger.error("PTM-Online: Turnierliste laden fehlgeschlagen", e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }

        logger.info("PTM-Online: zeige Auswahldialog");
        Optional<OnlineTournamentDto> auswahl = zeigeAuswahlDialog(auftrag, bewertungen);
        logger.info("PTM-Online: Auswahldialog beendet, Auswahl vorhanden={}", auswahl.isPresent());
        if (auswahl.isEmpty()) {
            return;
        }
        if (auftrag.verbindbarkeit().istLeeresDokument()) {
            verbindeLeeresDokument(auftrag.ws(), config, auswahl.get());
            return;
        }
        // Server-Bindung und Sync-Blatt in einem Runner: läuft schon einer, bleibt auch der Server unberührt.
        new PtmOnlineVerbindenRunner(auftrag.ws(), auftrag.ts(), auftrag.spieltagNr(), config, auswahl.get()).start();
    }

    /**
     * Meldeliste anlegen und verbinden in einem Runner; danach sofort die Online-Meldungen übernehmen wie
     * „Anmeldungen importieren“. Läuft auf dem Hintergrund-Thread, deshalb darf hier auf den Runner gewartet werden –
     * der Abgleich ist ein eigener Runner, nicht in den ersten verschachtelt.
     *
     * @return {@code true}, wenn das Dokument verbunden wurde (der Abgleich ist dann gestartet)
     */
    static boolean verbindeLeeresDokument(WorkingSpreadsheet ws, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            OnlineTournamentDto turnier) {
        Optional<OnlineTurnierAnlage> anlage = OnlineTurnierAnlage.aus(turnier);
        if (anlage.isEmpty()) {
            // Die Auswahl lässt nur abbildbare Turniere zu; das hier wäre ein Programmfehler.
            logger.error("PTM-Online: Turnier {} lässt sich nicht als Dokument anlegen", turnier.id);
            return false;
        }
        var runner = PtmOnlineVerbindenRunner.fuerLeeresDokument(ws, config, turnier, anlage.get());
        try {
            runner.start();
            runner.join();
            if (!runner.istVerbunden()) {
                return false;
            }
            // Der Beobachter kann nach der neuen Bindung kurz selbst laufen; der Abgleich würde sonst abgewiesen.
            warteAufFreieVerarbeitung();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("PTM-Online: Warten auf das Verbinden unterbrochen", e);
            return false;
        }
        logger.info("PTM-Online: leeres Dokument verbunden, übernehme die Online-Meldungen");
        RegistrationImportTask.starte(ws);
        return true;
    }

    private static void warteAufFreieVerarbeitung() throws InterruptedException {
        long frist = System.nanoTime() + MAX_WARTEZEIT_VERARBEITUNG.toNanos();
        while (SheetRunner.isRunning() && System.nanoTime() < frist) {
            Thread.sleep(WARTEINTERVALL_MS);
        }
    }

    /**
     * Läuft auf einem Hintergrund-Thread (siehe {@link #verbindenImHintergrund}), NICHT dem
     * LO-Main-Thread — die {@link ProcessBox} darf hier daher NICHT direkt angefasst werden
     * (setVisible/hide sind VCL-Aufrufe, siehe Threading-Regel in CLAUDE.md). Das eigentliche
     * Anzeigen des Dialogs marshalliert {@link PtmOnlineTurnierVerbindenDialog#zeigen} bereits
     * selbst per {@code LoMainThread.post} zurück auf den Main-Thread.
     */
    private static Optional<OnlineTournamentDto> zeigeAuswahlDialog(VerbindenAuftrag auftrag,
            List<OnlineVerbindbarkeit.Bewertung> bewertungen) {
        try {
            return PtmOnlineTurnierVerbindenDialog.zeigen(auftrag.ctx(), auftrag.parentPeer(), bewertungen,
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

    /** Springt zum Blatt „PTMOnline Konflikte“; gibt es noch keines, erklärt ein Hinweis, wann es entsteht. */
    public static void konfliktlisteOeffnen(WorkingSpreadsheet ws) {
        Optional<XSpreadsheet> konfliktListe = SheetMetadataHelper.findeSheet(ws.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.SCHLUESSEL_PTM_ONLINE_KONFLIKTE);
        if (konfliktListe.isEmpty()) {
            MessageBox.from(ws.getxContext(), MessageBoxTypeEnum.INFO_OK)
                    .caption(I18n.get("ptmonline.menu.toplevel"))
                    .message(I18n.get("ptmonline.konfliktliste.keine"))
                    .show();
            return;
        }
        new SheetHelper(ws).setActiveSheet(konfliktListe.get());
    }

    /** Holt die offenen Ergebnismeldungen von Spielern sofort ab (E-27). */
    public static void ergebnismeldungenAbholen(WorkingSpreadsheet ws) {
        mitVerbindung(ws, (verbindung, config) -> new PtmOnlineErgebnismeldungenRunner(ws, verbindung.ts(),
                verbindung.spieltagNr(), config, verbindung.tournamentId()).start());
    }

    /** Öffnet das verbundene Online-Turnier (Detailseite mit Anmeldung) auf PTM-Online im Standardbrowser. */
    public static void imBrowserOeffnen(WorkingSpreadsheet ws) {
        oeffneImBrowser(ws, "/turniere/");
    }

    /**
     * Öffnet die Live-Turnierseite des verbundenen Online-Turniers (Spielplan, Ergebnisse, Rangliste) im
     * Standardbrowser – dieselbe Seite, die Spieler und Zuschauer sehen.
     */
    public static void turnierseiteOeffnen(WorkingSpreadsheet ws) {
        oeffneImBrowser(ws, "/turnierseite/");
    }

    /** Öffnet {@code <Server><pfadPraefix><Turnier-Id>} im Standardbrowser. */
    private static void oeffneImBrowser(WorkingSpreadsheet ws, String pfadPraefix) {
        mitVerbindung(ws, (verbindung, config) -> {
            String basis = config.baseUrl().endsWith("/") ? config.baseUrl().substring(0, config.baseUrl().length() - 1)
                    : config.baseUrl();
            String url = basis + pfadPraefix + URLEncoder.encode(verbindung.tournamentId(), StandardCharsets.UTF_8);
            XComponentContext ctx = ws.getxContext();
            try {
                XSystemShellExecute shell = Lo.createInstanceMCF(XSystemShellExecute.class,
                        "com.sun.star.system.SystemShellExecute", ctx.getServiceManager(), ctx);
                if (shell == null) {
                    throw new IllegalStateException("SystemShellExecute nicht verfügbar");
                }
                shell.execute(url, "", (short) SystemShellExecuteFlags.URIS_ONLY);
            } catch (com.sun.star.uno.Exception | RuntimeException e) {
                logger.error("PTM-Online: Browser für {} nicht geöffnet", url, e);
                zeigeFehler(ctx, I18n.get("ptmonline.browser.fehler", e.getMessage()));
            }
        });
    }

    /** Verbindung des Dokuments (bzw. aktiven Spieltags) mit dem Online-Turnier. */
    private record Verbindung(TurnierSystem ts, Integer spieltagNr, String tournamentId) {}

    @FunctionalInterface
    private interface VerbindungsAktion {
        void ausfuehren(Verbindung verbindung, LibreOfficePtmOnlineSpeicher.Zugangsdaten config);
    }

    /** Führt die Aktion nur bei konfiguriertem Zugang und verbundenem Dokument aus, sonst ein Hinweis. */
    private static void mitVerbindung(WorkingSpreadsheet ws, VerbindungsAktion aktion) {
        XComponentContext ctx = ws.getxContext();
        LibreOfficePtmOnlineSpeicher.Zugangsdaten config = new LibreOfficePtmOnlineSpeicher(ctx).laden();
        if (!config.isConfigured()) {
            zeigeFehler(ctx, I18n.get("ptmonline.fehler.nicht_konfiguriert"));
            return;
        }
        try {
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            Integer spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, ts);
            Optional<String> tournamentId = new PtmOnlineRegistrationMapping(ws, ts, spieltagNr).getTournamentId();
            if (tournamentId.isEmpty()) {
                zeigeFehler(ctx, I18n.get("ptmonline.fehler.turnier_nicht_verbunden"));
                return;
            }
            aktion.ausfuehren(new Verbindung(ts, spieltagNr, tournamentId.get()), config);
        } catch (GenerateException e) {
            logger.error("PTM-Online: Verbindung ermitteln fehlgeschlagen", e);
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
