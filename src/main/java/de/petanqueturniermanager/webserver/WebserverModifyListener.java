package de.petanqueturniermanager.webserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.EventObject;
import com.sun.star.util.XModifyListener;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Lauscht auf Zelländerungen im Calc-Dokument und löst bei Änderungen einen SSE-Refresh aus.
 * <p>
 * Wird direkt auf dem {@code XModifyBroadcaster} des Spreadsheet-Dokuments registriert.
 * {@code modified()} wird im UNO-Event-Dispatch-Thread aufgerufen – UNO-Zugriffe sind dort sicher.
 * <p>
 * Eingebaut ist ein Debounce von {@value #DEBOUNCE_MS} ms: bei schnellen Eingabefolgen
 * (z.B. Tab von Zelle zu Zelle) wird der Timer bei jedem Event neu gestartet und erst nach
 * Ablauf der Stille-Periode ein einziger Update gesendet – immer mit dem aktuellen Zustand.
 */
public class WebserverModifyListener implements XModifyListener {

    private static final Logger logger = LogManager.getLogger(WebserverModifyListener.class);
    private static final long DEBOUNCE_MS = 600;

    private final ScheduledExecutorService debounceScheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                var t = new Thread(r, "WebserverDebounce");
                t.setDaemon(true);
                return t;
            });
    private final AtomicReference<ScheduledFuture<?>> pending = new AtomicReference<>();

    private volatile WorkingSpreadsheet ws;

    /** Setzt das aktive Spreadsheet, das bei Änderungen gerendert werden soll. */
    public void setWs(WorkingSpreadsheet ws) {
        this.ws = ws;
        if (ws == null) {
            abbrechen();
        }
    }

    @Override
    public void modified(EventObject source) {
        if (SheetRunner.isRunning()) {
            // SheetRunner ist aktiv – dessen Rendering hat Vorfahrt
            return;
        }
        pending.getAndUpdate(prev -> {
            if (prev != null && !prev.isDone()) {
                prev.cancel(false);
            }
            return debounceScheduler.schedule(() -> {
                try {
                    var current = ws;
                    if (current == null) {
                        return;
                    }
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    WebServerManager.get().sseRefreshSenden(current);
                } catch (Exception e) {
                    logger.debug("SSE-Refresh nach Zelländerung fehlgeschlagen: {}", e.getMessage());
                }
            }, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
        });
    }

    /**
     * Storniert einen evtl. noch ausstehenden Debounce-Task, ohne den Executor zu beenden.
     * Wird bei der Deregistrierung vom Dokument aufgerufen (via {@link #setWs(WorkingSpreadsheet)}).
     */
    public void abbrechen() {
        var task = pending.getAndSet(null);
        if (task != null) {
            task.cancel(false);
        }
    }

    /**
     * Storniert den ausstehenden Task und beendet den Debounce-Executor vollständig.
     * Wird beim Schließen des Dokuments ({@link #disposing(EventObject)}) aufgerufen.
     */
    public void herunterfahren() {
        abbrechen();
        debounceScheduler.shutdownNow();
    }

    @Override
    public void disposing(EventObject source) {
        ws = null;
        herunterfahren();
    }
}
