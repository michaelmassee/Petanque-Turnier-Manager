/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.toolbar;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XFramesSupplier;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * Blendet eine Add-on-Toolbar in allen offenen Calc-Fenstern ein oder aus. Nur auf dem LO-Main-Thread aufrufen
 * (VCL, siehe Threading-Regel in CLAUDE.md). Druckvorschau-Fenster werden übersprungen: ihr Controller ist keine
 * {@link XSpreadsheetView}.
 */
final class AddonToolbarSichtbarkeit {

    private static final Logger logger = LogManager.getLogger(AddonToolbarSichtbarkeit.class);

    private AddonToolbarSichtbarkeit() {
    }

    /**
     * @param toolbarUrl Ressourcen-URL der Toolbar; Add-on-Toolbars tragen intern den Prefix {@code addon_}
     * @param sichtbar   entscheidet je Fenster, ob die Toolbar sichtbar ist
     */
    static void setzeInAllenFrames(XComponentContext xContext, String toolbarUrl, Predicate<XFrame> sichtbar) {
        try {
            var xFramesSupplier = Lo.qi(XFramesSupplier.class, DocumentHelper.getCurrentDesktop(xContext));
            var xFrames = xFramesSupplier == null ? null : xFramesSupplier.getFrames();
            if (xFrames == null) {
                logger.debug("{}: keine Fenster gefunden", toolbarUrl);
                return;
            }
            for (int i = 0; i < xFrames.getCount(); i++) {
                try {
                    setzeInFrame(Lo.qi(XFrame.class, xFrames.getByIndex(i)), toolbarUrl, sichtbar);
                } catch (Exception e) {
                    logger.error("{}: Sichtbarkeit in Fenster {} nicht gesetzt", toolbarUrl, i, e);
                }
            }
        } catch (Exception e) {
            logger.error("{}: Fenster konnten nicht durchsucht werden", toolbarUrl, e);
        }
    }

    private static void setzeInFrame(XFrame xFrame, String toolbarUrl, Predicate<XFrame> sichtbar) throws Exception {
        if (xFrame == null || Lo.qi(XSpreadsheetView.class, xFrame.getController()) == null) {
            return;
        }
        var xFrameProps = Lo.qi(XPropertySet.class, xFrame);
        var xLayoutManager = xFrameProps == null ? null
                : Lo.qi(XLayoutManager.class, xFrameProps.getPropertyValue("LayoutManager"));
        if (xLayoutManager == null) {
            return;
        }
        boolean anzeigen = sichtbar.test(xFrame);
        if (anzeigen) {
            xLayoutManager.requestElement(toolbarUrl);
            xLayoutManager.showElement(toolbarUrl);
        } else {
            xLayoutManager.hideElement(toolbarUrl);
        }
        logger.trace("[FOKUS-TRACE] {} frameHash={} sichtbar={}", toolbarUrl, System.identityHashCode(xFrame),
                anzeigen);
    }
}
