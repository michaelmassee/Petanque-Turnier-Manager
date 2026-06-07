package de.petanqueturniermanager.helper.sheetsync;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XModel;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.container.XNamed;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;
import com.sun.star.view.XSelectionChangeListener;
import com.sun.star.view.XSelectionSupplier;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

/**
 * Schlanker {@link IGlobalEventListener}, der beim Wechsel auf ein Spielplan-
 * oder Spielrunden-Sheet einen leichten Formatierungs-Runner startet.
 * <p>
 * Im Gegensatz zum {@link SheetSyncListener} wird <em>kein</em> Hash-Vergleich
 * durchgeführt – der Runner feuert bei jedem Tab-Wechsel auf das Ziel-Sheet
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
public final class SpielplanFormatiererActivationListener implements IGlobalEventListener {

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

    /** Bereits registrierte Dokumente – verhindert Doppelregistrierung. */
    private final Set<XSpreadsheetDocument> registriert =
            Collections.newSetFromMap(new WeakHashMap<>());

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

    // ── Dokument-Laden ──────────────────────────────────────────────────────

    @Override
    public void onLoadFinished(Object source) {
        registriereListener(source);
    }

    @Override
    public void onNew(Object source) {
        registriereListener(source);
    }

    @Override
    public void onViewCreated(Object source) {
        registriereListener(source);
    }

    private void registriereListener(Object source) {
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) return;
            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xDoc == null) return;

            synchronized (registriert) {
                if (registriert.contains(xDoc)) return;
                registriert.add(xDoc);
            }

            registriereSelectionChangeListener(xModel, xDoc);

        } catch (RuntimeException e) {
            logger.error("Fehler beim Registrieren des Formatierer-Listeners", e);
        }
    }

    private void registriereSelectionChangeListener(XModel xModel, XSpreadsheetDocument xDoc) {
        var controller = xModel.getCurrentController();
        if (controller == null) return;
        if (Lo.qi(XSpreadsheetView.class, controller) == null) return;
        XSelectionSupplier selSupplier = Lo.qi(XSelectionSupplier.class, controller);
        if (selSupplier == null) return;

        String initialesSheetToken = aktivesSheetToken(xModel, xDoc);
        selSupplier.addSelectionChangeListener(new XSelectionChangeListener() {
            private String letztesSheetToken = initialesSheetToken;

            @Override
            public void selectionChanged(EventObject e) {
                try {
                    XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, e.Source);
                    if (view == null) return;

                    XSpreadsheet aktuellesSheet = view.getActiveSheet();
                    if (aktuellesSheet == null) return;

                    String aktuellesSheetToken = fokusToken(xDoc, aktuellesSheet);
                    if (aktuellesSheetToken.equals(letztesSheetToken)) {
                        return;
                    }
                    letztesSheetToken = aktuellesSheetToken;

                    if (!zielSheetMatch.test(xDoc, aktuellesSheet)) return;
                    if (SheetRunner.isRunning()) return;

                    planeFormatierer(xDoc, aktuellesSheet);

                } catch (RuntimeException ex) {
                    logger.error("Fehler im Formatierer-SelectionChangeListener", ex);
                }
            }

            @Override
            public void disposing(EventObject e) {
                // nichts zu tun
            }
        });
    }

    private static String aktivesSheetToken(XModel xModel, XSpreadsheetDocument xDoc) {
        XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
        if (view == null) return null;
        XSpreadsheet sheet = view.getActiveSheet();
        if (sheet == null) return null;
        return fokusToken(xDoc, sheet);
    }

    @Override
    public void onFocus(Object source) {
        try {
            XModel xModel = Lo.qi(XModel.class, source);
            if (xModel == null) return;
            XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
            if (xDoc == null) return;

            var controller = xModel.getCurrentController();
            XSpreadsheetView view = Lo.qi(XSpreadsheetView.class, controller);
            if (view == null) return;

            XSpreadsheet aktuellesSheet = view.getActiveSheet();
            if (aktuellesSheet == null) return;
            if (!zielSheetMatch.test(xDoc, aktuellesSheet)) return;
            if (SheetRunner.isRunning()) return;
            if (!merkeNeuenFokus(xDoc, aktuellesSheet)) {
                logger.trace("Formatierer-onFocus übersprungen: identischer Fokus erneut gemeldet");
                return;
            }

            planeFormatierer(xDoc, aktuellesSheet);

        } catch (RuntimeException e) {
            logger.error("Fehler beim Formatierer-onFocus", e);
        }
    }

    @Override
    public void onUnfocus(Object source) {
        letztesFokusToken = null;
    }

    @Override
    public void onUnload(Object source) {
        letztesFokusToken = null;
    }

    @Override
    public void onViewClosed(Object source) {
        letztesFokusToken = null;
    }

    boolean merkeNeuenFokus(XSpreadsheetDocument xDoc, XSpreadsheet xSheet) {
        String neuesToken = fokusToken(xDoc, xSheet);
        String altesToken = letztesFokusToken;
        if (neuesToken != null && neuesToken.equals(altesToken)) {
            return false;
        }
        letztesFokusToken = neuesToken;
        return true;
    }

    static String fokusToken(XSpreadsheetDocument xDoc, XSpreadsheet xSheet) {
        if (xDoc == null || xSheet == null) return null;
        String sheetName = "<unbekannt>";
        try {
            XNamed xNamed = Lo.qi(XNamed.class, xSheet);
            if (xNamed != null && xNamed.getName() != null) {
                sheetName = xNamed.getName();
            }
        } catch (RuntimeException e) {
            logger.trace("Sheet-Name für Fokus-Token nicht ermittelbar", e);
        }
        return System.identityHashCode(xDoc) + ":" + sheetName;
    }

    private void planeFormatierer(XSpreadsheetDocument xDoc, XSpreadsheet xSheet) {
        SheetSyncDebouncer.get().schedule(xDoc, debounceSchluessel, () -> {
            try {
                if (SheetRunner.isRunning()) return;
                if (!istDokumentLebendig(xDoc)) return;
                var ws = new WorkingSpreadsheet(xContext, xDoc);
                formatiererFactory.apply(ws, xSheet).startSilent();
            } catch (DisposedException e) {
                logger.debug("Formatierer übersprungen: Dokument bereits geschlossen", e);
            } catch (RuntimeException e) {
                logger.error("Formatierer konnte nicht gestartet werden", e);
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
