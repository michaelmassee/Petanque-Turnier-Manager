/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.ui;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindowPeer;
import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.system.SystemShellExecuteFlags;
import com.sun.star.system.XSystemShellExecute;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionSupplier;

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
import de.petanqueturniermanager.ptmonline.PtmOnlineAbschlussRunner;
import de.petanqueturniermanager.ptmonline.PtmOnlineErgebnismeldungenRunner;
import de.petanqueturniermanager.ptmonline.PtmOnlineFehlerText;
import de.petanqueturniermanager.ptmonline.PtmOnlineFormation;
import de.petanqueturniermanager.ptmonline.PtmOnlineHttpException;
import de.petanqueturniermanager.ptmonline.PtmOnlineRegistrationMapping;
import de.petanqueturniermanager.ptmonline.PtmOnlineZuordnungWiederherstellRunner;
import de.petanqueturniermanager.ptmonline.RegistrationImportTask;
import de.petanqueturniermanager.ptmonline.TournamentSyncClient;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

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
    /** Anzeige-Schlüssel und Live-Link: bei schlechtem Netz rasch eine Meldung statt langen Wartens. */
    private static final Duration TIMEOUT_ANZEIGE_SCHLUESSEL = Duration.ofSeconds(5);
    /** Anhang an den persönlichen Live-Link: PTM-Online zeigt dort dessen QR-Code. */
    static final String LIVE_QR_SEITE = "/qr";
    private static final int HTTP_NICHT_GEFUNDEN = 404;

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
        // Server-Bindung und Sync-Zustand in einem Runner: läuft schon einer, bleibt auch der Server unberührt.
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

    /** Stellt nach beschädigtem Sync-Zustand die vom Server bestätigten Zuordnungen wieder her (T-21). */
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

    /** Schließt das verbundene Online-Turnier nach Rückfrage ab, auf Wunsch mit Endrangliste und Benachrichtigung. */
    public static void turnierAbschliessen(WorkingSpreadsheet ws) {
        mitVerbindung(ws, (verbindung, config) -> new PtmOnlineAbschlussRunner(ws, verbindung.ts(),
                verbindung.spieltagNr()).start());
    }

    /** Öffnet das verbundene Online-Turnier (Detailseite mit Anmeldung) auf PTM-Online im Standardbrowser. */
    public static void imBrowserOeffnen(WorkingSpreadsheet ws) {
        mitVerbindung(ws, (verbindung, config) -> oeffneImBrowser(ws.getxContext(),
                turnierUrl(config.baseUrl(), "/turniere/", verbindung.tournamentId())));
    }

    /**
     * Öffnet die Live-Turnierseite des verbundenen Online-Turniers (Spielplan, Ergebnisse, Rangliste) im
     * Standardbrowser – dieselbe Seite, die Spieler und Zuschauer sehen. Der Link trägt den Anzeige-Schlüssel des
     * Turniers, damit die Seite auch auf einem nicht angemeldeten Bildschirm vor Ort Spielplan und Rangliste zeigt
     * (auch bei nicht veröffentlichter Meldeliste und bei online nicht durchführbaren Turniersystemen).
     */
    public static void turnierseiteOeffnen(WorkingSpreadsheet ws) {
        mitVerbindung(ws, (verbindung, config) -> {
            XComponentContext ctx = ws.getxContext();
            imHintergrund("PTM-Online-Turnierseite", ctx,
                    () -> oeffneTurnierseite(ctx, config, verbindung.tournamentId()));
        });
    }

    /** Hintergrund-Thread: holt den Anzeige-Schlüssel; Browser und Meldungen laufen auf dem Main-Thread. */
    private static void oeffneTurnierseite(XComponentContext ctx, LibreOfficePtmOnlineSpeicher.Zugangsdaten config,
            String tournamentId) {
        String schluessel;
        try {
            schluessel = TournamentSyncClient
                    .mitKurzemTimeout(config.baseUrl(), config.apiKey(), TIMEOUT_ANZEIGE_SCHLUESSEL)
                    .holeAnzeigeSchluessel(tournamentId);
        } catch (IOException e) {
            logger.error("PTM-Online: Anzeige-Schlüssel für Turnier {} nicht abgerufen", tournamentId, e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
            return;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("PTM-Online: Abruf des Anzeige-Schlüssels unterbrochen", e);
            return;
        }
        String url = turnierseitenUrl(config.baseUrl(), tournamentId, schluessel);
        LoMainThread.post(ctx, () -> oeffneImBrowser(ctx, url));
    }

    /**
     * Öffnet im Standardbrowser den QR-Code zum persönlichen Live-Link der Meldung, in deren Zeile der Cursor in der
     * Meldeliste (bei Supermelee in der Mêlée-Anmeldung) steht – zum Vorzeigen, etwa für Teams ohne E-Mail-Adresse.
     * Gibt es ihn nicht (Meldung nicht verbunden oder nicht bestätigt, Live-Ansicht aus), erscheint ein Hinweis.
     */
    public static void liveQrCodeOeffnen(WorkingSpreadsheet ws) {
        mitVerbindung(ws, (verbindung, config) -> {
            XComponentContext ctx = ws.getxContext();
            Optional<MeldelisteZiel> ziel = MeldelisteZielFactory.fuerPtmOnline(ws);
            OptionalInt zeile = ziel.isEmpty() ? OptionalInt.empty() : markierteMeldungsZeile(ws, ziel.get());
            if (ziel.isEmpty() || zeile.isEmpty()) {
                zeigeHinweis(ctx, I18n.get("ptmonline.liveqr.zeile_waehlen"));
                return;
            }
            Optional<String> onlineId;
            try {
                onlineId = onlineIdDerZeile(ziel.get().leseLokaleUuids(Set.of(zeile.getAsInt())),
                        zeile.getAsInt(), ziel.get().leseOnlineIdsProUuid());
            } catch (MeldelisteZiel.MeldelisteSchreibException e) {
                logger.error("PTM-Online: Meldung der markierten Zeile nicht lesbar", e);
                zeigeFehler(ctx, e.getMessage());
                return;
            }
            if (onlineId.isEmpty()) {
                zeigeHinweis(ctx, I18n.get("ptmonline.liveqr.nicht_verbunden"));
                return;
            }
            var abruf = new LiveLinkAbruf(ctx, TournamentSyncClient.mitKurzemTimeout(config.baseUrl(),
                    config.apiKey(), TIMEOUT_ANZEIGE_SCHLUESSEL), verbindung.tournamentId(), onlineId.get());
            imHintergrund("PTM-Online-Live-QR", ctx, () -> holeLiveLink(abruf, false));
        });
    }

    /**
     * 1-basierte Zeile der markierten Zelle; leer, wenn das aktive Blatt nicht das der Meldungen ist oder mehr als
     * eine Zeile markiert ist.
     */
    private static OptionalInt markierteMeldungsZeile(WorkingSpreadsheet ws, MeldelisteZiel ziel) {
        if (!ziel.liegtAufBlatt(ws.getWorkingSpreadsheetView().getActiveSheet())) {
            return OptionalInt.empty();
        }
        OptionalInt zeile = markierteZeile(ws);
        return zeile.isEmpty() ? zeile : OptionalInt.of(zeile.getAsInt() + 1);
    }

    /** Online-ID der Meldung in dieser Zeile; leer ohne lokale UUID oder ohne Online-Zuordnung. */
    static Optional<String> onlineIdDerZeile(Map<Integer, String> uuidProZeile, int zeile1Basiert,
            Map<String, String> onlineIdProUuid) {
        return Optional.ofNullable(uuidProZeile.get(zeile1Basiert)).map(onlineIdProUuid::get);
    }

    /** 0-basierte Zeile der markierten Zelle bzw. des markierten Bereichs, sofern er genau eine Zeile umfasst. */
    private static OptionalInt markierteZeile(WorkingSpreadsheet ws) {
        XSelectionSupplier auswahl = Lo.qi(XSelectionSupplier.class, ws.getWorkingSpreadsheetView());
        XCellRangeAddressable bereich = auswahl == null ? null
                : Lo.qi(XCellRangeAddressable.class, auswahl.getSelection());
        if (bereich == null) {
            return OptionalInt.empty();
        }
        CellRangeAddress adresse = bereich.getRangeAddress();
        return adresse.StartRow == adresse.EndRow ? OptionalInt.of(adresse.StartRow) : OptionalInt.empty();
    }

    /** Für den Live-Link nötige Angaben, im Dispatch aus dem Dokument gelesen. */
    private record LiveLinkAbruf(XComponentContext ctx, TournamentSyncClient client, String tournamentId,
            String onlineId) {}

    /**
     * Hintergrund-Thread: holt den persönlichen Live-Link und öffnet dessen QR-Code. Lässt er sich nicht
     * wiederherstellen, fragt der Main-Thread, ob er erneuert werden soll ({@link #frageErneuern}).
     */
    private static void holeLiveLink(LiveLinkAbruf abruf, boolean erneuern) {
        XComponentContext ctx = abruf.ctx();
        try {
            String url = abruf.client().holeLiveLink(abruf.tournamentId(), abruf.onlineId(), erneuern);
            LoMainThread.post(ctx, () -> oeffneImBrowser(ctx, url + LIVE_QR_SEITE));
        } catch (PtmOnlineHttpException e) {
            if (!erneuern && e.istLiveLinkNichtWiederherstellbar()) {
                logger.info("PTM-Online: Live-Link der Meldung {} nicht wiederherstellbar", abruf.onlineId(), e);
                LoMainThread.post(ctx, () -> frageErneuern(abruf));
                return;
            }
            logger.warn("PTM-Online: kein Live-Link für Meldung {} (HTTP {})", abruf.onlineId(), e.getStatusCode(),
                    e);
            LoMainThread.post(ctx, () -> keinLiveLinkHinweis(ctx, e));
        } catch (IOException e) {
            logger.error("PTM-Online: Live-Link für Meldung {} nicht abgerufen", abruf.onlineId(), e);
            LoMainThread.post(ctx, () -> zeigeNetzwerkFehler(ctx, e));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.debug("PTM-Online: Abruf des Live-Links unterbrochen", e);
        }
    }

    /** Main-Thread: Nach Bestätigung wird der Live-Link im Hintergrund erneuert. */
    private static void frageErneuern(LiveLinkAbruf abruf) {
        if (erneuernBestaetigt(abruf.ctx())) {
            imHintergrund("PTM-Online-Live-QR", abruf.ctx(), () -> holeLiveLink(abruf, true));
        }
    }

    /**
     * Startet einen Netzabruf außerhalb der Dispatch-Aufrufkette, damit Calc nicht bis zum Timeout blockiert.
     * Unerwartete Fehler werden protokolliert und auf dem Main-Thread gemeldet.
     */
    private static void imHintergrund(String name, XComponentContext ctx, Runnable aufgabe) {
        Thread thread = new Thread(() -> {
            try {
                aufgabe.run();
            } catch (RuntimeException e) {
                logger.error("PTM-Online: {} fehlgeschlagen", name, e);
                LoMainThread.post(ctx, () -> zeigeFehler(ctx, String.valueOf(e.getMessage())));
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
    }

    private static boolean erneuernBestaetigt(XComponentContext ctx) {
        return MessageBox.from(ctx, MessageBoxTypeEnum.WARN_YES_NO)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(I18n.get("ptmonline.liveqr.erneuern_frage"))
                .show() == MessageBoxResult.YES;
    }

    private static void keinLiveLinkHinweis(XComponentContext ctx, PtmOnlineHttpException e) {
        if (e.istLiveAnsichtAus()) {
            zeigeHinweis(ctx, I18n.get("ptmonline.liveqr.live_ansicht_aus"));
        } else if (e.istMeldungNichtBestaetigt()) {
            zeigeHinweis(ctx, I18n.get("ptmonline.liveqr.nicht_bestaetigt"));
        } else if (e.getStatusCode() == HTTP_NICHT_GEFUNDEN) {
            zeigeHinweis(ctx, I18n.get("ptmonline.liveqr.nicht_gefunden"));
        } else {
            zeigeNetzwerkFehler(ctx, e);
        }
    }

    /** {@code <Server><pfadPraefix><Turnier-Id>}; ein abschließender Schrägstrich der Server-Adresse entfällt. */
    static String turnierUrl(String baseUrl, String pfadPraefix, String tournamentId) {
        String basis = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        return basis + pfadPraefix + URLEncoder.encode(tournamentId, StandardCharsets.UTF_8);
    }

    /** Turnierseite mit Anzeige-Schlüssel ({@code ?share=}), wie der Anzeige-Link in PTM-Online. */
    static String turnierseitenUrl(String baseUrl, String tournamentId, String anzeigeSchluessel) {
        return turnierUrl(baseUrl, "/turnierseite/", tournamentId) + "?share="
                + URLEncoder.encode(anzeigeSchluessel, StandardCharsets.UTF_8);
    }

    private static void oeffneImBrowser(XComponentContext ctx, String url) {
        try {
            XSystemShellExecute shell = Lo.createInstanceMCF(XSystemShellExecute.class,
                    "com.sun.star.system.SystemShellExecute", ctx.getServiceManager(), ctx);
            if (shell == null) {
                throw new IllegalStateException("SystemShellExecute nicht verfügbar");
            }
            shell.execute(url, "", (short) SystemShellExecuteFlags.URIS_ONLY);
        } catch (com.sun.star.uno.Exception | RuntimeException e) {
            // Die URL enthält den Anzeige-Schlüssel bzw. den persönlichen Live-Link: nicht ins Log schreiben.
            logger.error("PTM-Online: Browser nicht geöffnet", e);
            zeigeFehler(ctx, I18n.get("ptmonline.browser.fehler", e.getMessage()));
        }
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

    private static void zeigeHinweis(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.INFO_OK)
                .caption(I18n.get("ptmonline.menu.toplevel"))
                .message(meldung)
                .show();
    }

    private static void zeigeFehler(XComponentContext ctx, String meldung) {
        MessageBox.from(ctx, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("ptmonline.fehler.titel"))
                .message(meldung)
                .show();
    }
}
