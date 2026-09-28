/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
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
import de.petanqueturniermanager.comp.DokumentKontext;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuelle;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;

/**
 * Überträgt den Live-Stand an PTM-Online, sobald sich ein Turnierdokument ändert: Jedes Turnierdokument erhält
 * einen {@link XModifyListener}; nach einer Änderung wird kurz gewartet (weitere Eingaben werden gebündelt) und
 * dann übertragen – nur, was sich geändert hat (siehe {@link LiveUebertragungsGedaechtnis}). Dazu gehört auch der
 * Check-in in der Meldeliste ({@link PtmOnlineCheckin}). Turnier-Kommandos stoßen die Übertragung über
 * {@link #anstossen} an ({@link PtmOnlineLiveAusloeser}).
 * <p>
 * Instabiles Netz: Übertragen wird ausschließlich auf dem eigenen Thread „PTM-Online-Live“ – Auslosen und
 * Ergebnis-Eingabe warten nie auf das Netz. Scheitert eine Übertragung, bleibt der Stand als offen markiert und
 * wird mit wachsendem Abstand ({@link #VERZOEGERUNG_MS} bis {@link #MAX_WIEDERHOLUNG_MS}) erneut versucht, bis sie
 * gelingt. Fehler werden nur protokolliert, damit ohne Netz nicht bei jeder Eingabe ein Dialog erscheint.
 * <p>
 * Threading: {@code modified()} läuft im UNO-Event-Thread und plant nur ein. Die Übertragung liest das Dokument
 * nur (keine UI, kein Schreibzugriff) und wartet, solange ein {@link SheetRunner} arbeitet. Nicht verbundene
 * Dokumente und pausierter Sync sind No-Ops – bei Pause wird nichts übertragen, beim Fortsetzen der komplette
 * Stand ({@link #nachPauseUebertragen}).
 */
public final class PtmOnlineLiveBeobachter implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(PtmOnlineLiveBeobachter.class);
    /** Wartezeit nach der letzten Änderung, bevor übertragen wird. */
    static final long VERZOEGERUNG_MS = 3000;
    /** Wartezeit nach einem Turnier-Kommando: dessen Änderungen sind vollständig. */
    static final long ANSTOSS_MS = 500;
    /** Obergrenze des Abstands zwischen zwei Versuchen nach Fehlern. */
    static final long MAX_WIEDERHOLUNG_MS = 60_000;

    private static volatile PtmOnlineLiveBeobachter instanz;

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
        if (instanz == null) {
            instanz = new PtmOnlineLiveBeobachter(xContext);
        }
        return instanz;
    }

    /**
     * Übertragung nach einem Turnier-Kommando anstoßen (aus dem SheetRunner; wartet nicht). Beobachtet das Dokument
     * dabei auch, falls es vor dem Plugin-Start geladen oder erst in dieser Sitzung verbunden wurde.
     */
    public static void anstossen(XSpreadsheetDocument xDoc) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        if (beobachter != null) {
            beobachter.registriere(xDoc).ifPresent(Beobachtung::anstossen);
        }
    }

    /**
     * Reiht den Status-Abgleich eines Rundenstarts zum Senden im Hintergrund ein; eine noch nicht gesendete ältere
     * Momentaufnahme wird ersetzt. Der Status wird vor der nächsten Live-Übertragung gesendet (Turnierstart und
     * Online-Zuordnung neuer Meldungen sind Voraussetzung für die Runden der Live-Ansicht).
     *
     * @return {@code false}, wenn kein Beobachter läuft (Plugin nicht gestartet) – dann muss der Aufrufer selbst
     *         senden
     */
    static boolean statusEinreihen(XSpreadsheetDocument xDoc, PtmOnlineStatusAuftrag auftrag) {
        PtmOnlineLiveBeobachter beobachter = instanz;
        Optional<Beobachtung> beobachtung = beobachter == null ? Optional.empty() : beobachter.registriere(xDoc);
        beobachtung.ifPresent(b -> b.statusEinreihen(auftrag));
        return beobachtung.isPresent();
    }

    /**
     * Nach „Sync fortsetzen“: während der Pause aufgelaufene Änderungen komplett übertragen, auch wenn der Stand
     * gegenüber der letzten Übertragung unverändert scheint.
     */
    public static void nachPauseUebertragen(XSpreadsheetDocument xDoc, String tournamentId) {
        PtmOnlineLiveSync.vergessen(tournamentId);
        anstossen(xDoc);
    }

    @Override
    public void onLoad(Object source) {
        XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, Lo.qi(XModel.class, source));
        if (xDoc != null && new DocumentPropertiesHelper(xDoc).getTurnierSystemAusDocument() != TurnierSystem.KEIN) {
            registriere(xDoc);
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
     * Fehlern Wiederholung mit wachsendem Abstand. Ein Durchlauf schreibt zuerst ein noch offenes Status-Ergebnis
     * zurück, sendet dann einen offenen Status-Abgleich und geänderte Check-ins und überträgt zuletzt den Live-Stand.
     */
    private final class Beobachtung implements XModifyListener {

        private final String oid;
        private final XSpreadsheetDocument xDoc;
        private final AtomicBoolean offen = new AtomicBoolean();
        private final AtomicBoolean eingeplant = new AtomicBoolean();
        private final AtomicReference<PtmOnlineStatusAuftrag> offenerStatus = new AtomicReference<>();
        /** Nur vom Thread „PTM-Online-Live“ verwendet. */
        private final PtmOnlineCheckin checkin = new PtmOnlineCheckin();
        /** Nur vom Thread „PTM-Online-Live“ verwendet. */
        private PtmOnlineStatusAbgleich.@Nullable Ergebnis offenesSchreiben;
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
        }

        void anstossen() {
            offen.set(true);
            planeEin(ANSTOSS_MS);
        }

        void statusEinreihen(PtmOnlineStatusAuftrag auftrag) {
            offenerStatus.accumulateAndGet(auftrag, (aelter, neu) -> neu.ersetzt(aelter));
            anstossen();
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
                    DokumentKontext.mitKontextWerfend(xDoc, () -> uebertrage(new WorkingSpreadsheet(xContext, xDoc), ts));
                }
                naechsteWiederholungMs = VERZOEGERUNG_MS;
            } catch (DisposedException e) {
                logger.debug("PTM-Online Live: Dokument {} geschlossen", oid, e);
                beobachtungen.remove(oid);
            } catch (SchreibenVerschoben e) {
                logger.debug("PTM-Online Live: Zurückschreiben verschoben, ein anderer Lauf ist aktiv", e);
                offen.set(true);
                planeEin(VERZOEGERUNG_MS);
            } catch (GenerateException | RuntimeException e) {
                logger.warn("PTM-Online Live: Übertragung fehlgeschlagen, neuer Versuch in {} ms",
                        naechsteWiederholungMs, e);
                wiederholen();
            }
        }

        private void uebertrage(WorkingSpreadsheet ws, TurnierSystem ts) throws GenerateException {
            Optional<PtmOnlineVerbindung> verbindung = PtmOnlineVerbindung.ermitteln(ws, ts);
            if (verbindung.isEmpty()) {
                offenerStatus.set(null);
                offenesSchreiben = null;
                return;
            }
            if (!PtmOnlineSpielrundeSync.istSyncAktiv(verbindung.get().mapping())) {
                // Pause: nichts senden; ein offener Status-Abgleich wartet auf „Sync fortsetzen“.
                return;
            }
            try {
                schreibeOffenesErgebnis(ws, ts, verbindung.get());
                sendeOffenenStatus(ws, ts, verbindung.get());
                sendeCheckinAenderungen(ws, ts, verbindung.get());
                Optional<LiveStandQuelle> quelle = LiveStandQuellen.fuer(ws, ts, verbindung.get().spieltagNr());
                if (quelle.isPresent()) {
                    PtmOnlineLiveSync.uebertragen(verbindung.get(), quelle.get());
                }
            } catch (IOException e) {
                throw new GenerateException(PtmOnlineFehlerText.fuer(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenerateException(e.getMessage());
            }
        }

        private void sendeOffenenStatus(WorkingSpreadsheet ws, TurnierSystem ts, PtmOnlineVerbindung verbindung)
                throws GenerateException, IOException, InterruptedException {
            PtmOnlineStatusAuftrag auftrag = offenerStatus.get();
            if (auftrag == null) {
                return;
            }
            PtmOnlineStatusAbgleich.Ergebnis ergebnis = PtmOnlineStatusAbgleich.senden(auftrag,
                    verbindung.mapping(), verbindung.gebundenerClient());
            offenerStatus.compareAndSet(auftrag, null);
            offenesSchreiben = ergebnis;
            checkin.uebernehmen(auftrag);
            if (!ergebnis.abgelehnt().isEmpty()) {
                PtmOnlineSpielrundeSync.zeigeFehlerSammlung(xContext,
                        List.of(RegistrationImportTask.onlineAbgelehntHinweis(ergebnis.abgelehnt())));
            }
            schreibeOffenesErgebnis(ws, ts, verbindung);
        }

        /**
         * Meldet geänderte Check-ins sofort (siehe {@link PtmOnlineCheckin}). Hat PTM-Online die Teilnahme
         * inzwischen selbst geändert (Revisionskonflikt), wird die Änderung verworfen statt endlos wiederholt – der
         * nächste Rundenstart gleicht ohnehin vollständig ab.
         */
        private void sendeCheckinAenderungen(WorkingSpreadsheet ws, TurnierSystem ts, PtmOnlineVerbindung verbindung)
                throws GenerateException, IOException, InterruptedException {
            Optional<PtmOnlineCheckin.Aenderung> aenderung = checkin.ermittle(verbindung.tournamentId(),
                    PtmOnlineCheckin.leseStand(verbindung));
            if (aenderung.isEmpty()) {
                return;
            }
            try {
                offenesSchreiben = PtmOnlineStatusAbgleich.senden(aenderung.get().auftrag(), verbindung.mapping(),
                        verbindung.gebundenerClient());
                logger.info("PTM-Online: {} geänderte Check-ins gemeldet",
                        aenderung.get().auftrag().eintraege().size());
            } catch (PtmOnlineHttpException e) {
                if (!e.istRevisionsKonflikt()) {
                    throw e;
                }
                logger.warn("PTM-Online: Check-in online zwischenzeitlich geändert, Meldung verworfen", e);
            }
            checkin.bestaetigen(aenderung.get());
            schreibeOffenesErgebnis(ws, ts, verbindung);
        }

        /** Schreibt im eigenen SheetRunner zurück; ist gerade ein anderer Lauf aktiv, wird verschoben. */
        private void schreibeOffenesErgebnis(WorkingSpreadsheet ws, TurnierSystem ts, PtmOnlineVerbindung verbindung)
                throws GenerateException, InterruptedException {
            PtmOnlineStatusAbgleich.Ergebnis ergebnis = offenesSchreiben;
            if (ergebnis == null) {
                return;
            }
            PtmOnlineStatusSchreibRunner runner = new PtmOnlineStatusSchreibRunner(ws, ts, verbindung.mapping(),
                    ergebnis);
            runner.startSilent();
            if (runner.getState() == Thread.State.NEW) {
                throw new SchreibenVerschoben();
            }
            runner.join();
            if (runner.isLetzterLaufFehlgeschlagen()) {
                throw new GenerateException("PTM-Online: Status-Ergebnis konnte nicht zurückgeschrieben werden");
            }
            offenesSchreiben = null;
        }

        private void wiederholen() {
            offen.set(true);
            planeEin(naechsteWiederholungMs);
            naechsteWiederholungMs = Math.min(naechsteWiederholungMs * 2, MAX_WIEDERHOLUNG_MS);
        }
    }

    /** Ein anderer SheetRunner belegt gerade das Dokument; das Zurückschreiben folgt im nächsten Durchlauf. */
    private static final class SchreibenVerschoben extends GenerateException {

        private static final long serialVersionUID = 1L;

        SchreibenVerschoben() {
            super("PTM-Online: Zurückschreiben verschoben");
        }
    }
}
