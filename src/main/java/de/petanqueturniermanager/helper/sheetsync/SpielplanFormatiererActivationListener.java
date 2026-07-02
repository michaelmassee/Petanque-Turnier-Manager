package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.DisposedException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.perflog.PerfLog;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Schlanker {@link SheetAktivierungsHandler}, der beim Wechsel auf ein Spielplan-
 * oder Spielrunden-Sheet einen leichten Formatierungs-Runner startet.
 * <p>
 * Die Aktivierungs-Events kommen vom zentralen {@link SheetAktivierungsDispatcher};
 * dieser Handler registriert selbst <b>keinen</b> UNO-Selection-Listener mehr.
 * <p>
 * Im Gegensatz zum {@link SheetSyncListener} wird <em>kein</em> Hash-Vergleich
 * durchgeführt – der Runner feuert bei jedem Wechsel auf das Ziel-Sheet
 * (entprellt via {@link SheetSyncDebouncer}). Der Runner selbst ist billig:
 * er prüft zunächst per Spot-Check, ob CF fehlt, und schreibt nur dann.
 * <p>
 * Zwei Factory-Methoden für die häufigsten Matching-Strategien:
 * <ul>
 *   <li>{@link #fuerSchluessel}: exakter Metadaten-Schlüssel (einzelne Sheets)</li>
 *   <li>{@link #fuerPraefix}: Präfix-Matching (dynamische Schlüssel wie
 *       {@code __PTM_SCHWEIZER_SPIELRUNDE_N__})</li>
 * </ul>
 */
public final class SpielplanFormatiererActivationListener implements SheetAktivierungsHandler {

    private static final Logger logger = LogManager.getLogger(SpielplanFormatiererActivationListener.class);

    private final XComponentContext xContext;
    private final BiPredicate<XSpreadsheetDocument, XSpreadsheet> zielSheetMatch;
    private final BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> formatiererFactory;
    private final String debounceSchluessel;
    /**
     * LibreOffice feuert OnFocus teils mehrfach für dasselbe Dokument/Sheet, ohne dass
     * wirklich ein neuer Aktivierungswechsel stattgefunden hat. Der Token verhindert,
     * dass dadurch derselbe leichte Formatierer unnötig erneut läuft.
     */
    private volatile String letztesFokusToken;

    // ── Factory-Methoden ────────────────────────────────────────────────────

    /**
     * Exakter Metadaten-Schlüssel – für einzelne Sheets (z.B. Liga Spielplan).
     */
    public static SpielplanFormatiererActivationListener fuerSchluessel(
            XComponentContext xContext,
            String namedRangeKey,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> formatiererFactory) {
        return new SpielplanFormatiererActivationListener(xContext,
                (xDoc, sheet) -> SheetMetadataHelper.istRegistriertesSheet(xDoc, sheet, namedRangeKey),
                formatiererFactory,
                "FORMATIERER_" + namedRangeKey);
    }

    /**
     * Präfix-Matching – für Systeme mit dynamischen Schlüsseln
     * (z.B. {@code __PTM_SCHWEIZER_SPIELRUNDE_} für alle Spielrunden-Sheets).
     */
    public static SpielplanFormatiererActivationListener fuerPraefix(
            XComponentContext xContext,
            String schluesselPraefix,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> formatiererFactory) {
        return new SpielplanFormatiererActivationListener(xContext,
                (xDoc, sheet) -> SheetMetadataHelper.hatPraefixSchluessel(xDoc, sheet, schluesselPraefix),
                formatiererFactory,
                "FORMATIERER_PRAEFIX_" + schluesselPraefix);
    }

    SpielplanFormatiererActivationListener(
            XComponentContext xContext,
            BiPredicate<XSpreadsheetDocument, XSpreadsheet> zielSheetMatch,
            BiFunction<WorkingSpreadsheet, XSpreadsheet, SheetRunner> formatiererFactory,
            String debounceSchluessel) {
        this.xContext = checkNotNull(xContext);
        this.zielSheetMatch = checkNotNull(zielSheetMatch);
        this.formatiererFactory = checkNotNull(formatiererFactory);
        this.debounceSchluessel = checkNotNull(debounceSchluessel);
    }

    // ── Sheet-Aktivierung (vom Dispatcher) ──────────────────────────────────

    @Override
    public void aufSheetAktiviert(XSpreadsheetDocument xDoc, XSpreadsheet sheet, String trigger,
            String traceId) {
        boolean perf = PerfLog.isEnabled();
        long startNs = System.nanoTime();
        String ergebnis = "start";
        try {
            if (!zielSheetMatch.test(xDoc, sheet)) {
                ergebnis = "keinZielSheet";
                return;
            }
            // Fokus-Events kann LO mehrfach fürs gleiche Sheet melden – nur bei echtem
            // Fokuswechsel formatieren. Tab-Wechsel dedupt bereits der Dispatcher per Token.
            if (TRIGGER_FOCUS.equals(trigger) && !merkeNeuenFokus(xDoc, sheet)) {
                ergebnis = "gleicherFokus";
                return;
            }
            planeFormatierer(xDoc, sheet, trigger, traceId);
            ergebnis = "geplant";
        } catch (RuntimeException e) {
            ergebnis = "fehler";
            logger.error("Fehler im Formatierer-Handler (trigger={})", trigger, e);
        } finally {
            if (perf) {
                PerfLog.log(logger,
                        "[TAB-TIMING] Formatierer.aufSheetAktiviert trace={} trigger={} key={} sheet={} ergebnis={} gesamt={} ms thread={}",
                        traceId, trigger, debounceSchluessel, SheetFokusToken.sheetName(sheet),
                        ergebnis, (System.nanoTime() - startNs) / 1_000_000L,
                        Thread.currentThread().getName());
            }
        }
    }

    @Override
    public void aufFokusVerloren() {
        letztesFokusToken = null;
    }

    boolean merkeNeuenFokus(XSpreadsheetDocument xDoc, XSpreadsheet xSheet) {
        String neuesToken = SheetFokusToken.von(xDoc, xSheet);
        String altesToken = letztesFokusToken;
        if (neuesToken != null && neuesToken.equals(altesToken)) {
            return false;
        }
        letztesFokusToken = neuesToken;
        return true;
    }

    private void planeFormatierer(XSpreadsheetDocument xDoc, XSpreadsheet xSheet,
            String trigger, String traceId) {
        if (PerfLog.isEnabled()) {
            PerfLog.log(logger, "[TAB-TIMING] Formatierer.schedule trace={} trigger={} key={} sheet={} thread={}",
                    traceId, trigger, debounceSchluessel, SheetFokusToken.sheetName(xSheet), Thread.currentThread().getName());
        }
        SheetSyncDebouncer.get().schedule(xDoc, debounceSchluessel, () -> {
            long startNs = System.nanoTime();
            String ergebnis = "start";
            try {
                if (SheetRunner.isRunning()) {
                    ergebnis = "sheetRunnerLaeuft";
                    return;
                }
                if (!istDokumentLebendig(xDoc)) {
                    ergebnis = "dokumentNichtLebendig";
                    return;
                }
                var ws = new WorkingSpreadsheet(xContext, xDoc);
                formatiererFactory.apply(ws, xSheet).startSilent();
                ergebnis = "runnerGestartet";
            } catch (DisposedException e) {
                ergebnis = "disposed";
                logger.debug("Formatierer übersprungen: Dokument bereits geschlossen", e);
            } catch (RuntimeException e) {
                ergebnis = "fehler";
                logger.error("Formatierer konnte nicht gestartet werden", e);
            } finally {
                if (PerfLog.isEnabled()) {
                    PerfLog.log(logger, "[TAB-TIMING] Formatierer.worker trace={} trigger={} key={} sheet={} "
                            + "ergebnis={} gesamt={} ms thread={}",
                            traceId, trigger, debounceSchluessel, SheetFokusToken.sheetName(xSheet), ergebnis,
                            (System.nanoTime() - startNs) / 1_000_000L, Thread.currentThread().getName());
                }
            }
        });
    }

    private static boolean istDokumentLebendig(XSpreadsheetDocument xDoc) {
        if (xDoc == null) return false;
        try {
            xDoc.getSheets();
            return true;
        } catch (DisposedException e) {
            logger.debug("Formatierer übersprungen: Dokument bereits geschlossen", e);
            return false;
        } catch (RuntimeException e) {
            logger.debug("Formatierer übersprungen: Dokument nicht nutzbar", e);
            return false;
        }
    }
}
