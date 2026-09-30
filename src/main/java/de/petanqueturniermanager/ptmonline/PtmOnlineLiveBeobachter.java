/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifyBroadcaster;
import com.sun.star.util.XModifyListener;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.AuftragsVersand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandStopp;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;

/**
 * Überträgt Schreibaufträge an PTM-Online, sobald sich ein Turnierdokument ändert: Jedes Turnierdokument erhält einen
 * {@link XModifyListener}; nach einer Änderung wird kurz gewartet (weitere Eingaben werden gebündelt) und dann ein
 * Durchlauf gestartet. Turnier-Kommandos stoßen ihn über {@link #anstossen} an.
 * <p>
 * Ein Durchlauf hat zwei Seiten (T-09):
 * <ol>
 * <li>Dokument-Seite: ein stiller {@link PtmOnlineErfassungsRunner} wendet die Ergebnisse gesendeter Aufträge an und
 * erfasst neue aus einem Snapshot (Check-in, Live-Stand, ausstehender Turnierstart). Die Aufträge werden mit dem
 * Dokument gespeichert, bevor sie gesendet werden.</li>
 * <li>Hintergrund (Thread „PTM-Online-Live“): sendet die offenen Aufträge in Zählerreihenfolge. Er liest und schreibt
 * kein Dokument; seine Ergebnisse wendet der nächste Durchlauf an.</li>
 * </ol>
 * Auslosen und Ergebnis-Eingabe warten nie auf das Netz. Scheitert der Versand am Netz oder Server, bleiben die
 * Aufträge offen und werden mit wachsendem Abstand ({@link #VERZOEGERUNG_MS} bis {@link #MAX_WIEDERHOLUNG_MS})
 * erneut gesendet – auch nach einem Neustart von LibreOffice, mit denselben Auftrags-IDs. Schreibt eine Kopie des
 * Dokuments parallel ({@code document_forked}) oder hat ein anderes Dokument übernommen, hält der Versand an und die
 * Turnierleitung wird einmal informiert.
 */
public final class PtmOnlineLiveBeobachter implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(PtmOnlineLiveBeobachter.class);
    /** Wartezeit nach der letzten Änderung, bevor übertragen wird. */
    static final long VERZOEGERUNG_MS = 3000;
    /** Wartezeit nach einem Turnier-Kommando: dessen Änderungen sind vollständig. */
    static final long ANSTOSS_MS = 500;
    /** Obergrenze des Abstands zwischen zwei Versuchen nach Fehlern. */
    static final long MAX_WIEDERHOLUNG_MS = 60_000;

    private static volatile @Nullable PtmOnlineLiveBeobachter instanz;

    private final XComponentContext xContext;
    private final Map<String, Beobachtung> beobachtungen = new ConcurrentHashMap<>();
    private final ScheduledExecutorService ausfuehrung = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "PTM-Online-Live");
        thread.setDaemon(true);
        return thread;
    });

    private PtmOnlineLiveBeobachter(XComponentContext xContext) {
        this.xContext = xContext;
    }

    /** Einmalig beim Start des Plugins; die Instanz wird als globaler Event-Listener registriert. */
    public static synchronized PtmOnlineLiveBeobachter init(XComponentContext xContext) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter == null) {
            beobachter = new PtmOnlineLiveBeobachter(xContext);
            instanz = beobachter;
        }
        return beobachter;
    }

    /**
     * Durchlauf nach einem Turnier-Kommando anstoßen (wartet nicht). Beobachtet das Dokument dabei auch, falls es vor
     * dem Plugin-Start geladen oder erst in dieser Sitzung verbunden wurde.
     */
    public static void anstossen(XSpreadsheetDocument xDoc) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.registriere(xDoc).ifPresent(Beobachtung::anstossen);
        }
    }

    /**
     * Ein Rundenstart hat den vollständigen Teilnahme-Stand als Auftrag erfasst; die Check-in-Erkennung übernimmt ihn
     * als bekannt. Läuft im SheetRunner des Rundenstarts.
     */
    static void teilnahmeErfasst(XSpreadsheetDocument xDoc, PtmOnlineStatusAuftrag status) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.registriere(xDoc).ifPresent(beobachtung -> beobachtung.checkin.uebernehmen(status));
        }
    }

    /**
     * Nach „Sync fortsetzen“: während der Pause aufgelaufene Änderungen komplett erfassen, auch wenn der Stand
     * gegenüber der letzten Erfassung unverändert scheint.
     */
    public static void nachPauseUebertragen(XSpreadsheetDocument xDoc, String tournamentId) {
        PtmOnlineLiveSync.vergessen(tournamentId);
        anstossen(xDoc);
    }

    /**
     * Neue Bindung (Verbinden, Übernehmen): ein wegen Kopie oder Ablösung angehaltener Versand läuft wieder, der
     * Live-Stand wird vollständig neu erfasst.
     */
    public static void bindungErneuert(XSpreadsheetDocument xDoc, String tournamentId) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.registriere(xDoc).ifPresent(Beobachtung::bindungErneuert);
        }
        nachPauseUebertragen(xDoc, tournamentId);
    }

    @Override
    public void onLoad(Object source) {
        XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, Lo.qi(XModel.class, source));
        if (xDoc != null && new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument() != TurnierSystem.KEIN) {
            // Gespeicherte, noch offene Aufträge nach dem Laden senden (P-55).
            registriere(xDoc).ifPresent(Beobachtung::anstossen);
        }
    }

    private Optional<Beobachtung> registriere(XSpreadsheetDocument xDoc) {
        XModifyBroadcaster broadcaster = Lo.qi(XModifyBroadcaster.class, xDoc);
        if (broadcaster == null) {
            return Optional.empty();
        }
        return Optional.of(beobachtungen.computeIfAbsent(UnoRuntime.generateOid(xDoc), oid -> {
            Beobachtung beobachtung = new Beobachtung(oid, xDoc);
            broadcaster.addModifyListener(beobachtung);
            logger.debug("PTM-Online Live: Dokument {} wird beobachtet", oid);
            return beobachtung;
        }));
    }

    /**
     * Änderungs-Listener eines Dokuments mit „offen“-Merker: je Welle von Änderungen genau ein Durchlauf, nach
     * Fehlern Wiederholung mit wachsendem Abstand.
     */
    private final class Beobachtung implements XModifyListener {

        private final String oid;
        private final XSpreadsheetDocument xDoc;
        private final AtomicBoolean offen = new AtomicBoolean();
        private final AtomicBoolean eingeplant = new AtomicBoolean();
        /** Versand angehalten (Kopie im Einsatz, Bindung abgelöst) bis zu einer neuen Bindung. */
        private final AtomicBoolean angehalten = new AtomicBoolean();
        /** Nur in SheetRunnern dieses Dokuments verwendet (nie gleichzeitig). */
        private final PtmOnlineCheckin checkin = new PtmOnlineCheckin();
        /** Letzter Abruf des Online-Zustands; nach neuer Bindung verworfen. */
        private volatile PtmOnlineErfassungsRunner.@Nullable OnlineStandAbruf onlineStand;
        /** Nur vom Thread „PTM-Online-Live“ verändert. */
        private long naechsteWiederholungMs = VERZOEGERUNG_MS;

        Beobachtung(String oid, XSpreadsheetDocument xDoc) {
            this.oid = oid;
            this.xDoc = xDoc;
        }

        @Override
        public void modified(EventObject event) {
            offen.set(true);
            planeEin(VERZOEGERUNG_MS);
        }

        @Override
        public void disposing(EventObject event) {
            beobachtungen.remove(oid);
            PtmOnlineAuftraege.vergessen(xDoc);
        }

        void anstossen() {
            offen.set(true);
            planeEin(ANSTOSS_MS);
        }

        void bindungErneuert() {
            angehalten.set(false);
            onlineStand = null;
        }

        private void planeEin(long verzoegerungMs) {
            if (eingeplant.compareAndSet(false, true)) {
                ausfuehrung.schedule(this::durchlauf, verzoegerungMs, TimeUnit.MILLISECONDS);
            }
        }

        private void durchlauf() {
            eingeplant.set(false);
            if (SheetRunner.isRunning()) {
                planeEin(VERZOEGERUNG_MS);
                return;
            }
            if (!offen.getAndSet(false)) {
                return;
            }
            try {
                TurnierSystem ts = new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument();
                if (ts != TurnierSystem.KEIN) {
                    uebertrage(new WorkingSpreadsheet(xContext, xDoc), ts);
                }
            } catch (DisposedException e) {
                logger.debug("PTM-Online Live: Dokument {} geschlossen", oid, e);
                beobachtungen.remove(oid);
            } catch (LaufVerschoben e) {
                logger.debug("{}", e.getMessage(), e);
                offen.set(true);
                planeEin(VERZOEGERUNG_MS);
            } catch (GenerateException | RuntimeException e) {
                logger.warn("PTM-Online Live: Durchlauf fehlgeschlagen, neuer Versuch in {} ms",
                        naechsteWiederholungMs, e);
                wiederholen();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("PTM-Online Live: Durchlauf unterbrochen", e);
            }
        }

        private void uebertrage(WorkingSpreadsheet ws, TurnierSystem ts) throws GenerateException, InterruptedException {
            Optional<PtmOnlineErfassungsRunner.Erfassung> erfassung = erfasse(ws, ts);
            if (erfassung.isEmpty()) {
                return;
            }
            PtmOnlineErfassungsRunner.Erfassung e = erfassung.get();
            zeigeHinweise(e.anwendung());
            String tournamentId = e.verbindung().tournamentId();
            TournamentSyncClient client = e.verbindung().gebundenerClient();
            if (e.brauchtStand()) {
                onlineStand = new PtmOnlineErfassungsRunner.OnlineStandAbruf(tournamentId,
                        holeOnlineStand(client, tournamentId));
                anstossen();
                return;
            }
            if (angehalten.get()) {
                return;
            }
            VersandStopp stopp = sende(e.bestand(), client, e.pausiert());
            if (!e.bestand().ergebnisseZumAnwenden().isEmpty()) {
                // Ergebnisse im nächsten Durchlauf im Dokument anwenden.
                anstossen();
            }
            reagiere(stopp, ws, ts, e.verbindung());
        }

        /** Dokument-Seite im eigenen, stillen SheetRunner; der Hintergrund wartet darauf. */
        private Optional<PtmOnlineErfassungsRunner.Erfassung> erfasse(WorkingSpreadsheet ws, TurnierSystem ts)
                throws GenerateException, InterruptedException {
            PtmOnlineErfassungsRunner runner = new PtmOnlineErfassungsRunner(ws, ts, checkin, onlineStand);
            fuehreAus(runner, "PTM-Online: Aufträge konnten nicht erfasst werden");
            return runner.erfassung();
        }

        private Optional<SyncStandDto> holeOnlineStand(TournamentSyncClient client, String tournamentId)
                throws InterruptedException {
            try {
                return Optional.of(client.fetchSyncStand(tournamentId));
            } catch (IOException e) {
                logger.info("PTM-Online: Online-Zustand von {} nicht abrufbar, erfasse ohne", tournamentId, e);
                return Optional.empty();
            }
        }

        private VersandStopp sende(AuftragsBestand bestand, TournamentSyncClient client, boolean pausiert)
                throws InterruptedException {
            bestand.versandSperre().lockInterruptibly();
            try {
                return AuftragsVersand.sende(bestand.zuSenden(pausiert), client, bestand::gesendet);
            } finally {
                bestand.versandSperre().unlock();
            }
        }

        private void reagiere(VersandStopp stopp, WorkingSpreadsheet ws, TurnierSystem ts,
                PtmOnlineVerbindung verbindung) throws GenerateException, InterruptedException {
            switch (stopp) {
                case FERTIG -> naechsteWiederholungMs = VERZOEGERUNG_MS;
                case NETZ, SERVERFEHLER, NICHT_BERECHTIGT -> {
                    logger.info("PTM-Online: Versand angehalten ({}), neuer Versuch in {} ms", stopp,
                            naechsteWiederholungMs);
                    wiederholen();
                }
                case DOKUMENT_GEFORKT -> halteAn("ptmonline.fehler.dokument_geforkt");
                case BINDUNG_ABGELOEST -> halteAn("ptmonline.fehler.bindung_abgeloest");
                case TURNIER_GELOESCHT -> verbindungAufheben(ws, ts, verbindung);
            }
        }

        /** Hält den Versand bis zu einer neuen Bindung an und informiert die Turnierleitung einmal. */
        private void halteAn(String meldungSchluessel) {
            if (angehalten.compareAndSet(false, true)) {
                LoMainThread.post(xContext, () -> MessageBox.from(xContext, MessageBoxTypeEnum.WARN_OK)
                        .caption(I18n.get("ptmonline.menu.toplevel")).message(I18n.get(meldungSchluessel)).show());
            }
        }

        /**
         * Das Online-Turnier wurde in PTM-Online gelöscht: statt endlos zu wiederholen, wird das Blatt „PTMOnline
         * Sync“ archiviert (gilt dann als nicht verbunden) und einmal darauf hingewiesen. Das Turnier läuft im
         * Dokument normal weiter.
         */
        private void verbindungAufheben(WorkingSpreadsheet ws, TurnierSystem ts, PtmOnlineVerbindung verbindung)
                throws GenerateException, InterruptedException {
            logger.warn("PTM-Online: Online-Turnier {} wurde gelöscht, Verbindung wird aufgehoben",
                    verbindung.tournamentId());
            PtmOnlineLiveSync.vergessen(verbindung.tournamentId());
            fuehreAus(new PtmOnlineArchivRunner(ws, ts, verbindung.mapping()),
                    "PTM-Online: Blatt konnte nach dem Löschen des Online-Turniers nicht archiviert werden");
            LoMainThread.post(xContext, this::zeigeTurnierGeloescht);
        }

        private void zeigeTurnierGeloescht() {
            MessageBox.from(xContext, MessageBoxTypeEnum.WARN_OK).caption(I18n.get("ptmonline.menu.toplevel"))
                    .message(I18n.get("ptmonline.turnier.geloescht")).show();
        }

        /** Meldet Anlagen, die PTM-Online abgelehnt hat (bereits angemeldet oder nach dem Turnierstart). */
        private void zeigeHinweise(PtmOnlineAuftraege.Anwendung anwendung) {
            List<String> hinweise = new ArrayList<>();
            if (!anwendung.abgelehnt().isEmpty()) {
                hinweise.add(RegistrationImportTask.onlineAbgelehntHinweis(anwendung.abgelehnt()));
            }
            if (!anwendung.nachStart().isEmpty()) {
                hinweise.add(I18n.get("ptmonline.hinweis.nach_start_lokal",
                        String.join(", ", anwendung.nachStart())));
            }
            if (!hinweise.isEmpty()) {
                PtmOnlineSpielrundeSync.zeigeFehlerSammlung(xContext, hinweise);
            }
        }

        /** Führt einen stillen Lauf aus und wartet darauf; ist gerade ein anderer Lauf aktiv, wird verschoben. */
        private static void fuehreAus(SheetRunner runner, String fehlertext)
                throws GenerateException, InterruptedException {
            runner.startSilent();
            if (runner.getState() == Thread.State.NEW) {
                throw new LaufVerschoben("Lauf verschoben, ein anderer Lauf ist aktiv");
            }
            runner.join();
            if (runner.isLetzterLaufFehlgeschlagen()) {
                throw new GenerateException(fehlertext);
            }
        }

        private void wiederholen() {
            offen.set(true);
            planeEin(naechsteWiederholungMs);
            naechsteWiederholungMs = Math.min(naechsteWiederholungMs * 2, MAX_WIEDERHOLUNG_MS);
        }
    }

    /** Ein anderer SheetRunner belegt gerade das Dokument; die Erfassung folgt im nächsten Durchlauf. */
    private static final class LaufVerschoben extends GenerateException {

        private static final long serialVersionUID = 1L;

        LaufVerschoben(String grund) {
            super("PTM-Online Live: " + grund);
        }
    }
}
