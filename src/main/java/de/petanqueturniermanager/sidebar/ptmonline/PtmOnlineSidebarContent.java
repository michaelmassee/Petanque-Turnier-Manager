/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.ptmonline;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.DisposedException;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.PtmOnlineAktivierung;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.LoMainThread;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.onlinesync.SpieltagKontext;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncSheet;
import de.petanqueturniermanager.onlinesync.sheet.PtmOnlineSyncStatus;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.AusblendbaresControlLayout;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;

/**
 * Inhalt des Panels „PTM-Online“: Verbindung, letzter Sync und Status des Online-Turniers in je einer Zeile. Die
 * beiden letzten Zeilen sind ausgeblendet, solange es dazu nichts anzuzeigen gibt (z.&nbsp;B. nicht verbunden).
 * <p>
 * Maßgeblich ist das Dokument dieser Sidebar (bei Supermelee der aktive Spieltag), nie der UI-Fokus – bei mehreren
 * offenen Turnieren zeigt jede Sidebar ihr eigenes Dokument.
 *
 * @author Michael Massee
 */
public class PtmOnlineSidebarContent extends BaseSidebarContent {

    private static final Logger logger = LogManager.getLogger(PtmOnlineSidebarContent.class);
    private static final DateTimeFormatter UHRZEIT = DateTimeFormatter.ofPattern("HH:mm");

    private volatile XFixedText verbindungLabel;
    private volatile XFixedText letzterSyncLabel;
    private volatile XFixedText onlineStatusLabel;
    private volatile AusblendbaresControlLayout letzterSyncZeile;
    private volatile AusblendbaresControlLayout onlineStatusZeile;
    // Feuert aus dem SheetRunner-Worker-Thread; setText ist VCL und darf nur auf den Main-Thread.
    private final Runnable runnerZustandListener = () -> aufMainThread(this::runnerZustandAktualisieren);

    /**
     * Zeilen der Anzeige.
     *
     * @param verbindung   erste Zeile (verbunden, pausiert, nicht verbunden, nicht aktiviert)
     * @param letzterSync  Zeitpunkt des letzten Abgleichs, sofern verbunden und bekannt
     * @param onlineStatus Status des Online-Turniers, sofern verbunden und bekannt
     */
    record Zeilen(String verbindung, Optional<String> letzterSync, Optional<String> onlineStatus) {

        static Zeilen nur(String verbindungSchluessel) {
            return new Zeilen(I18n.get(verbindungSchluessel), Optional.empty(), Optional.empty());
        }
    }

    public PtmOnlineSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
        SheetRunner.addStateChangeListener(runnerZustandListener);
    }

    @Override
    protected void felderHinzufuegen() {
        Zeilen zeilen = zeilen();
        XControl verbindungCtrl = neuesLabel(zeilen.verbindung());
        if (verbindungCtrl == null) {
            return;
        }
        verbindungLabel = Lo.qi(XFixedText.class, verbindungCtrl);
        getLayout().addLayout(new ControlLayout(verbindungCtrl), 1);
        XControl syncCtrl = neuesLabel(zeilen.letzterSync().orElse(""));
        if (syncCtrl != null) {
            letzterSyncLabel = Lo.qi(XFixedText.class, syncCtrl);
            letzterSyncZeile = new AusblendbaresControlLayout(syncCtrl, zeilen.letzterSync().isPresent());
            getLayout().addLayout(letzterSyncZeile, 1);
        }
        XControl statusCtrl = neuesLabel(zeilen.onlineStatus().orElse(""));
        if (statusCtrl != null) {
            onlineStatusLabel = Lo.qi(XFixedText.class, statusCtrl);
            onlineStatusZeile = new AusblendbaresControlLayout(statusCtrl, zeilen.onlineStatus().isPresent());
            getLayout().addLayout(onlineStatusZeile, 1);
        }
    }

    private XControl neuesLabel(String text) {
        return GuiFactory.createLabel(getGuiFactoryCreateParam(), text, new Rectangle(0, 0, 200, 20), null);
    }

    @Override
    protected void felderAktualisieren(ITurnierEvent event) {
        aktualisieren();
    }

    /** Verbinden, Abgleich, Pausieren/Fortsetzen, Trennen und die Live-Erfassung laufen als SheetRunner. */
    private void runnerZustandAktualisieren() {
        if (!SheetRunner.isRunning()) {
            aktualisieren();
        }
    }

    private void aufMainThread(Runnable aktion) {
        var ws = getCurrentSpreadsheet();
        if (ws != null) {
            LoMainThread.post(ws.getxContext(), aktion);
        }
    }

    /** Nur auf dem Main-Thread: Texte setzen und Zeilen ein- oder ausblenden. */
    private void aktualisieren() {
        var label = verbindungLabel;
        if (label == null) {
            return;
        }
        try {
            Zeilen zeilen = zeilen();
            label.setText(zeilen.verbindung());
            boolean layoutGeaendert = zeileAktualisieren(letzterSyncLabel, letzterSyncZeile, zeilen.letzterSync());
            layoutGeaendert |= zeileAktualisieren(onlineStatusLabel, onlineStatusZeile, zeilen.onlineStatus());
            if (layoutGeaendert) {
                requestLayout();
            }
        } catch (DisposedException e) {
            logger.debug("Dokument bereits disposed beim PTM-Online-Status-Update – ignoriert", e);
        } catch (RuntimeException e) {
            logger.error("Fehler beim Aktualisieren des PTM-Online-Status", e);
        }
    }

    /** Setzt den Text einer ausblendbaren Zeile; liefert, ob sich ihre Sichtbarkeit geändert hat. */
    private static boolean zeileAktualisieren(XFixedText label, AusblendbaresControlLayout zeile,
            Optional<String> text) {
        if (label == null || zeile == null) {
            return false;
        }
        label.setText(text.orElse(""));
        return zeile.setSichtbar(text.isPresent());
    }

    /** Anzeige für das Dokument dieser Sidebar. */
    Zeilen zeilen() {
        if (!PtmOnlineAktivierung.istAktiv()) {
            return Zeilen.nur("sidebar.ptmonline.nicht_aktiv");
        }
        var ws = getCurrentSpreadsheet();
        var system = getTurnierSystemAusDocument();
        if (ws == null || system == null || system == TurnierSystem.KEIN) {
            return Zeilen.nur("sidebar.ptmonline.nicht_verbunden");
        }
        Integer spieltagNr = null;
        try {
            spieltagNr = SpieltagKontext.aktiverSpieltagOderNull(ws, system);
        } catch (GenerateException e) {
            logger.warn("Aktiver Spieltag für den PTM-Online-Status nicht ermittelbar", e);
        }
        PtmOnlineSyncStatus status = PtmOnlineSyncSheet.leseStatus(ws, spieltagNr);
        if (!status.verbunden()) {
            return Zeilen.nur("sidebar.ptmonline.nicht_verbunden");
        }
        return new Zeilen(
                I18n.get(status.pausiert() ? "sidebar.ptmonline.pausiert" : "sidebar.ptmonline.verbunden"),
                status.letzterSync().map(zeitpunkt -> I18n.get("sidebar.ptmonline.letzter_sync",
                        UHRZEIT.format(zeitpunkt.atZone(ZoneId.systemDefault())))),
                status.onlineStatusAnzeige()
                        .map(onlineStatus -> I18n.get("sidebar.ptmonline.onlinestatus", onlineStatus)));
    }

    @Override
    protected void vorFensterDispose() {
        felderVergessen();
    }

    @Override
    protected void onDisposing(EventObject event) {
        felderVergessen();
        SheetRunner.removeStateChangeListener(runnerZustandListener);
    }

    private void felderVergessen() {
        verbindungLabel = null;
        letzterSyncLabel = null;
        onlineStatusLabel = null;
        letzterSyncZeile = null;
        onlineStatusZeile = null;
    }
}
