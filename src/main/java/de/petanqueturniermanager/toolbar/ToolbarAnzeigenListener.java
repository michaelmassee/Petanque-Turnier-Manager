/**
 * Erstellung : 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.adapter.IGlobalEventListener;
import de.petanqueturniermanager.helper.Lo;

/**
 * Zeigt die PétTurnMngr-Symbolleiste automatisch an, sobald ein Calc-Dokument geöffnet oder neu erstellt wird.
 * <p>
 * LibreOffice zeigt Addon-Symbolleisten (definiert via {@code OfficeToolBar} in XCU) nicht automatisch an.
 * Dieser Listener und die statische Hilfsmethode stellen sicher, dass die Symbolleiste ohne manuelles
 * Aktivieren über Ansicht > Symbolleisten erscheint.
 * </p>
 * <p>
 * Zwei Pfade:
 * <ul>
 *   <li>{@link #zeigeToolbarInAllenFrames(XComponentContext)} – wird aus dem {@code ProtocolHandler}-Konstruktor
 *       aufgerufen und deckt das erste geöffnete Dokument ab (bevor der Listener noch registriert war).</li>
 *   <li>{@link #onLoad} / {@link #onNew} – deckt alle danach geöffneten bzw. neu erstellten Dokumente ab.</li>
 * </ul>
 * </p>
 */
public class ToolbarAnzeigenListener implements IGlobalEventListener {

    private static final Logger logger = LogManager.getLogger(ToolbarAnzeigenListener.class);

    // Addon-Toolbars erhalten von LibreOffice intern den Prefix "addon_".
    // Die korrekte Resource-URL für OfficeToolBar-Einträge aus XCU lautet daher:
    //   private:resource/toolbar/addon_<NodeName>
    // Die plain URL ohne "addon_" erzeugt eine leere Phantom-Toolbar ohne Buttons.
    static final String TOOLBAR_RESOURCE_URL = "private:resource/toolbar/addon_de.petanqueturniermanager.toolbar";

    /** Bereits beim Laden abgeschlossene Dokumente – verhindert Doppelaufruf falls onLoad + onNew beide feuern. */
    @Override
    public void onLoad(Object source) {
        zeigeSymbolleiste(source);
    }

    @Override
    public void onNew(Object source) {
        zeigeSymbolleiste(source);
    }

    // ---------------------------------------------------------------------------
    // Statische Hilfsmethode für den ProtocolHandler-Konstruktor

    /**
     * Zeigt die Symbolleiste in allen aktuell offenen Frames, die ein Calc-Dokument enthalten.
     * Wird aus dem {@code ProtocolHandler}-Konstruktor aufgerufen, um das erste Dokument abzudecken,
     * das geöffnet wurde bevor der globale Listener registriert war.
     */
    public static void zeigeToolbarInAllenFrames(XComponentContext xContext) {
        try {
            var xDesktop = DocumentHelper.getCurrentDesktop(xContext);
            if (xDesktop == null) {
                logger.debug("zeigeToolbarInAllenFrames: kein Desktop gefunden");
                return;
            }
            var xFramesSupplier = Lo.qi(XFramesSupplier.class, xDesktop);
            if (xFramesSupplier == null) {
                logger.debug("zeigeToolbarInAllenFrames: XFramesSupplier nicht verfügbar");
                return;
            }
            var xFrames = xFramesSupplier.getFrames();
            if (xFrames == null) {
                logger.debug("zeigeToolbarInAllenFrames: Frames-Container ist null");
                return;
            }
            int anzahl = xFrames.getCount();
            logger.debug("zeigeToolbarInAllenFrames: {} Frame(s) gefunden", anzahl);
            for (int i = 0; i < anzahl; i++) {
                try {
                    var xFrame = Lo.qi(XFrame.class, xFrames.getByIndex(i));
                    zeigeToolbarInFrame(xFrame);
                } catch (Exception e) {
                    logger.error("Fehler beim Anzeigen der Symbolleiste in Frame {}", i, e);
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Durchsuchen aller Frames", e);
        }
    }

    // ---------------------------------------------------------------------------

    private void zeigeSymbolleiste(Object source) {
        try {
            var xModel = Lo.qi(XModel.class, source);
            if (xModel == null) {
                return;
            }
            // nur Calc-Dokumente
            if (Lo.qi(XSpreadsheetDocument.class, xModel) == null) {
                return;
            }
            var xController = xModel.getCurrentController();
            if (xController == null) {
                return;
            }
            zeigeToolbarInFrame(xController.getFrame());
        } catch (Exception e) {
            logger.error("Fehler beim automatischen Anzeigen der Symbolleiste", e);
        }
    }

    private static void zeigeToolbarInFrame(XFrame xFrame) {
        if (xFrame == null) {
            return;
        }
        try {
            var xFrameProps = Lo.qi(XPropertySet.class, xFrame);
            if (xFrameProps == null) {
                return;
            }
            var xLayoutManager = Lo.qi(XLayoutManager.class, xFrameProps.getPropertyValue("LayoutManager"));
            if (xLayoutManager == null) {
                return;
            }
            // Addon-Toolbar (addon_* URL) via showElement + requestElement einblenden.
            // createElement ist NICHT nötig – LO verwaltet Addon-Toolbars intern via XCU.
            xLayoutManager.showElement(TOOLBAR_RESOURCE_URL);
            boolean result = xLayoutManager.requestElement(TOOLBAR_RESOURCE_URL);
            logger.debug("zeigeToolbarInFrame '{}': showElement+requestElement={} isVisible={}",
                    xFrame.getName(), result, xLayoutManager.isElementVisible(TOOLBAR_RESOURCE_URL));
        } catch (Exception e) {
            logger.error("Fehler beim Anzeigen der Symbolleiste in Frame '{}'",
                    xFrame.getName(), e);
        }
    }
}
