package de.petanqueturniermanager.webserver;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.EventObject;
import com.sun.star.util.XModifyListener;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Lauscht auf Zelländerungen im Calc-Dokument und löst bei Änderungen einen SSE-Refresh aus.
 * <p>
 * Wird auf dem {@code XModifyBroadcaster} des Spreadsheet-Dokuments registriert. {@code modified()}
 * läuft im UNO-Event-Thread.
 * <p>
 * Designprinzipien:
 * <ul>
 *   <li><strong>Dirty-Flag-Pattern, keine Cancel-Races:</strong> jedes {@code modified()} setzt
 *       unbedingt {@link #dirty}. Genau ein Refresh-Lauf ist zu jedem Zeitpunkt geplant
 *       ({@link #scheduled}).</li>
 *   <li><strong>Self-Rescheduling-Loop:</strong> der Refresh-Task arbeitet in einer {@code do/while}-
 *       Schleife, solange {@code dirty} zwischenzeitlich wieder gesetzt wurde – kein Timer-Loop,
 *       kein Event-Verlust, exakt ein Owner.</li>
 *   <li><strong>Externer Mark-Only-Pfad:</strong> {@link #markDirty()} setzt nur den Flag (ohne
 *       Schedule), {@link #markDirtyAndSchedule()} markiert und triggert. Externe Aufrufer
 *       manipulieren {@code dirty} niemals direkt.</li>
 * </ul>
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

    private final AtomicBoolean dirty = new AtomicBoolean(false);
    private final AtomicBoolean scheduled = new AtomicBoolean(false);
    /** Zeitpunkt, zu dem die aktuelle Dirty-Welle begann ({@code 0} wenn nicht dirty). */
    private volatile long dirtySeitMs;

    private volatile WorkingSpreadsheet ws;

    /** Setzt das aktive Spreadsheet, das bei Änderungen gerendert werden soll. */
    public void setWs(WorkingSpreadsheet ws) {
        this.ws = ws;
        if (ws == null) {
            dirty.set(false);
            dirtySeitMs = 0L;
        }
    }

    @Override
    public void modified(EventObject source) {
        markiereDirty();
        if (SheetRunner.isRunning()) {
            // Während eines laufenden SheetRunners nicht selbst schedulen –
            // der Runner ruft am Ende markDirtyAndSchedule() auf und übernimmt damit
            // die aufgelaufene Welle. Dirty bleibt gesetzt, kein Event verloren.
            return;
        }
        scheduleIfNeeded();
    }

    /**
     * Markiert nur als dirty, ohne einen Refresh-Lauf einzuplanen.
     * Vorgesehen für konkurrierende Aufrufer, die an einem Refresh-Lock abgeprallt sind
     * und dem aktuellen Owner signalisieren, dass eine weitere Welle nachgeholt werden muss.
     */
    public void markDirty() {
        markiereDirty();
    }

    /**
     * Markiert als dirty und plant einen Refresh-Lauf ein, falls nicht bereits einer pending ist.
     * Wird vom SheetRunner-Ende aufgerufen, um während des Runners eingetroffene
     * Modify-Events nicht zu verlieren.
     */
    public void markDirtyAndSchedule() {
        markiereDirty();
        scheduleIfNeeded();
    }

    /** Öffentlich für den Refresh-Lock-Owner, um nach Lock-Freigabe eine Folgewelle anzustoßen. */
    public void scheduleIfNeededExternal() {
        scheduleIfNeeded();
    }

    /** @return {@code true} wenn aktuell eine unverarbeitete Dirty-Welle aussteht. */
    public boolean isDirty() {
        return dirty.get();
    }

    /**
     * @return Millisekunden seit Beginn der aktuellen Dirty-Welle; {@code 0} wenn nicht dirty.
     *         Wird vom Watchdog zur Erkennung hängender Wellen verwendet.
     */
    public long dirtySinceMs() {
        long start = dirtySeitMs;
        return start == 0L ? 0L : Math.max(0L, System.currentTimeMillis() - start);
    }

    private void markiereDirty() {
        if (dirty.compareAndSet(false, true)) {
            dirtySeitMs = System.currentTimeMillis();
        }
    }

    private void scheduleIfNeeded() {
        if (!scheduled.compareAndSet(false, true)) {
            return;
        }
        debounceScheduler.schedule(this::refreshTask, DEBOUNCE_MS, TimeUnit.MILLISECONDS);
    }

    private void refreshTask() {
        try {
            do {
                dirty.set(false);
                dirtySeitMs = 0L;
                var current = ws;
                if (current == null) {
                    // ws kann zwischenzeitlich abgemeldet worden sein – Welle als verarbeitet markieren.
                    break;
                }
                try {
                    WebServerManager.get().sseRefreshSenden(current);
                } catch (RuntimeException e) {
                    logger.debug("SSE-Refresh fehlgeschlagen: {}", e.getMessage());
                }
                // Falls während refresh wieder dirty gesetzt wurde, nochmal durch.
            } while (dirty.get());
        } finally {
            scheduled.set(false);
            // Race-Fenster zwischen letztem dirty.get()==false und scheduled=false:
            // ein modified() könnte exakt hier ankommen und sehen, dass bereits ein Task
            // pending ist (scheduled==true). Nach dem Reset auf false muss daher nochmal
            // geprüft werden, sonst bleibt dirty stehen.
            if (dirty.get()) {
                scheduleIfNeeded();
            }
        }
    }

    /**
     * Storniert evtl. ausstehende Tasks ohne den Executor zu beenden.
     * Wird bei Deregistrierung vom Dokument aufgerufen.
     */
    public void abbrechen() {
        // Kein direktes Cancel – der Task läuft (falls in Ausführung) auf ws==null und beendet
        // sich selbst. Pending-Schedules laufen einmal leer durch, was billig ist.
        dirty.set(false);
        dirtySeitMs = 0L;
    }

    /** Storniert ausstehende Tasks und beendet den Debounce-Executor vollständig. */
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
