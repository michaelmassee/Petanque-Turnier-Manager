/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Steuert die Sichtbarkeit der Spieltag-Toolbar
 * ({@code private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.spieltag}).
 * <p>
 * Die Toolbar wird nur eingeblendet wenn das aktive Turniersystem mehrere Spieltage
 * unterstützt ({@link TurnierSystem#hatMehrereSpielTage()}), andernfalls wird sie
 * ausgeblendet.
 * </p>
 * <p>
 * {@link #aktualisiereInAllenFrames(XComponentContext)} wird aus
 * {@code ProtocolHandler.notifyAllListeners()} und dem Konstruktor aufgerufen.
 * </p>
 */
public final class SpieltagToolbarSteuerung {

    private static final Logger logger = LogManager.getLogger(SpieltagToolbarSteuerung.class);

    // Addon-Toolbars erhalten von LibreOffice intern den Prefix "addon_".
    static final String SPIELTAG_TOOLBAR_URL =
            "private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.spieltag";

    private SpieltagToolbarSteuerung() {
    }

    /**
     * Zeigt oder versteckt die Spieltag-Toolbar in allen aktuell offenen Frames
     * abhängig vom aktiven Turniersystem.
     */
    public static void aktualisiereInAllenFrames(XComponentContext xContext) {
        try {
            var xDesktop = DocumentHelper.getCurrentDesktop(xContext);
            if (xDesktop == null) {
                logger.debug("aktualisiereInAllenFrames: kein Desktop gefunden");
                return;
            }
            var xFramesSupplier = Lo.qi(XFramesSupplier.class, xDesktop);
            if (xFramesSupplier == null) {
                logger.debug("aktualisiereInAllenFrames: XFramesSupplier nicht verfügbar");
                return;
            }
            var xFrames = xFramesSupplier.getFrames();
            if (xFrames == null) {
                return;
            }
            int anzahl = xFrames.getCount();
            for (int i = 0; i < anzahl; i++) {
                try {
                    var xFrame = Lo.qi(XFrame.class, xFrames.getByIndex(i));
                    aktualisiereInFrame(xFrame, xContext);
                } catch (Exception e) {
                    logger.error("Fehler beim Aktualisieren der Spieltag-Toolbar in Frame {}", i, e);
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Durchsuchen aller Frames für Spieltag-Toolbar", e);
        }
    }

    // ---------------------------------------------------------------------------

    private static void aktualisiereInFrame(XFrame xFrame, XComponentContext xContext) {
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
            boolean sichtbar = ermittleSichtbarkeit(xFrame, xContext);
            if (sichtbar) {
                xLayoutManager.requestElement(SPIELTAG_TOOLBAR_URL);
                xLayoutManager.showElement(SPIELTAG_TOOLBAR_URL);
            } else {
                xLayoutManager.hideElement(SPIELTAG_TOOLBAR_URL);
            }
            logger.debug("aktualisiereInFrame '{}': sichtbar={}", xFrame.getName(), sichtbar);
        } catch (Exception e) {
            logger.error("Fehler beim Aktualisieren der Spieltag-Toolbar in Frame '{}'",
                    xFrame.getName(), e);
        }
    }

    private static boolean ermittleSichtbarkeit(XFrame xFrame, XComponentContext xContext) {
        try {
            var ws = new WorkingSpreadsheet(xContext);
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            return ts != null && ts.hatMehrereSpielTage();
        } catch (Exception e) {
            logger.debug("Sichtbarkeit konnte nicht ermittelt werden: {}", e.getMessage());
            return false;
        }
    }
}
