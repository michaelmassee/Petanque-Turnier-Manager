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

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

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

            boolean istGeradeKiosk = !lm.isElementVisible("private:resource/menubar/menubar");
            boolean neuerZustand;
            if (istGeradeKiosk) {
                deaktivierenIntern(lm, ws);
                neuerZustand = false;
            } else {
                aktivierenIntern(lm, ws);
                neuerZustand = true;
            }

            var docProps = new DocumentPropertiesHelper(ws);
            if (docProps.getTurnierSystemAusDocument() != TurnierSystem.KEIN) {
                docProps.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIER_MODUS, neuerZustand);
            }
        } catch (Exception e) {
            logger.error("Fehler beim Umschalten", e);
        }
    }

    public void aktivieren(WorkingSpreadsheet ws) {
        try {
            var lm = holeLayoutManager(ws);
            if (lm == null) return;
            aktivierenIntern(lm, ws);
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
            deaktivierenIntern(lm, ws);
        } catch (Exception e) {
            logger.error("Fehler beim Wiederherstellen der UI-Elemente", e);
        }
    }

    public boolean startupNochNichtDurchgefuehrt() {
        return startupDurchgefuehrt.compareAndSet(false, true);
    }

    // -------------------------------------------------------------------------

    private void aktivierenIntern(XLayoutManager lm, WorkingSpreadsheet ws) {
        gespeicherteElemente.clear();
        String ptmUrl = ToolbarAnzeigenListener.TOOLBAR_RESOURCE_URL;

        try {
            lm.lock();
            // Nicht-PTM-Elemente ausblenden. url==null → unbekanntes Element → NICHT ausblenden.
            try {
                for (XUIElement el : lm.getElements()) {
                    String url = el.getResourceURL();
                    if (url == null) continue;
                    if (url.contains("de.petanqueturniermanager.toolbar")) continue;
                    if (lm.isElementVisible(url)) {
                        gespeicherteElemente.add(url);
                        lm.hideElement(url);
                    }
                }
            } catch (Exception e) {
                logger.error("Fehler beim Ausblenden der UI-Elemente", e);
            }
        } finally {
            lm.unlock();
        }

        // Rechnerleiste zuerst ausblenden – setPropertyValue("ShowFormulaBar") kann einen
        // LO-internen Layout-Refresh auslösen, der Context-sensitive Toolbars neu bewertet.
        blendeRechnerleiste(ws, false);

        // PTM-Toolbar nach dem Layout-Refresh einblenden.
        // Addon-Toolbars (addon_* URL) brauchen kein createElement – LO verwaltet sie via XCU.
        lm.showElement(ptmUrl);
        lm.requestElement(ptmUrl);

        // Toolbar zusätzlich in allen Frames sicherstellen (belt-and-suspenders)
        ToolbarAnzeigenListener.zeigeToolbarInAllenFrames(ws.getxContext());

        aktiv = true;
    }

    private void deaktivierenIntern(XLayoutManager lm, WorkingSpreadsheet ws) {
        var zuRestaurieren = gespeicherteElemente.isEmpty() ? STANDARD_ELEMENTE : gespeicherteElemente;
        String ptmUrl = ToolbarAnzeigenListener.TOOLBAR_RESOURCE_URL;

        try {
            lm.lock();

            for (String url : zuRestaurieren) {
                if (url != null && !url.equals(ptmUrl)) {
                    try {
                        lm.showElement(url);
                    } catch (Exception e) {
                        logger.warn("Konnte Element nicht zeigen: {}", url);
                    }
                }
            }

            lm.showElement(ptmUrl); // PTM-Toolbar zur Sicherheit nochmal triggern

        } finally {
            lm.unlock();
            gespeicherteElemente.clear();
            aktiv = false;
        }

        // Rechnerleiste via ShowFormulaBar-Property wiederherstellen
        blendeRechnerleiste(ws, true);
    }

    private void blendeRechnerleiste(WorkingSpreadsheet ws, boolean anzeigen) {
        try {
            var xModel = Lo.qi(XModel.class, ws.getWorkingSpreadsheetDocument());
            if (xModel == null) return;
            var controller = xModel.getCurrentController();
            if (controller == null) return;
            var controllerProps = Lo.qi(XPropertySet.class, controller);
            if (controllerProps == null) return;
            controllerProps.setPropertyValue("ShowFormulaBar", anzeigen);
        } catch (Exception e) {
            logger.warn("Konnte Rechnerleiste nicht {}: {}", anzeigen ? "einblenden" : "ausblenden", e.getMessage());
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
