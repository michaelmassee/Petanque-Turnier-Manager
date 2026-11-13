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
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

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
import de.petanqueturniermanager.comp.PtmOnlineAktivierung;
import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.DocumentHelper;
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
import de.petanqueturniermanager.ptmonline.auftrag.versand.ErgebnismeldungsStand;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandStopp;
import de.petanqueturniermanager.ptmonline.dto.ErgebnismeldungenDto;
import de.petanqueturniermanager.ptmonline.dto.SyncStandDto;

/**
 * Überträgt Schreibaufträge an PTM-Online, sobald sich ein verbundenes Turnierdokument ändert: Verbundene Dokumente
 * erhalten einen {@link XModifyListener}; nach einer Änderung wird kurz gewartet (weitere Eingaben werden gebündelt) und dann ein
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
 * erneut gesendet – auch nach einem Neustart von LibreOffice, mit denselben Auftrags-IDs. Kennt PTM-Online schon
 * neuere Aufträge dieses Dokuments ({@code document_forked}, z.&nbsp;B. nach einem Neustart ohne Speichern), hält der
 * Versand an und die Turnierleitung wird einmal gefragt, ob der Stand dieses Dokuments gelten soll
 * ({@link PtmOnlineDokumentstand}). Hat ein anderes Dokument übernommen, hält der Versand an und die Turnierleitung
 * wird einmal informiert.
 */
public final class PtmOnlineLiveBeobachter implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(PtmOnlineLiveBeobachter.class);
    /** Wartezeit ab der ersten Änderung einer gebündelten Änderungswelle. */
    static final long VERZOEGERUNG_MS = 3000;
    /** Wartezeit nach einem Turnier-Kommando: dessen Änderungen sind vollständig. */
    static final long ANSTOSS_MS = 500;
    /** Obergrenze des Abstands zwischen zwei Versuchen nach Fehlern. */
    static final long MAX_WIEDERHOLUNG_MS = 60_000;
    /**
     * Abstand des Abrufs offener Ergebnismeldungen (E-27). Nur solange Spieler Ergebnisse melden dürfen und das
     * Turnier läuft; der Huckepack der Sync-Antworten löst ihn bei neuen Meldungen sofort aus.
     */
    static final long ERGEBNISMELDUNGEN_ABRUF_MS = 60_000;
    /**
     * Abstand des Abrufs, solange das Sofort-Signal ({@link PtmOnlineAenderungsKanal}) verbunden ist: nur noch
     * Sicherheitsnetz, neue Meldungen kündigt das Signal an.
     */
    static final long ERGEBNISMELDUNGEN_SICHERHEITSNETZ_MS = 300_000;
    /** Bündelt mehrere kurz aufeinander folgende Signale zu einem Abruf. */
    static final long SIGNAL_BUENDELUNG_MS = 1_000;

    private static volatile @Nullable PtmOnlineLiveBeobachter instanz;
    private static volatile @Nullable XComponentContext reaktivierungsKontext;

    private final XComponentContext xContext;
    private volatile boolean beendet;
    private final Map<String, Beobachtung> beobachtungen = new ConcurrentHashMap<>();
    private final ScheduledExecutorService ausfuehrung = new ScheduledThreadPoolExecutor(1, r -> {
        Thread thread = new Thread(r, "PTM-Online-Live");
        thread.setDaemon(true);
        return thread;
    }) {
        @Override
        protected void terminated() {
            // Auch beim regulären Shutdown erst freigeben, wenn alle Kind-Runner beendet sind.
            synchronized (PtmOnlineLiveBeobachter.class) {
                beobachtungen.clear();
                if (instanz == PtmOnlineLiveBeobachter.this) {
                    instanz = null;
                }
            }
            XComponentContext kontext = reaktivierungsKontext;
            if (kontext != null && PtmOnlineAktivierung.istAktiv()) {
                LoMainThread.post(kontext, () -> konfigurationGeaendert(kontext));
            }
        }
    };

    private PtmOnlineLiveBeobachter(XComponentContext xContext) {
        this.xContext = xContext;
    }

    /** Einmalig beim Start des Plugins; die Instanz wird als globaler Event-Listener registriert. */
    public static synchronized PtmOnlineLiveBeobachter init(XComponentContext xContext) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        logger.trace("PTM-Online Live init: angefragter Kontext={}, bestehender Kontext={}",
                System.identityHashCode(xContext), beobachter == null ? null : System.identityHashCode(beobachter.xContext));
        if (beobachter == null) {
            beobachter = new PtmOnlineLiveBeobachter(xContext);
            instanz = beobachter;
        } else if (beobachter.beendet) {
            throw new IllegalStateException("PTM-Online-Live-Beobachter wird noch beendet");
        }
        return beobachter;
    }

    /**
     * Regulärer Plugin-Shutdown: verhindert neue Arbeit und bricht geplante und laufende Arbeit ab. Wartet nicht
     * auf UNO-Kind-Runner: der Aufrufer kann der LibreOffice-Hauptthread sein, den diese Runner noch benötigen.
     * Der Executor gibt die statische Instanz nach dem Ende der Arbeit selbst frei. Keine UNO-Aufrufe beim Stoppen.
     */
    public static void dispose() {
        reaktivierungsKontext = null;
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.stoppen();
        }
    }

    /** Optionsänderung: Stop ohne UNO-Warten; Aktivierung übernimmt auch schon offene Dokumente. */
    public static void konfigurationGeaendert(XComponentContext context) {
        PtmOnlineLiveBeobachter bisher = instanz;
        if (!PtmOnlineAktivierung.istAktiv()) {
            reaktivierungsKontext = null;
            if (bisher != null) {
                PetanqueTurnierMngrSingleton.removeGlobalEventListener(bisher);
                var entfernen = java.util.List.copyOf(bisher.beobachtungen.values());
                bisher.stoppen();
                LoMainThread.post(context, () -> entfernen.forEach(b -> {
                    try {
                        b.geschlossen = true;
                        XModifyBroadcaster broadcaster = Lo.qi(XModifyBroadcaster.class, b.xDoc);
                        if (broadcaster != null) broadcaster.removeModifyListener(b);
                    } catch (DisposedException e) {
                        // Dokument wurde zwischenzeitlich geschlossen.
                    }
                }));
            }
            return;
        }
        boolean neu;
        synchronized (PtmOnlineLiveBeobachter.class) {
            bisher = instanz;
            if (bisher != null && bisher.beendet) {
                reaktivierungsKontext = context;
                return;
            }
            reaktivierungsKontext = null;
            neu = bisher == null;
            if (neu) bisher = init(context);
        }
        if (neu) PetanqueTurnierMngrSingleton.addGlobalEventListener(bisher);
        var desktop = DocumentHelper.getCurrentDesktop(context);
        if (desktop != null) {
            var dokumente = desktop.getComponents().createEnumeration();
            try {
                while (dokumente.hasMoreElements()) bisher.onLoad(dokumente.nextElement());
            } catch (com.sun.star.uno.Exception e) {
                logger.debug("Offene Dokumente für PTM-Online nicht vollständig erfasst", e);
            }
        }
    }

    private void stoppen() {
        // Sofort-Signale ohne UNO trennen, bevor die Ausführung (Ping, Neuverbinden) endet.
        beobachtungen.values().forEach(Beobachtung::aenderungsKanalSchliessen);
        synchronized (this) {
            beendet = true;
            for (Runnable aufgabe : ausfuehrung.shutdownNow()) {
                if (aufgabe instanceof Future<?> future) {
                    future.cancel(false);
                }
            }
        }
    }

    /** Nur für Tests: denselben Shutdown ausführen und vor dem Office-Wechsel auf dessen Abschluss warten. */
    public static void zuruecksetzenFuerTest() {
        reaktivierungsKontext = null;
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter == null) {
            return;
        }
        beobachter.stoppen();
        try {
            if (!beobachter.ausfuehrung.awaitTermination(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("PTM-Online-Hintergrundarbeit endet vor dem Office-Wechsel nicht");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Warten auf PTM-Online-Hintergrundarbeit unterbrochen", e);
        }
        logger.trace("PTM-Online Live: Test-Beobachter beendet und Kontext freigegeben");
    }

    /**
     * Durchlauf nach einem Turnier-Kommando anstoßen (wartet nicht). Beobachtet das Dokument dabei auch, falls es vor
     * dem Plugin-Start geladen oder erst in dieser Sitzung verbunden wurde.
     */
    public static void anstossen(XSpreadsheetDocument xDoc) {
        if (!PtmOnlineAktivierung.istAktiv()) return;
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.registriere(xDoc).ifPresent(Beobachtung::anstossen);
        }
    }

    /**
     * Holt die offenen Ergebnismeldungen sofort ab, statt auf den nächsten regelmäßigen Abruf zu warten (wartet nicht).
     * Braucht einen vorausgegangenen Durchlauf des Dokuments.
     */
    static void ergebnismeldungenAbholen(XSpreadsheetDocument xDoc) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.registriere(xDoc).ifPresent(beobachtung -> beobachtung.planeMeldungsAbruf(0));
        }
    }

    /** Nur für Tests: ob das Sofort-Signal von PTM-Online für dieses Dokument verbunden ist. */
    static boolean istSofortSignalVerbunden(XSpreadsheetDocument xDoc) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        Beobachtung beobachtung = beobachter == null ? null
                : beobachter.beobachtungen.get(UnoRuntime.generateOid(xDoc));
        PtmOnlineAenderungsKanal kanal = beobachtung == null ? null : beobachtung.aenderungsKanal.get();
        return kanal != null && kanal.istVerbunden();
    }

    /**
     * Ein Runner (Abgleich, „Gemeldete Ergebnisse abholen“) hat die Ergebnismeldungen selbst abgeholt und die
     * Konfliktliste nachgeführt. Der Hintergrund übernimmt diesen Stand, damit sein nächster Durchlauf nicht mit
     * einem älteren Abruf frisch eingetragene Fälle wieder entfernt.
     */
    static void ergebnismeldungenAbgeholt(XSpreadsheetDocument xDoc, String tournamentId,
            ErgebnismeldungenDto abruf) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter == null) {
            return;
        }
        beobachter.registriere(xDoc).ifPresent(beobachtung -> {
            beobachtung.ergebnismeldungen = new PtmOnlineErfassungsRunner.ErgebnismeldungsAbruf(tournamentId,
                    abruf.reports());
            if (abruf.istAktiv()) {
                beobachtung.planeMeldungsAbruf(beobachtung.meldungsAbrufAbstand());
            }
        });
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
        PtmOnlineSpieleinstellungen.vergessen(tournamentId);
        anstossen(xDoc);
    }

    /**
     * Neue Bindung (Verbinden, Übernehmen) oder nach Rückfrage übernommener Dokumentstand ({@link
     * PtmOnlineDokumentstand}): ein wegen Kopie oder Ablösung angehaltener Versand läuft wieder, der Live-Stand wird
     * vollständig neu erfasst.
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
        if (beendet || !PtmOnlineAktivierung.istAktiv()) {
            return;
        }
        XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, Lo.qi(XModel.class, source));
        if (xDoc != null && new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument() != TurnierSystem.KEIN) {
            // Gespeicherte, noch offene Aufträge nach dem Laden senden (P-55).
            registriere(xDoc).ifPresent(Beobachtung::anstossen);
        }
    }

    private Optional<Beobachtung> registriere(XSpreadsheetDocument xDoc) {
        if (beendet || !PtmOnlineAktivierung.istAktiv()) {
            return Optional.empty();
        }
        WorkingSpreadsheet ws = new WorkingSpreadsheet(xContext, xDoc);
        TurnierSystem ts = new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument();
        try {
            if (!PtmOnlineVerbindung.hatVerbindung(ws, ts)) {
                return Optional.empty();
            }
        } catch (GenerateException e) {
            logger.debug("PTM-Online Live: lokale Verbindung nicht lesbar, Beobachtung übersprungen", e);
            return Optional.empty();
        }
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
        private volatile boolean geschlossen;
        private final AtomicBoolean offen = new AtomicBoolean();
        private final AtomicBoolean eingeplant = new AtomicBoolean();
        /** Versand angehalten (Kopie im Einsatz, Bindung abgelöst) bis zu einer neuen Bindung. */
        private final AtomicBoolean angehalten = new AtomicBoolean();
        /** Offene Rückfrage nach {@code document_forked}, bis die Turnierleitung geantwortet hat. */
        private final AtomicReference<@Nullable DokumentstandFrage> dokumentstandFrage = new AtomicReference<>();
        /** Nur in SheetRunnern dieses Dokuments verwendet (nie gleichzeitig). */
        private final PtmOnlineCheckin checkin = new PtmOnlineCheckin();
        /** Letzter Abruf des Online-Zustands; nach neuer Bindung verworfen. */
        private volatile PtmOnlineErfassungsRunner.@Nullable OnlineStandAbruf onlineStand;
        /** Zuletzt abgeholte offene Ergebnismeldungen; nach neuer Bindung verworfen. */
        private volatile PtmOnlineErfassungsRunner.@Nullable ErgebnismeldungsAbruf ergebnismeldungen;
        /** Client und Turnier des letzten Durchlaufs, für den Abruf der Ergebnismeldungen. */
        private volatile @Nullable TournamentSyncClient letzterClient;
        private volatile @Nullable String letzteTurnierId;
        /** Geplanter Abruf der Ergebnismeldungen; nur unter der Sperre des Beobachters verändert. */
        private @Nullable ScheduledFuture<?> naechsterMeldungsAbruf;
        /** Sofort-Signal, solange Ergebnismeldungen abgeholt werden. */
        private final AtomicReference<@Nullable PtmOnlineAenderungsKanal> aenderungsKanal = new AtomicReference<>();
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
            geschlossen = true;
            aenderungsKanalSchliessen();
            beobachtungen.remove(oid);
            PtmOnlineAuftraege.vergessen(xDoc);
        }

        void anstossen() {
            offen.set(true);
            planeEin(ANSTOSS_MS);
        }

        void bindungErneuert() {
            // Neue Bindung = neues Lease: das Sofort-Signal verbindet beim nächsten Abruf neu.
            aenderungsKanalSchliessen();
            angehalten.set(false);
            onlineStand = null;
            ergebnismeldungen = null;
            String tournamentId = letzteTurnierId;
            if (tournamentId != null) {
                PtmOnlineErgebnismeldungen.vergessen(tournamentId);
            }
        }

        private void planeEin(long verzoegerungMs) {
            synchronized (PtmOnlineLiveBeobachter.this) {
                if (!beendet && !geschlossen && PtmOnlineAktivierung.istAktiv() && eingeplant.compareAndSet(false, true)) {
                    ausfuehrung.schedule(this::durchlauf, verzoegerungMs, TimeUnit.MILLISECONDS);
                }
            }
        }

        private void durchlauf() {
            eingeplant.set(false);
            if (beendet || geschlossen || !PtmOnlineAktivierung.istAktiv()) {
                return;
            }
            if (SheetRunner.isRunning()) {
                planeEin(VERZOEGERUNG_MS);
                return;
            }
            if (!offen.getAndSet(false)) {
                return;
            }
            try {
                TurnierSystem ts = new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument();
                WorkingSpreadsheet ws = new WorkingSpreadsheet(xContext, xDoc);
                if (!PtmOnlineVerbindung.hatVerbindung(ws, ts)) {
                    geschlossen = true;
                    aenderungsKanalSchliessen();
                    beobachtungen.remove(oid, this);
                    XModifyBroadcaster broadcaster = Lo.qi(XModifyBroadcaster.class, xDoc);
                    if (broadcaster != null) {
                        broadcaster.removeModifyListener(this);
                    }
                    return;
                }
                uebertrage(ws, ts);
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
            pruefeNichtBeendet();
            Optional<PtmOnlineErfassungsRunner.Erfassung> erfassung = erfasse(ws, ts);
            pruefeNichtBeendet();
            if (erfassung.isEmpty()) {
                return;
            }
            PtmOnlineErfassungsRunner.Erfassung e = erfassung.get();
            zeigeHinweise(e.anwendung());
            String tournamentId = e.verbindung().tournamentId();
            TournamentSyncClient client = e.verbindung().gebundenerClient();
            letzterClient = client;
            letzteTurnierId = tournamentId;
            if (e.brauchtStand()) {
                Optional<SyncStandDto> stand = holeOnlineStand(client, tournamentId);
                onlineStand = new PtmOnlineErfassungsRunner.OnlineStandAbruf(tournamentId, stand);
                if (stand.filter(s -> s.istRunning() && s.playerResultReportingEnabled()).isPresent()) {
                    planeMeldungsAbruf(0);
                }
                anstossen();
                return;
            }
            if (angehalten.get()) {
                return;
            }
            VersandStopp stopp = sende(e.bestand(), client, e.pausiert());
            pruefeNichtBeendet();
            if (!e.bestand().ergebnisseZumAnwenden().isEmpty()) {
                // Ergebnisse im nächsten Durchlauf im Dokument anwenden.
                anstossen();
            }
            reagiere(stopp, ws, ts, e.verbindung(), e.bestand());
        }

        /** Dokument-Seite im eigenen, stillen SheetRunner; der Hintergrund wartet darauf. */
        private Optional<PtmOnlineErfassungsRunner.Erfassung> erfasse(WorkingSpreadsheet ws, TurnierSystem ts)
                throws GenerateException, InterruptedException {
            PtmOnlineErfassungsRunner runner = new PtmOnlineErfassungsRunner(ws, ts, checkin, onlineStand,
                    ergebnismeldungen);
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
                return AuftragsVersand.sende(bestand.zuSenden(pausiert), client, ergebnis -> {
                    bestand.gesendet(ergebnis);
                    beachteHuckepack(ergebnis.antwort().ergebnismeldungen());
                });
            } finally {
                bestand.versandSperre().unlock();
            }
        }

        /**
         * Huckepack einer Sync-Antwort: Weicht die Zahl offener Meldungen vom zuletzt abgeholten Stand ab, wird sofort
         * abgeholt; bei eingeschalteter Meldung läuft sonst der regelmäßige Abruf. Ist die Meldung abgeschaltet und
         * stehen noch Meldungen in der Liste, räumt ein letzter Abruf sie weg.
         */
        private void beachteHuckepack(@Nullable ErgebnismeldungsStand stand) {
            if (stand == null) {
                return;
            }
            PtmOnlineErfassungsRunner.ErgebnismeldungsAbruf bekannt = ergebnismeldungen;
            int bekannteOffene = bekannt == null ? -1 : bekannt.meldungen().size();
            if (stand.aktiv()) {
                planeMeldungsAbruf(stand.offen() != bekannteOffene ? 0 : meldungsAbrufAbstand());
            } else if (bekannteOffene > 0) {
                planeMeldungsAbruf(0);
            }
        }

        /** Plant den Abruf der Ergebnismeldungen; ein früher geplanter Abruf bleibt, ein späterer wird vorgezogen. */
        private void planeMeldungsAbruf(long verzoegerungMs) {
            synchronized (PtmOnlineLiveBeobachter.this) {
                if (beendet || geschlossen || !PtmOnlineAktivierung.istAktiv()) {
                    return;
                }
                ScheduledFuture<?> geplant = naechsterMeldungsAbruf;
                if (geplant != null && !geplant.isDone()) {
                    if (geplant.getDelay(TimeUnit.MILLISECONDS) <= verzoegerungMs) {
                        return;
                    }
                    geplant.cancel(false);
                }
                naechsterMeldungsAbruf = ausfuehrung.schedule(this::holeErgebnismeldungen, verzoegerungMs,
                        TimeUnit.MILLISECONDS);
            }
        }

        /**
         * Holt die offenen Ergebnismeldungen (Thread „PTM-Online-Live“). Ein geänderter Stand stößt einen Durchlauf
         * an, der die Konfliktliste nachführt. Weiter abgeholt wird nur, solange die Meldung eingeschaltet ist und das
         * Turnier läuft – sonst erst wieder nach einem Huckepack.
         */
        private void holeErgebnismeldungen() {
            TournamentSyncClient client = letzterClient;
            String tournamentId = letzteTurnierId;
            if (beendet || geschlossen || client == null || tournamentId == null) {
                return;
            }
            ErgebnismeldungenDto abruf;
            try {
                abruf = client.fetchErgebnismeldungen(tournamentId);
            } catch (IOException e) {
                logger.info("PTM-Online: Ergebnismeldungen von {} nicht abrufbar, neuer Versuch in {} ms", tournamentId,
                        ERGEBNISMELDUNGEN_ABRUF_MS, e);
                planeMeldungsAbruf(ERGEBNISMELDUNGEN_ABRUF_MS);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("PTM-Online: Abruf der Ergebnismeldungen unterbrochen", e);
                return;
            }
            var neu = new PtmOnlineErfassungsRunner.ErgebnismeldungsAbruf(tournamentId, abruf.reports());
            if (!neu.equals(ergebnismeldungen)) {
                ergebnismeldungen = neu;
                anstossen();
            }
            if (abruf.istAktiv()) {
                aenderungsKanalSicherstellen(client, tournamentId);
                planeMeldungsAbruf(meldungsAbrufAbstand());
            } else {
                aenderungsKanalSchliessen();
            }
        }

        /** Mit verbundenem Sofort-Signal reicht ein seltener Abruf als Sicherheitsnetz. */
        long meldungsAbrufAbstand() {
            PtmOnlineAenderungsKanal kanal = aenderungsKanal.get();
            return kanal != null && kanal.istVerbunden() ? ERGEBNISMELDUNGEN_SICHERHEITSNETZ_MS
                    : ERGEBNISMELDUNGEN_ABRUF_MS;
        }

        /**
         * Öffnet das Sofort-Signal des Turniers, falls noch keins besteht. Ein aufgegebenes Signal (Zugang abgelehnt,
         * zu viele Verbindungen) bleibt bis zur nächsten Bindung zu, damit nicht bei jedem Abruf neu verbunden wird.
         */
        private void aenderungsKanalSicherstellen(TournamentSyncClient client, String tournamentId) {
            PtmOnlineAenderungsKanal bisher = aenderungsKanal.get();
            if ((bisher != null && bisher.gehoertZu(tournamentId)) || beendet || geschlossen) {
                return;
            }
            var neu = new PtmOnlineAenderungsKanal(tournamentId, ausfuehrung,
                    listener -> client.oeffneAenderungsSignal(tournamentId, listener),
                    () -> planeMeldungsAbruf(SIGNAL_BUENDELUNG_MS));
            if (aenderungsKanal.compareAndSet(bisher, neu)) {
                if (bisher != null) {
                    bisher.stoppen();
                }
                neu.starten();
            }
        }

        void aenderungsKanalSchliessen() {
            PtmOnlineAenderungsKanal kanal = aenderungsKanal.getAndSet(null);
            if (kanal != null) {
                kanal.stoppen();
            }
        }

        private void reagiere(VersandStopp stopp, WorkingSpreadsheet ws, TurnierSystem ts,
                PtmOnlineVerbindung verbindung, AuftragsBestand bestand) throws GenerateException, InterruptedException {
            switch (stopp) {
                case FERTIG -> naechsteWiederholungMs = VERZOEGERUNG_MS;
                case NETZ, SERVERFEHLER, NICHT_BERECHTIGT -> {
                    logger.info("PTM-Online: Versand angehalten ({}), neuer Versuch in {} ms", stopp,
                            naechsteWiederholungMs);
                    wiederholen();
                }
                case DOKUMENT_GEFORKT -> frageNachDokumentstand(
                        new DokumentstandFrage(bestand, verbindung.gebundenerClient(), verbindung.tournamentId()));
                case BINDUNG_ABGELOEST -> halteAn("ptmonline.fehler.bindung_abgeloest");
                case TURNIER_GELOESCHT -> verbindungAufheben(ws, ts, verbindung);
            }
        }

        /** Hält den Versand bis zu einer neuen Bindung an und informiert die Turnierleitung einmal. */
        private void halteAn(String meldungSchluessel) {
            aenderungsKanalSchliessen();
            if (angehalten.compareAndSet(false, true)) {
                LoMainThread.post(xContext, () -> {
                    if (!beendet) {
                        MessageBox.from(xContext, MessageBoxTypeEnum.WARN_OK)
                                .caption(I18n.get("ptmonline.menu.toplevel")).message(I18n.get(meldungSchluessel)).show();
                    }
                });
            }
        }

        /**
         * PTM-Online kennt neuere Aufträge dieses Dokuments: Versand anhalten und die Turnierleitung einmal fragen,
         * ob der Stand dieses Dokuments gelten soll. Die Rückfrage läuft auf dem Main-Thread.
         */
        private void frageNachDokumentstand(DokumentstandFrage frage) {
            aenderungsKanalSchliessen();
            if (angehalten.compareAndSet(false, true)) {
                dokumentstandFrage.set(frage);
                LoMainThread.post(xContext, this::stelleDokumentstandFrage);
            }
        }

        /** Main-Thread: Rückfrage; bei „Ja“ übernimmt der Hintergrund den Server-Zähler. */
        private void stelleDokumentstandFrage() {
            DokumentstandFrage frage = dokumentstandFrage.get();
            if (beendet || geschlossen || frage == null) {
                return;
            }
            if (!PtmOnlineDokumentstand.frageUebernahme(xContext)) {
                dokumentstandFrage.compareAndSet(frage, null);
                logger.info("PTM-Online: Stand des Dokuments soll nicht gelten, Versand bleibt angehalten");
                return;
            }
            planeDokumentstandUebernahme(0);
        }

        /** Hintergrund: Server-Zähler übernehmen und den vollständigen Stand des Dokuments neu erfassen. */
        private void uebernehmeDokumentstand() {
            DokumentstandFrage frage = dokumentstandFrage.getAndSet(null);
            if (frage == null || beendet || geschlossen) {
                return;
            }
            try {
                PtmOnlineDokumentstand.uebernehme(frage.bestand(), frage.client(), frage.tournamentId());
            } catch (IOException e) {
                // Die Antwort der Turnierleitung gilt weiter: nur der Abruf des Zählerstands wird wiederholt.
                logger.warn("PTM-Online: Zählerstand nicht abrufbar, neuer Versuch in {} ms", naechsteWiederholungMs,
                        e);
                dokumentstandFrage.set(frage);
                planeDokumentstandUebernahme(naechsteWiederholungMs);
                naechsteWiederholungMs = Math.min(naechsteWiederholungMs * 2, MAX_WIEDERHOLUNG_MS);
                return;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.debug("PTM-Online: Übernahme des Dokumentstands unterbrochen", e);
                return;
            }
            // Versand läuft wieder; die nächste Erfassung überträgt Runden, Rangliste und Einstellungen vollständig.
            bindungErneuert();
            PtmOnlineLiveSync.vergessen(frage.tournamentId());
            PtmOnlineSpieleinstellungen.vergessen(frage.tournamentId());
            anstossen();
        }

        private void planeDokumentstandUebernahme(long verzoegerungMs) {
            synchronized (PtmOnlineLiveBeobachter.this) {
                if (!beendet && !geschlossen) {
                    ausfuehrung.schedule(this::uebernehmeDokumentstand, verzoegerungMs, TimeUnit.MILLISECONDS);
                }
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
            aenderungsKanalSchliessen();
            PtmOnlineLiveSync.vergessen(verbindung.tournamentId());
            PtmOnlineSpieleinstellungen.vergessen(verbindung.tournamentId());
            fuehreAus(new PtmOnlineArchivRunner(ws, ts, verbindung.mapping()),
                    "PTM-Online: Blatt konnte nach dem Löschen des Online-Turniers nicht archiviert werden");
            LoMainThread.post(xContext, this::zeigeTurnierGeloescht);
        }

        private void zeigeTurnierGeloescht() {
            if (beendet) {
                return;
            }
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
        private void fuehreAus(SheetRunner runner, String fehlertext)
                throws GenerateException, InterruptedException {
            pruefeNichtBeendet();
            runner.startSilent();
            if (runner.getState() == Thread.State.NEW) {
                throw new LaufVerschoben("Lauf verschoben, ein anderer Lauf ist aktiv");
            }
            warteAufRunner(runner);
            if (runner.isLetzterLaufFehlgeschlagen()) {
                throw new GenerateException(fehlertext);
            }
        }

        private void wiederholen() {
            offen.set(true);
            planeEin(naechsteWiederholungMs);
            naechsteWiederholungMs = Math.min(naechsteWiederholungMs * 2, MAX_WIEDERHOLUNG_MS);
        }

        private void pruefeNichtBeendet() throws InterruptedException {
            if (beendet || !PtmOnlineAktivierung.istAktiv() || Thread.currentThread().isInterrupted()) {
                throw new InterruptedException("PTM-Online-Live-Beobachter wurde beendet");
            }
        }
    }

    /** Offene Rückfrage nach {@code document_forked}: was bei „Ja“ übernommen wird. */
    private record DokumentstandFrage(AuftragsBestand bestand, TournamentSyncClient client, String tournamentId) {}

    /** Ein Abbruch des wartenden Schedulers darf keinen weiterlaufenden UNO-Kind-Thread zurücklassen. */
    static void warteAufRunner(Thread runner) throws InterruptedException {
        try {
            runner.join();
        } catch (InterruptedException e) {
            runner.interrupt();
            boolean fertig = false;
            while (!fertig) {
                try {
                    runner.join();
                    fertig = true;
                } catch (InterruptedException erneut) {
                    runner.interrupt();
                }
            }
            Thread.currentThread().interrupt();
            throw e;
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
