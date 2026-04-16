package de.petanqueturniermanager.toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatch;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XUIElement;
import com.sun.star.util.XURLTransformer;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
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
    private Boolean gespeicherteRechnerleiste = null;

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
            zeigeFehlermeldung(ws);
        }
    }

    public void aktivieren(WorkingSpreadsheet ws) {
        try {
            var lm = holeLayoutManager(ws);
            if (lm == null) return;
            aktivierenIntern(lm, ws);
        } catch (Exception e) {
            logger.error("Fehler beim Aktivieren des Turnier-Modus", e);
            zeigeFehlermeldung(ws);
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
            zeigeFehlermeldung(ws);
        }
    }

    public boolean startupNochNichtDurchgefuehrt() {
        return startupDurchgefuehrt.compareAndSet(false, true);
    }

    // -------------------------------------------------------------------------

    private void schuetzeBlattschutzFuerAktivesTournierSystem(WorkingSpreadsheet ws) {
        try {
            var ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            BlattschutzRegistry.fuer(ts).ifPresent(k -> BlattschutzManager.get().schuetzen(k, ws));
        } catch (Exception e) {
            logger.warn("Blattschutz konnte nicht aktiviert werden: {}", e.getMessage(), e);
        }
    }

    private void entsperreBlattschutzFuerAktivesTournierSystem(WorkingSpreadsheet ws) {
        try {
            var ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            BlattschutzRegistry.fuer(ts).ifPresent(k -> BlattschutzManager.get().entsperren(k, ws));
        } catch (Exception e) {
            logger.warn("Blattschutz konnte nicht entfernt werden: {}", e.getMessage(), e);
        }
    }

    private void zeigeFehlermeldung(WorkingSpreadsheet ws) {
        MessageBox.from(ws, MessageBoxTypeEnum.ERROR_OK)
                .caption(I18n.get("turnier.modus"))
                .message(I18n.get("turnier.modus.fehler"))
                .show();
    }

    private void aktivierenIntern(XLayoutManager lm, WorkingSpreadsheet ws) {
        gespeicherteElemente.clear();
        gespeicherteRechnerleiste = leseRechnerleistenZustand(ws);
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

        // Rechenleiste ausblenden – nur wenn sie aktuell sichtbar ist.
        // Der Dispatch kann einen LO-internen Layout-Refresh auslösen, der
        // Context-sensitive Toolbars neu bewertet.
        if (Boolean.TRUE.equals(gespeicherteRechnerleiste)) {
            setzeRechnerleiste(ws, false);
        }

        // PTM-Toolbar nach dem Layout-Refresh einblenden.
        // Addon-Toolbars (addon_* URL) brauchen kein createElement – LO verwaltet sie via XCU.
        lm.showElement(ptmUrl);
        lm.requestElement(ptmUrl);

        // Toolbar zusätzlich in allen Frames sicherstellen (belt-and-suspenders)
        ToolbarAnzeigenListener.zeigeToolbarInAllenFrames(ws.getxContext());

        aktiv = true;
        schuetzeBlattschutzFuerAktivesTournierSystem(ws);
    }

    private void deaktivierenIntern(XLayoutManager lm, WorkingSpreadsheet ws) {
        entsperreBlattschutzFuerAktivesTournierSystem(ws);
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

        // Rechenleiste auf gespeicherten Zustand zurücksetzen (Standard: sichtbar)
        setzeRechnerleiste(ws, gespeicherteRechnerleiste == null || gespeicherteRechnerleiste);
        gespeicherteRechnerleiste = null;
    }

    private boolean leseRechnerleistenZustand(WorkingSpreadsheet ws) {
        try {
            var xModel = Lo.qi(XModel.class, ws.getWorkingSpreadsheetDocument());
            if (xModel == null) return true;
            var xController = xModel.getCurrentController();
            if (xController == null) return true;
            var frame = xController.getFrame();
            if (frame == null) return true;

            var urlTransformer = Lo.qi(XURLTransformer.class,
                    ws.getxContext().getServiceManager()
                            .createInstanceWithContext("com.sun.star.util.URLTransformer", ws.getxContext()));
            if (urlTransformer == null) return true;

            var url = new com.sun.star.util.URL();
            url.Complete = ".uno:InputLineVisible";
            var urls = new com.sun.star.util.URL[]{url};
            urlTransformer.parseStrict(urls);
            var parsedUrl = urls[0];

            var dispatchProvider = Lo.qi(XDispatchProvider.class, frame);
            if (dispatchProvider == null) return true;
            var dispatch = dispatchProvider.queryDispatch(parsedUrl, "_self", 0);
            if (dispatch == null) return true;

            // LO ruft statusChanged() synchron bei addStatusListener auf
            final boolean[] zustand = {true};
            var listener = new XStatusListener() {
                @Override
                public void statusChanged(FeatureStateEvent event) {
                    if (event.State instanceof Boolean b) zustand[0] = b;
                }

                @Override
                public void disposing(EventObject source) {
                    // nichts zu tun
                }
            };
            dispatch.addStatusListener(listener, parsedUrl);
            dispatch.removeStatusListener(listener, parsedUrl);
            return zustand[0];
        } catch (Exception e) {
            logger.warn("Konnte Rechnerleisten-Zustand nicht lesen: {}", e.getMessage());
            return true;
        }
    }

    private void setzeRechnerleiste(WorkingSpreadsheet ws, boolean anzeigen) {
        try {
            var pv = new PropertyValue();
            pv.Name = "InputLineVisible";
            pv.Value = Boolean.valueOf(anzeigen);
            ws.executeDispatch(".uno:InputLineVisible", "_self", 0, new PropertyValue[]{pv});
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
