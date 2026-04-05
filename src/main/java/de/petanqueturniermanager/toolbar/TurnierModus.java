package de.petanqueturniermanager.toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.ui.XUIElement;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;

/**
 * Verwaltung des Turnier-Modus (Kiosk-Modus) für LibreOffice Calc.
 * Die PTM-Toolbar bleibt immer sichtbar.
 */
public class TurnierModus {

    private static final Logger logger = LogManager.getLogger(TurnierModus.class);

    private static final TurnierModus INSTANCE = new TurnierModus();

    private static final List<String> STANDARD_ELEMENTE = List.of(
            "private:resource/menubar/menubar",
            "private:resource/toolbar/standardbar",
            "private:resource/toolbar/formatobjectbar",
            "private:resource/toolbar/formulabar",
            "private:resource/statusbar/statusbar"
    );

    private volatile boolean aktiv = false;
    private final List<String> gespeicherteElemente = new ArrayList<>();
    private final AtomicBoolean startupDurchgefuehrt = new AtomicBoolean(false);

    private TurnierModus() {
    }

    public static TurnierModus get() {
        return INSTANCE;
    }

    public boolean istAktiv() {
        return aktiv;
    }


    public void umschalten(WorkingSpreadsheet ws) {
        try {
            var lm = holeLayoutManager(ws);
            if (lm == null) return;

            // Wir prüfen den IST-Zustand direkt am aktuellen Frame/LayoutManager
            // Anstatt die globale Variable 'aktiv' zu nutzen
            boolean istGeradeKiosk = !lm.isElementVisible("private:resource/menubar/menubar");

            if (istGeradeKiosk) {
                deaktivierenIntern(lm);
            } else {
                aktivierenIntern(lm);
            }

            // TODO im Document speichern nicht in der Properties Datei !
            // GlobalProperties.get().setStartupTurnierModus(!istGeradeKiosk);
        } catch (Exception e) {
            logger.error("Fehler beim Umschalten", e);
        }
    }


    public void aktivieren(WorkingSpreadsheet ws) {
        try {
            var lm = holeLayoutManager(ws);
            if (lm == null) return;
            aktivierenIntern(lm);
        } catch (Exception e) {
            logger.error("Fehler beim Aktivieren des Turnier-Modus", e);
        }
    }

    public void wiederherstellenAlleElemente(WorkingSpreadsheet ws) {
        try {
            var lm = holeLayoutManager(ws);
            if (lm == null) return;

            // PTM-Toolbar immer zuerst anzeigen
            lm.showElement(ToolbarAnzeigenListener.TOOLBAR_RESOURCE_URL);
            deaktivierenIntern(lm);
        } catch (Exception e) {
            logger.error("Fehler beim Wiederherstellen der UI-Elemente", e);
        }
    }

    public boolean startupNochNichtDurchgefuehrt() {
        return startupDurchgefuehrt.compareAndSet(false, true);
    }

    // -------------------------------------------------------------------------

    private void aktivierenIntern(XLayoutManager lm) {
        gespeicherteElemente.clear();
        String ptmUrl = ToolbarAnzeigenListener.TOOLBAR_RESOURCE_URL;

        try {
            lm.lock();
            XUIElement[] elements = lm.getElements();
            for (XUIElement el : elements) {
                String url = el.getResourceURL();
                // contains() statt equals() – robust gegen abweichende URL-Prefix-Formen
                if (url == null || url.contains("de.petanqueturniermanager.toolbar")) continue;

                if (lm.isElementVisible(url)) {
                    gespeicherteElemente.add(url);
                    lm.hideElement(url);
                }
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            lm.unlock();
        }

        // Nach dem Lock: Toolbar explizit einblenden – wie ToolbarAnzeigenListener.zeigeToolbarInFrame()
        try {
            lm.createElement(ptmUrl);
            lm.showElement(ptmUrl);
        } catch (Exception e) {
            logger.error("Fehler beim Einblenden der PTM-Toolbar nach TurnierModus-Aktivierung", e);
        }
    }

    private void deaktivierenIntern(XLayoutManager lm) {
        var zuRestaurieren = gespeicherteElemente.isEmpty() ? STANDARD_ELEMENTE : gespeicherteElemente;
        String ptmUrl = ToolbarAnzeigenListener.TOOLBAR_RESOURCE_URL;

        try {
            lm.lock(); // UI einfrieren

            for (String url : zuRestaurieren) {
                if (url != null && !url.equals(ptmUrl)) {
                    try {
                        lm.showElement(url);
                    } catch (Exception e) {
                        logger.warn("Konnte Element nicht zeigen: {}", url);
                    }
                }
            }

            lm.showElement(ptmUrl); // PTM Toolbar zur Sicherheit nochmal triggern

        } finally {
            lm.unlock(); // UI berechnet sich jetzt einmalig neu
            gespeicherteElemente.clear();
            aktiv = false;
        }
    }

    private XLayoutManager holeLayoutManager(WorkingSpreadsheet ws) {
        try {
            var xModel = Lo.qi(XModel.class, ws.getWorkingSpreadsheetDocument());
            if (xModel == null) return null;

            var xController = xModel.getCurrentController();
            if (xController == null) return null;

            XFrame frame = xController.getFrame();
            if (frame == null) return null;

            XPropertySet props = Lo.qi(XPropertySet.class, frame);
            if (props == null) return null;

            Object lmObj = props.getPropertyValue("LayoutManager");
            return Lo.qi(XLayoutManager.class, lmObj);
        } catch (Exception e) {
            logger.error("Fehler beim Holen des LayoutManagers", e);
            return null;
        }
    }
}