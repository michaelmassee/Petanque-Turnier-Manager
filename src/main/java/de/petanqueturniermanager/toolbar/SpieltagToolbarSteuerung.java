/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XFrame;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

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
        AddonToolbarSichtbarkeit.setzeInAllenFrames(xContext, SPIELTAG_TOOLBAR_URL,
                frame -> ermittleSichtbarkeit(frame, xContext));
    }

    private static boolean ermittleSichtbarkeit(XFrame xFrame, XComponentContext xContext) {
        try {
            // Dokument aus dem eigenen Frame ermitteln – nicht das global aktive Dokument.
            // Bei mehreren offenen Dokumenten könnte sonst der falsche TurnierSystem-Wert
            // für diesen Frame verwendet werden.
            var controller = xFrame.getController();
            if (controller == null) {
                return false;
            }
            var doc = Lo.qi(XSpreadsheetDocument.class, controller.getModel());
            if (doc == null) {
                return false;
            }
            var ws = new WorkingSpreadsheet(xContext, doc);
            TurnierSystem ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            return ts != null && ts.hatMehrereSpielTage();
        } catch (Exception e) {
            logger.debug("Sichtbarkeit konnte nicht ermittelt werden: {}", e.getMessage());
            return false;
        }
    }
}
