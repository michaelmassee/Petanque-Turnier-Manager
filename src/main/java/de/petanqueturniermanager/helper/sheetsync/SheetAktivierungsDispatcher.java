package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.perflog.PerfLog;

/**
 * Zentraler Verteiler für Sheet-Aktivierungen. Registriert pro Dokument-Controller
 * <b>genau einen</b> {@link XSelectionChangeListener} und leitet jeden Wechsel an alle
 * angemeldeten {@link SheetAktivierungsHandler} weiter.
 * <p>
 * Motivation: Früher hängte jeder Handler (Ranglisten-, Teilnehmer-, Checkin-Sync,
 * Spielplan-Formatierer – in Summe dutzende) seinen eigenen UNO-Selection-Listener an
 * denselben Controller. Ein einziger Tab-Wechsel löste dadurch pro Handler einen
 * Cross-Bridge-Callback (auf jeweils frischem Bridge-Thread) aus. Der Dispatcher bündelt das:
 * <ul>
 *   <li>ein Cross-Bridge-Callback pro Tab-Wechsel statt einem je Handler,</li>
 *   <li>{@code view}/{@code getActiveSheet}/Token werden einmal statt N-mal ermittelt,</li>
 *   <li>die Token-Dedup greift verlässlich (früher fragiler {@code ==}-Proxy-Vergleich),</li>
 *   <li>die {@code SelectionChangeSuppression} (One-Shot) wird genau einmal konsumiert –
 *       früher verbrauchte sie der erste von vielen Handlern, die übrigen liefen weiter.</li>
 * </ul>
 * Threading: {@code selectionChanged} feuert auf einem UNO-Bridge-Thread. Die Handler dürfen
 * darin ausschließlich entprellt planen ({@link SheetSyncDebouncer}) und keine UNO-UI/VCL
 * mutieren (siehe CLAUDE.md).
 */
public final class SheetAktivierungsDispatcher implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(SheetAktivierungsDispatcher.class);

    private final List<SheetAktivierungsHandler> handlers = new CopyOnWriteArrayList<>();

    /** Bereits mit einem Selection-Listener versehene Dokumente – verhindert Doppelregistrierung. */
    private final Set<XSpreadsheetDocument> registriert =
            Collections.newSetFromMap(new WeakHashMap<>());

    /** Meldet einen Handler an. Reihenfolge = Aufrufreihenfolge. */
    public SheetAktivierungsDispatcher registriere(SheetAktivierungsHandler handler) {
        handlers.add(checkNotNull(handler));
        return this;
    }

    // ── Dokument-Lebenszyklus ───────────────────────────────────────────────

    @Override
    public void onLoadFinished(Object source) {
        registriereProDokument(source);
    }

    @Override
    public void onNew(Object source) {
        registriereProDokument(source);
    }

    @Override
    public void onViewCreated(Object source) {
        registriereProDokument(source);
    }

    @Override
    public void onUnfocus(Object source) {
        meldeFokusVerloren();
    }

    @Override
    public void onUnload(Object source) {
        meldeFokusVerloren();
    }

    @Override
    public void onViewClosed(Object source) {
        meldeFokusVerloren();
    }

    // ── Fokus ───────────────────────────────────────────────────────────────

    /**
     * Fenster-Fokus: Sheet einmal ermitteln und an alle Handler verteilen. Bewusst
     * <b>ohne</b> Token-Dedup – Handler wie der Sheet-Sync nutzen den Fokus für eine
     * periodische Safety-Revalidation und müssen auch beim Re-Fokus desselben Sheets feuern.
     */
    @Override
    public void onFocus(Object source) {
        long startNs = System.nanoTime();
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) {
                return;
            }
            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xDoc == null) {
                return;
            }
            XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
            if (view == null) {
                return;
            }
            XSpreadsheet sheet = view.getActiveSheet();
            if (sheet == null) {
                return;
            }
            if (SheetRunner.isRunning()) {
                return;
            }
            verteile(xDoc, sheet, SheetAktivierungsHandler.TRIGGER_FOCUS,
                    SheetFokusToken.traceId(xDoc, startNs));
        } catch (RuntimeException e) {
            logger.error("Fehler beim onFocus-Dispatch", e);
        }
    }

    private void meldeFokusVerloren() {
        for (SheetAktivierungsHandler handler : handlers) {
            try {
                handler.aufFokusVerloren();
            } catch (RuntimeException e) {
                logger.error("Handler {} warf bei aufFokusVerloren",
                        handler.getClass().getSimpleName(), e);
            }
        }
    }

    // ── Registrierung + Verteilung ──────────────────────────────────────────

    private void registriereProDokument(Object source) {
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) {
                return;
            }
            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xDoc == null) {
                return;
            }
            synchronized (registriert) {
                if (registriert.contains(xDoc)) {
                    return;
                }
                registriert.add(xDoc);
            }
            registriereSelectionListener(xModel, xDoc);
        } catch (RuntimeException e) {
            logger.error("Fehler beim Registrieren des Sheet-Aktivierungs-Dispatchers", e);
        }
    }

    private void registriereSelectionListener(XModel xModel, XSpreadsheetDocument xDoc) {
        var controller = xModel.getCurrentController();
        if (controller == null) {
            return;
        }
        if (Lo.qi(XSpreadsheetView.class, controller) == null) {
            return; // Druckvorschau/Sonstige – kein Tab-Wechsel-Kontext
        }
        XSelectionSupplier selSupplier = Lo.qi(XSelectionSupplier.class, controller);
        if (selSupplier == null) {
            return;
        }
        selSupplier.addSelectionChangeListener(
                new ZentralerSelectionListener(xDoc, aktivesSheetToken(xModel, xDoc)));
        logger.debug("Zentraler XSelectionChangeListener registriert");
    }

    private static String aktivesSheetToken(XModel xModel, XSpreadsheetDocument xDoc) {
        XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
        if (view == null) {
            return null;
        }
        XSpreadsheet sheet = view.getActiveSheet();
        if (sheet == null) {
            return null;
        }
        return SheetFokusToken.von(xDoc, sheet);
    }

    private void verteile(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String trigger,
            String traceId) {
        boolean perf = PerfLog.isEnabled();
        long startNs = System.nanoTime();
        for (SheetAktivierungsHandler handler : handlers) {
            try {
                handler.aufSheetAktiviert(xDoc, sheet, trigger, traceId);
            } catch (RuntimeException e) {
                logger.error("Handler {} warf beim Dispatch (trigger={})",
                        handler.getClass().getSimpleName(), trigger, e);
            }
        }
        if (perf) {
            PerfLog.log(logger,
                    "[TAB-TIMING] Dispatcher.verteile trace={} trigger={} sheet={} handler={} gesamt={} ms thread={}",
                    traceId, trigger, SheetFokusToken.sheetName(sheet), handlers.size(),
                    (System.nanoTime() - startNs) / 1_000_000L, Thread.currentThread().getName());
        }
    }

    /** Ein einziger Selection-Listener pro Controller, der an alle Handler verteilt. */
    private final class ZentralerSelectionListener implements XSelectionChangeListener {

        private final XSpreadsheetDocument xDoc;
        private String letztesToken;

        ZentralerSelectionListener(XSpreadsheetDocument xDoc, String initialToken) {
            this.xDoc = xDoc;
            this.letztesToken = initialToken;
        }

        @Override
        public void selectionChanged(EventObject e) {
            long startNs = System.nanoTime();
            try {
                XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, e.Source);
                if (view == null) {
                    return;
                }
                XSpreadsheet sheet = view.getActiveSheet();
                if (sheet == null) {
                    return;
                }
                // One-Shot immer zuerst: genau ein durch setActiveSheet() ausgelöstes Event
                // schlucken – und den Token nachziehen, damit der nächste echte Wechsel feuert.
                if (SheetRunner.consumeSelectionChangeSuppression()) {
                    letztesToken = SheetFokusToken.von(xDoc, sheet);
                    return;
                }
                String token = SheetFokusToken.von(xDoc, sheet);
                if (token != null && token.equals(letztesToken)) {
                    return; // gleiches Sheet – kein echter Wechsel
                }
                letztesToken = token;
                if (SheetRunner.isRunning()) {
                    return;
                }
                verteile(xDoc, sheet, SheetAktivierungsHandler.TRIGGER_SELECTION,
                        SheetFokusToken.traceId(xDoc, startNs));
            } catch (RuntimeException ex) {
                logger.error("Fehler im zentralen SelectionChangeListener", ex);
            }
        }

        @Override
        public void disposing(EventObject e) {
            SheetSyncDebouncer.get().cancelAlle(xDoc);
        }
    }
}
