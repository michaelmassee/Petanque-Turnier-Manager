/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.DisposedException;
import com.sun.star.lang.XComponent;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.LoMainThread;

/**
 * Debounced-Aktualisierung der PageStyles eines PTM-Turnier-Dokuments.
 *
 * <p>Wird von {@link de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty}
 * als Default-{@code nachSpeichernAktion} verdrahtet, sodass jedes Schreiben
 * einer Kopf-/Fußzeilen-Property einen Timer (200 ms) startet, der nach Ablauf
 * den PageStyle im Dokument-Stilkatalog aktualisiert. Schnelles Tippen erzeugt
 * dadurch nur eine PageStyle-Update-Operation pro Tipp-Burst.
 *
 * <p><b>Threading:</b> Der Scheduler verzögert nur. Die eigentliche UNO-Arbeit
 * wird per {@link LoMainThread#post} auf den LO-Main-Thread geschoben.
 *
 * <p><b>Mehrere Dokumente:</b> Pro {@link XSpreadsheetDocument} läuft ein eigener
 * Timer ({@link WeakHashMap}). Edits in zwei offenen PTM-Dokumenten beeinflussen
 * sich nicht gegenseitig.
 *
 * <p><b>Self-cleanup:</b> Nach Ausführung entfernt sich der geplante Auftrag aus der Map.
 * Geschlossene Dokumente werden zusätzlich vom GC freigegeben.
 *
 * <p><b>Disposed-Guard:</b> Zwischen Timer-Start und Ablauf (200 ms) kann das
 * Dokument geschlossen werden. Vor dem UNO-Call wird darum die Lebendigkeit
 * geprüft und im Zweifel still abgebrochen.
 */
public final class SeitenstileDebouncer {

    private static final Logger logger = LogManager.getLogger(SeitenstileDebouncer.class);

    private static final int DEBOUNCE_MS = 200;

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "PTM-SeitenstileDebouncer");
        thread.setDaemon(true);
        return thread;
    });

    private static final Map<XSpreadsheetDocument, ScheduledFuture<?>> FUTURE_PRO_DOKUMENT
            = Collections.synchronizedMap(new WeakHashMap<>());

    /** Lock für die eigentliche PageStyle-Aktualisierung – verhindert parallele Style-Library-Writes. */
    private static final Object UPDATE_LOCK = new Object();

    private SeitenstileDebouncer() {
    }

    /**
     * Plant eine debounced PageStyle-Aktualisierung für das Dokument hinter
     * {@code ws}. Bei mehreren Aufrufen innerhalb der Debounce-Frist läuft am
     * Ende nur eine einzige Aktualisierung.
     */
    public static void aktualisiereSeitenstileDebounced(WorkingSpreadsheet ws) {
        if (ws == null) {
            return;
        }
        XSpreadsheetDocument doc = ws.getWorkingSpreadsheetDocument();
        if (doc == null) {
            return;
        }

        synchronized (FUTURE_PRO_DOKUMENT) {
            ScheduledFuture<?> alt = FUTURE_PRO_DOKUMENT.remove(doc);
            if (alt != null) {
                alt.cancel(false);
            }

            AtomicReference<ScheduledFuture<?>> futureRef = new AtomicReference<>();
            ScheduledFuture<?> neu = SCHEDULER.schedule(
                    () -> posteSeitenstilAktualisierung(ws, doc, futureRef.get()),
                    DEBOUNCE_MS,
                    TimeUnit.MILLISECONDS);
            futureRef.set(neu);
            FUTURE_PRO_DOKUMENT.put(doc, neu);
        }
    }

    private static void posteSeitenstilAktualisierung(WorkingSpreadsheet ws, XSpreadsheetDocument doc,
            ScheduledFuture<?> future) {
        synchronized (FUTURE_PRO_DOKUMENT) {
            if (FUTURE_PRO_DOKUMENT.get(doc) == future) {
                FUTURE_PRO_DOKUMENT.remove(doc);
            }
        }
        LoMainThread.post(ws.getxContext(), () -> {
            if (istDisposed(doc)) {
                logger.debug("PageStyle-Update übersprungen – Dokument bereits disposed");
                return;
            }
            aktualisiereSeitenstileJetzt(ws);
        });
    }

    /**
     * Synchroner Pfad – schreibt den PageStyle ohne Debounce. Einsatz: Test
     * oder spezielle Trigger-Punkte, an denen sofortige Wirkung erwünscht ist.
     */
    public static void aktualisiereSeitenstileJetzt(WorkingSpreadsheet ws) {
        if (ws == null) {
            return;
        }
        synchronized (UPDATE_LOCK) {
            try {
                KonfigurationSheetRegistry.fuerAktivesDokument(ws)
                        .ifPresent(BaseKonfigurationSheet::seitenstileAktualisieren);
            } catch (RuntimeException e) {
                logger.warn("PageStyle-Aktualisierung fehlgeschlagen", e);
            }
        }
    }

    private static boolean istDisposed(XSpreadsheetDocument doc) {
        XComponent comp = UnoRuntime.queryInterface(XComponent.class, doc);
        if (comp == null) {
            return true;
        }
        try {
            // Probe-Call: bei disposedem Dokument wirft jede UNO-Methode DisposedException
            doc.getSheets();
            return false;
        } catch (DisposedException ignored) {
            return true;
        } catch (RuntimeException e) {
            logger.debug("Dokument-Lebendigkeit nicht prüfbar, behandle als disposed", e);
            return true;
        }
    }
}
