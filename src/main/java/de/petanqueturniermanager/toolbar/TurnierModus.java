package de.petanqueturniermanager.toolbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.FeatureStateEvent;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStatusListener;
import com.sun.star.lang.EventObject;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.XUIElement;
import com.sun.star.util.XURLTransformer;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.DokumentKontext;
import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.sidebar.SidebarAnzeigenListener;

/**
 * Verwaltung des Turnier-Modus (Kiosk-Modus) für LibreOffice Calc.
 * Die PTM-Toolbar bleibt immer sichtbar.
 * <p>
 * Der Modus gilt je Dokumentfenster: Status, ausgeblendete Leisten und der Zustand der
 * Rechenleiste werden pro Frame gespeichert, damit mehrere gleichzeitig geöffnete
 * Turnier-Dokumente sich nicht gegenseitig beeinflussen.
 */
public class TurnierModus {

    private static final Logger logger = LogManager.getLogger(TurnierModus.class);

    private static final TurnierModus INSTANCE = new TurnierModus();

    private static final String MENUELEISTE = "private:resource/menubar/menubar";

    private static final List<String> STANDARD_ELEMENTE = List.of(
            MENUELEISTE,
            "private:resource/toolbar/standardbar",
            "private:resource/toolbar/formatobjectbar",
            "private:resource/statusbar/statusbar"
    );

    private final FrameZuordnung<KioskZustand> kioskZustaende = new FrameZuordnung<>();
    private final AtomicBoolean startupDurchgefuehrt = new AtomicBoolean(false);
    private volatile boolean aktivFuerTest = false;
    private final KioskFormatierungsSchutzVerwaltung kioskFormatierungsSchutz = new KioskFormatierungsSchutzVerwaltung();

    private TurnierModus() {
    }

    public static TurnierModus get() {
        return INSTANCE;
    }

    /** Ob der Turniermodus im Fenster dieses Dokuments aktiv ist. */
    public boolean istAktiv(XSpreadsheetDocument dokument) {
        return aktivFuerTest || kioskZustaende.wert(holeFrame(dokument)).isPresent();
    }

    public boolean istAktiv(WorkingSpreadsheet ws) {
        return istAktiv(ws.getWorkingSpreadsheetDocument());
    }

    /**
     * Für Code ohne eigenen Dokumentbezug: prüft das Dokument aus dem {@link DokumentKontext}
     * (gesetzt von {@code SheetRunner.run()}). Ohne Kontext zählt, ob der Modus in irgendeinem
     * Dokument aktiv ist.
     */
    public boolean istAktivImAktuellenKontext() {
        XSpreadsheetDocument dokument = DokumentKontext.get();
        if (dokument != null) {
            return istAktiv(dokument);
        }
        return aktivFuerTest || !kioskZustaende.istLeer();
    }

    /**
     * Nur für Tests: lässt den Turniermodus ohne UI-Side-Effects für alle Dokumente als aktiv
     * gelten bzw. hebt das wieder auf.
     */
    public void setAktivForTest(boolean wert) {
        this.aktivFuerTest = wert;
    }

    public void umschalten(WorkingSpreadsheet ws) {
        try {
            var lm = holeLayoutManager(ws);
            if (lm == null) return;

            boolean istGeradeKiosk = !lm.isElementVisible(MENUELEISTE);
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
                merkeZustandImDokument(docProps, neuerZustand);
            }
        } catch (Exception e) {
            logger.error("Fehler beim Umschalten", e);
            zeigeFehlermeldung(ws);
        }
    }

    /**
     * Schaltet das Turnierdokument {@code ws} in den Turniermodus, wenn die Plugin-Option
     * „Turniermodus automatisch aktivieren" gesetzt ist und der Modus dort noch nicht aktiv ist.
     * Andere geöffnete Dokumente bleiben unberührt. Muss auf dem LO-Main-Thread laufen.
     *
     * @return {@code true}, wenn der Turniermodus dadurch aktiviert wurde
     */
    public boolean aktiviereAutomatischFallsNoetig(WorkingSpreadsheet ws) {
        return aktiviereAutomatischFallsNoetig(ws, GlobalProperties.get().isAutoTurnierModus());
    }

    /** Wie {@link #aktiviereAutomatischFallsNoetig(WorkingSpreadsheet)}, Option explizit (für Tests). */
    boolean aktiviereAutomatischFallsNoetig(WorkingSpreadsheet ws, boolean optionAktiv) {
        try {
            var docProps = new DocumentPropertiesHelper(ws);
            if (!sollAutomatischAktivieren(optionAktiv, docProps.getTurnierSystemAusDocument(), istAktiv(ws))) {
                return false;
            }
            var lm = holeLayoutManager(ws);
            if (lm == null) return false;
            aktivierenIntern(lm, ws);
            merkeZustandImDokument(docProps, true);
            return true;
        } catch (Exception e) {
            logger.error("Fehler beim automatischen Aktivieren des Turnier-Modus", e);
            zeigeFehlermeldung(ws);
            return false;
        }
    }

    /** Entscheidungsregel für {@link #aktiviereAutomatischFallsNoetig(WorkingSpreadsheet)}. */
    static boolean sollAutomatischAktivieren(boolean optionAktiv, TurnierSystem turnierSystem, boolean bereitsAktiv) {
        return optionAktiv && turnierSystem != TurnierSystem.KEIN && !bereitsAktiv;
    }

    private static void merkeZustandImDokument(DocumentPropertiesHelper docProps, boolean aktiv) {
        docProps.setBooleanProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIER_MODUS, aktiv);
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

    private void schuetzeBlattschutzFuerAktivesTurnierSystem(WorkingSpreadsheet ws) {
        try {
            var ts = new DocumentPropertiesHelper(ws).getTurnierSystemAusDocument();
            BlattschutzRegistry.fuer(ts).ifPresent(k -> BlattschutzManager.get().schuetzen(k, ws));
        } catch (Exception e) {
            logger.warn("Blattschutz konnte nicht aktiviert werden: {}", e.getMessage(), e);
        }
    }

    private void entsperreBlattschutzFuerAktivesTurnierSystem(WorkingSpreadsheet ws) {
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
        XFrame frame = holeFrame(ws);
        // Bereits aktiv: den gespeicherten Ausgangszustand behalten – ein erneutes Erfassen sähe
        // nur noch die ausgeblendete Oberfläche und könnte sie später nicht wiederherstellen.
        boolean bereitsAktiv = kioskZustaende.wert(frame).isPresent();
        boolean rechnerleisteWarSichtbar = !bereitsAktiv && leseRechnerleistenZustand(ws);
        List<String> ausgeblendeteElemente = new ArrayList<>();
        String ptmUrl = ToolbarAnzeigenListener.TOOLBAR_RESOURCE_URL;

        try {
            lm.lock();
            // Nicht-PTM-Elemente ausblenden. url==null → unbekanntes Element → NICHT ausblenden.
            try {
                for (XUIElement element : lm.getElements()) {
                    String url = element.getResourceURL();
                    if (url == null || url.equals(MENUELEISTE)) continue;
                    if (url.contains("de.petanqueturniermanager.toolbar")) continue;
                    if (lm.isElementVisible(url)) {
                        ausgeblendeteElemente.add(url);
                        lm.hideElement(url);
                    }
                }
                // Die Menüleiste immer ausblenden, auch wenn LO sie noch nicht erzeugt hat (Fenster
                // direkt nach dem Laden): der LayoutManager merkt sich den Zustand und wendet ihn
                // beim späteren Erzeugen an. Sonst erschiene sie im Turniermodus nachträglich.
                lm.hideElement(MENUELEISTE);
                ausgeblendeteElemente.add(MENUELEISTE);
            } catch (Exception e) {
                logger.error("Fehler beim Ausblenden der UI-Elemente", e);
            }
        } finally {
            lm.unlock();
        }

        // Rechenleiste ausblenden – nur wenn sie aktuell sichtbar ist.
        // Der Dispatch kann einen LO-internen Layout-Refresh auslösen, der
        // Context-sensitive Toolbars neu bewertet.
        if (rechnerleisteWarSichtbar) {
            setzeRechnerleiste(ws, false);
        }
        if (!bereitsAktiv) {
            kioskZustaende.zuordnenFallsNeu(frame,
                    new KioskZustand(List.copyOf(ausgeblendeteElemente), rechnerleisteWarSichtbar));
        }

        // PTM-Toolbar nach dem Layout-Refresh einblenden.
        // Addon-Toolbars (addon_* URL) brauchen kein createElement – LO verwaltet sie via XCU.
        lm.showElement(ptmUrl);
        lm.requestElement(ptmUrl);

        // Toolbar zusätzlich in allen Frames sicherstellen (belt-and-suspenders)
        ToolbarAnzeigenListener.zeigeToolbarInAllenFrames(ws.getxContext());
        TimerToolbarSteuerung.anzeigenInAllenFrames(ws.getxContext());

        schuetzeBlattschutzFuerAktivesTurnierSystem(ws);
        kioskFormatierungsSchutz.aktivieren(frame);
        SidebarAnzeigenListener.zeigePtmSidebar(ws);
    }

    private void deaktivierenIntern(XLayoutManager lm, WorkingSpreadsheet ws) {
        XFrame frame = holeFrame(ws);
        kioskFormatierungsSchutz.deaktivieren(frame);
        entsperreBlattschutzFuerAktivesTurnierSystem(ws);
        Optional<KioskZustand> zustand = kioskZustaende.entfernen(frame);
        List<String> zuRestaurieren = zustand.map(KioskZustand::ausgeblendeteElemente)
                .filter(elemente -> !elemente.isEmpty()).orElse(STANDARD_ELEMENTE);
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
        }

        // Rechenleiste auf gespeicherten Zustand zurücksetzen (Standard: sichtbar)
        setzeRechnerleiste(ws, zustand.map(KioskZustand::rechnerleisteWarSichtbar).orElse(true));
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

    /** Nur für Tests: ob am Frame des Dokuments der Kiosk-Formatierungsschutz registriert ist. */
    boolean istKioskFormatierungsSchutzAktiv(WorkingSpreadsheet ws) {
        return kioskFormatierungsSchutz.istAktiv(holeFrame(ws));
    }

    private XFrame holeFrame(WorkingSpreadsheet ws) {
        return holeFrame(ws.getWorkingSpreadsheetDocument());
    }

    private static XFrame holeFrame(XSpreadsheetDocument dokument) {
        var xModel = Lo.qi(XModel.class, dokument);
        if (xModel == null || xModel.getCurrentController() == null) return null;
        return xModel.getCurrentController().getFrame();
    }

    private XLayoutManager holeLayoutManager(WorkingSpreadsheet ws) {
        try {
            XFrame frame = holeFrame(ws);
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

    /** Ausgangszustand eines Fensters vor dem Turniermodus, für die Wiederherstellung. */
    private record KioskZustand(List<String> ausgeblendeteElemente, boolean rechnerleisteWarSichtbar) {
    }
}
