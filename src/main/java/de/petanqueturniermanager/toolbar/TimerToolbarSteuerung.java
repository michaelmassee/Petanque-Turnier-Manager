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
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.timer.TimerListener;
import de.petanqueturniermanager.timer.TimerState;
import de.petanqueturniermanager.timer.TimerZustand;

/**
 * Blendet die Timer-Toolbar ({@code private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.timer})
 * automatisch ein, sobald der Timer gestartet wird.
 * <p>
 * Wird als {@link TimerListener} beim {@link de.petanqueturniermanager.timer.TimerManager} registriert
 * und reagiert auf jeden Zustandswechsel: beim Übergang in den Zustand {@link TimerZustand#LAEUFT}
 * wird die Toolbar in allen offenen Frames eingeblendet.
 * </p>
 */
public final class TimerToolbarSteuerung implements TimerListener {

    private static final Logger logger = LogManager.getLogger(TimerToolbarSteuerung.class);

    // Addon-Toolbars erhalten von LibreOffice intern den Prefix "addon_".
    static final String TIMER_TOOLBAR_URL =
            "private:resource/toolbar/addon_de.petanqueturniermanager.toolbar.timer";

    private final XComponentContext xContext;

    public TimerToolbarSteuerung(XComponentContext xContext) {
        this.xContext = xContext;
    }

    public static void anzeigenInAllenFrames(XComponentContext xContext) {
        new TimerToolbarSteuerung(xContext).zeigeToolbarInAllenFrames();
    }

    @Override
    public void onChange(TimerState state) {
        if (state.zustand() == TimerZustand.LAEUFT) {
            zeigeToolbarInAllenFrames();
        }
    }

    // ---------------------------------------------------------------------------

    private void zeigeToolbarInAllenFrames() {
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
                return;
            }
            int anzahl = xFrames.getCount();
            for (int i = 0; i < anzahl; i++) {
                try {
                    var xFrame = Lo.qi(XFrame.class, xFrames.getByIndex(i));
                    zeigeToolbarInFrame(xFrame);
                } catch (Exception e) {
                    logger.error("Fehler beim Einblenden der Timer-Toolbar in Frame {}", i, e);
                }
            }
        } catch (Exception e) {
            logger.error("Fehler beim Durchsuchen aller Frames für Timer-Toolbar", e);
        }
    }

    private void zeigeToolbarInFrame(XFrame xFrame) {
        if (xFrame == null) {
            return;
        }
        try {
            // Druckvorschau-Frames überspringen: ScPreviewController implementiert XSpreadsheetView nicht
            if (Lo.qi(XSpreadsheetView.class, xFrame.getController()) == null) {
                logger.debug("zeigeToolbarInFrame '{}': Druckvorschau-Frame erkannt – übersprungen", xFrame.getName());
                return;
            }
            var xFrameProps = Lo.qi(XPropertySet.class, xFrame);
            if (xFrameProps == null) {
                return;
            }
            var xLayoutManager = Lo.qi(XLayoutManager.class, xFrameProps.getPropertyValue("LayoutManager"));
            if (xLayoutManager == null) {
                return;
            }
            xLayoutManager.requestElement(TIMER_TOOLBAR_URL);
            xLayoutManager.showElement(TIMER_TOOLBAR_URL);
            logger.debug("zeigeToolbarInFrame '{}': Timer-Toolbar eingeblendet", xFrame.getName());
        } catch (Exception e) {
            logger.error("Fehler beim Einblenden der Timer-Toolbar in Frame '{}'", xFrame.getName(), e);
        }
    }
}
