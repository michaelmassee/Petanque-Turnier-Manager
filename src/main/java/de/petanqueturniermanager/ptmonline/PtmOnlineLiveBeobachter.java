/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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

/**
 * Überträgt den Live-Stand an PTM-Online, sobald sich ein Turnierdokument ändert: Jedes Turnierdokument erhält
 * einen {@link XModifyListener}; nach einer Änderung wird kurz gewartet (weitere Eingaben werden gebündelt) und
 * dann übertragen – nur, was sich geändert hat (siehe {@link LiveUebertragungsGedaechtnis}). Turnier-Kommandos
 * stoßen die Übertragung über {@link #anstossen} an ({@link PtmOnlineLiveAusloeser}).
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
     * Änderungs-Listener eines Dokuments mit „offen“-Merker: je Welle von Änderungen genau eine Übertragung,
     * nach Fehlern Wiederholung mit wachsendem Abstand.
     */
    private final class Beobachtung implements XModifyListener {

        private final String oid;
        private final XSpreadsheetDocument xDoc;
        private final AtomicBoolean offen = new AtomicBoolean();
        private final AtomicBoolean eingeplant = new AtomicBoolean();
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

        private void planeEin(long verzoegerungMs) {
            if (eingeplant.compareAndSet(false, true)) {
                ausfuehrung.schedule(this::uebertrage, verzoegerungMs, TimeUnit.MILLISECONDS);
            }
        }

        private void uebertrage() {
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
                    DokumentKontext.mitKontextWerfend(xDoc, () -> uebertrageStand(ts));
                }
                naechsteWiederholungMs = VERZOEGERUNG_MS;
            } catch (DisposedException e) {
                logger.debug("PTM-Online Live: Dokument {} geschlossen", oid, e);
                beobachtungen.remove(oid);
            } catch (GenerateException | RuntimeException e) {
                logger.warn("PTM-Online Live: Übertragung fehlgeschlagen, neuer Versuch in {} ms",
                        naechsteWiederholungMs, e);
                wiederholen();
            }
        }

        private void uebertrageStand(TurnierSystem ts) throws GenerateException {
            try {
                PtmOnlineLiveSync.uebertragen(new WorkingSpreadsheet(xContext, xDoc), ts);
            } catch (IOException e) {
                throw new GenerateException(PtmOnlineFehlerText.fuer(e));
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new GenerateException(e.getMessage());
            }
        }

        private void wiederholen() {
            offen.set(true);
            planeEin(naechsteWiederholungMs);
            naechsteWiederholungMs = Math.min(naechsteWiederholungMs * 2, MAX_WIEDERHOLUNG_MS);
        }
    }
}
