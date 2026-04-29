package de.petanqueturniermanager.helper.rangliste;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.util.XModifyBroadcaster;
import com.sun.star.util.XModifyListener;

import de.petanqueturniermanager.helper.Lo;

/**
 * Verfolgt pro Dokument ob Daten seit dem letzten Rangliste-Update geändert wurden.
 * <p>
 * Das Dirty-Flag startet als {@code true} für jedes neue Dokument, damit der erste
 * Tab-Wechsel zur Rangliste immer ein Update auslöst. Ein {@link XModifyListener} setzt
 * das Flag nach jeder Dokumentänderung wieder auf {@code true}.
 * <p>
 * Ausgelegt für den Einsatz in {@link RanglisteRefreshListener}: Ein Tracker wird pro
 * Turniersystem-Listener gehalten; mehrere offene Dokumente werden über eine
 * {@code WeakHashMap} unabhängig voneinander verwaltet (automatische GC bei Schließen).
 */
public class RanglisteDirtyFlagTracker {

    private static final Logger logger = LogManager.getLogger(RanglisteDirtyFlagTracker.class);

    private final Map<XSpreadsheetDocument, AtomicBoolean> flags =
            Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Registriert ein Dokument mit initial dirty=true.
     * Mehrfache Aufrufe für dasselbe Dokument haben keine Wirkung (putIfAbsent).
     */
    public void registriereDocument(XSpreadsheetDocument xDoc) {
        flags.putIfAbsent(xDoc, new AtomicBoolean(true));
    }

    /**
     * Registriert ein Dokument und hängt einen {@link XModifyListener} an den
     * {@link XModifyBroadcaster} des Dokuments, der das Dirty-Flag bei jeder
     * Dokumentänderung auf {@code true} setzt.
     * <p>
     * Wenn kein {@code XModifyBroadcaster} verfügbar ist, wird das Dokument trotzdem
     * registriert (dirty=true bleibt erhalten – kein Graceful-Degradation-Verlust).
     */
    public void registriereUndBeobachte(XSpreadsheetDocument xDoc) {
        registriereDocument(xDoc);

        XModifyBroadcaster broadcaster = Lo.qi(XModifyBroadcaster.class, xDoc);
        if (broadcaster == null) {
            logger.debug("XModifyBroadcaster nicht verfügbar – Dirty-Flag wird nicht auto-gesetzt");
            return;
        }

        broadcaster.addModifyListener(new XModifyListener() {
            @Override
            public void modified(EventObject e) {
                markiereDirty(xDoc);
            }

            @Override
            public void disposing(EventObject e) {
                entferneDocument(xDoc);
            }
        });

        logger.debug("XModifyListener für Dirty-Flag-Tracking registriert");
    }

    /**
     * Setzt das Dirty-Flag für das angegebene Dokument auf {@code true}.
     * Kein Fehler wenn das Dokument nicht registriert ist.
     */
    public void markiereDirty(XSpreadsheetDocument xDoc) {
        AtomicBoolean flag = flags.get(xDoc);
        if (flag != null) {
            flag.set(true);
        }
    }

    /**
     * Prüft ob das Dokument dirty ist und setzt das Flag atomar auf {@code false}.
     * <p>
     * Wenn das Dokument nicht registriert ist, wird {@code true} zurückgegeben
     * (Failsafe: unbekanntes Dokument = always-update).
     *
     * @return {@code true} wenn das Dokument dirty war (und jetzt clean ist);
     *         {@code false} wenn es bereits clean war
     */
    public boolean isDirtyUndConsume(XSpreadsheetDocument xDoc) {
        AtomicBoolean flag = flags.get(xDoc);
        if (flag == null) {
            logger.trace("isDirtyUndConsume: Dokument nicht registriert – Failsafe dirty=true");
            return true;
        }
        boolean war = flag.getAndSet(false);
        logger.trace("isDirtyUndConsume: wasDirty={}", war);
        return war;
    }

    /**
     * Entfernt das Tracking für das angegebene Dokument.
     * Wird automatisch bei {@code disposing()} des {@link XModifyListener} aufgerufen.
     */
    public void entferneDocument(XSpreadsheetDocument xDoc) {
        flags.remove(xDoc);
    }
}
