package de.petanqueturniermanager.helper.rangliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheetDocument;

/**
 * Debounce-Helper: bündelt Event-Stürme (mehrere {@code selectionChanged}-/{@code onFocus}-Ticks
 * in kurzer Folge) zu einem einzigen Hash-Check pro Dokument+Schlüssel.
 * <p>
 * Jeder neue {@link #schedule}-Aufruf cancelt einen ggf. anstehenden Future für denselben
 * Schlüssel und plant einen neuen. Tasks laufen auf einem dedizierten Daemon-Thread,
 * nicht im UI-Thread.
 * <p>
 * Für {@code TransientFehler} stellt {@link #scheduleRetry} eine eigene Methode mit
 * eigenem Backoff zur Verfügung – siehe {@link #BACKOFF_MS}.
 */
public final class RanglisteRefreshDebouncer {

    private static final Logger logger = LogManager.getLogger(RanglisteRefreshDebouncer.class);

    /** Standard-Debounce-Fenster für Selection/Focus-Trigger. */
    public static final long DEBOUNCE_MS = 150L;

    /** Linearer Backoff für Re-Schedule nach TransientFehler (Versuche 1..3). */
    public static final long[] BACKOFF_MS = {300L, 600L, 1200L};

    /** Maximaler Versuch (inkl.) – danach forceNextCheck statt weiteres Retry. */
    public static final int MAX_RETRY = BACKOFF_MS.length;

    private static final RanglisteRefreshDebouncer INSTANZ = new RanglisteRefreshDebouncer();

    public static RanglisteRefreshDebouncer get() {
        return INSTANZ;
    }

    private final ScheduledExecutorService executor;
    private final ConcurrentHashMap<String, ScheduledFuture<?>> ausstehend = new ConcurrentHashMap<>();

    private RanglisteRefreshDebouncer() {
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PTM-RanglisteRefreshDebouncer");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Plant einen Check für ({@code xDoc}, {@code schluessel}) nach {@link #DEBOUNCE_MS}.
     * Bei mehrfachem Aufruf innerhalb des Fensters wird der vorherige Future gecancelt.
     */
    public void schedule(XSpreadsheetDocument xDoc, String schluessel, Runnable check) {
        scheduleMitDelay(xDoc, schluessel, check, DEBOUNCE_MS);
    }

    /**
     * Plant einen Retry nach TransientFehler. {@code versuch} ist 1-basiert (1 = erster Retry).
     * Außerhalb von [1..{@link #MAX_RETRY}] wird ignoriert.
     */
    public void scheduleRetry(XSpreadsheetDocument xDoc, String schluessel, int versuch,
            Runnable check) {
        checkArgument(versuch >= 1, "versuch muss >= 1 sein");
        if (versuch > MAX_RETRY) {
            logger.debug("scheduleRetry ignoriert (versuch={} > MAX_RETRY={})", versuch, MAX_RETRY);
            return;
        }
        scheduleMitDelay(xDoc, schluessel, check, BACKOFF_MS[versuch - 1]);
    }

    private void scheduleMitDelay(XSpreadsheetDocument xDoc, String schluessel, Runnable check,
            long delayMs) {
        checkNotNull(xDoc);
        checkNotNull(schluessel);
        checkNotNull(check);
        String key = mapKey(xDoc, schluessel);
        ScheduledFuture<?> neu = executor.schedule(() -> ausfuehren(key, check), delayMs,
                TimeUnit.MILLISECONDS);
        ScheduledFuture<?> alt = ausstehend.put(key, neu);
        if (alt != null) {
            alt.cancel(false);
        }
    }

    private void ausfuehren(String key, Runnable check) {
        ausstehend.remove(key);
        try {
            check.run();
        } catch (RuntimeException e) {
            logger.warn("Rangliste-Refresh-Check warf Exception (key={})", key, e);
        }
    }

    /**
     * Cancelt alle ausstehenden Checks zu diesem Dokument. Wird beim Document-Dispose
     * vom Listener gerufen.
     */
    public void cancelAlle(XSpreadsheetDocument xDoc) {
        checkNotNull(xDoc);
        String prefix = docPrefix(xDoc);
        ausstehend.entrySet().removeIf(eintrag -> {
            if (eintrag.getKey().startsWith(prefix)) {
                eintrag.getValue().cancel(false);
                return true;
            }
            return false;
        });
    }

    private static String mapKey(XSpreadsheetDocument xDoc, String schluessel) {
        return docPrefix(xDoc) + schluessel;
    }

    private static String docPrefix(XSpreadsheetDocument xDoc) {
        return System.identityHashCode(xDoc) + ":";
    }
}
