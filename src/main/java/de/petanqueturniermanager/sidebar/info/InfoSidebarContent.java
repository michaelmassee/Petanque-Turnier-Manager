/*
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.info;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import com.sun.star.beans.XPropertySet;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.comp.newrelease.ReleaseUpdateService;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.webserver.TurnierStatusErmittler;
import de.petanqueturniermanager.webserver.WebServerManager;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.timer.TimerListener;
import de.petanqueturniermanager.timer.TimerManager;
import de.petanqueturniermanager.timer.TimerState;

/**
 * Zeigt die installierte Plugin-Version, das aktuelle Turniersystem und den Turnier-Timer.
 *
 * @author Michael Massee
 */
public class InfoSidebarContent extends BaseSidebarContent implements TimerListener {

    private static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

    private volatile XFixedText versionLabel;
    private XFixedText turnierSystemLabel;
    private XFixedText turnierSchrittLabel;
    private volatile XFixedText timerLabel;
    private volatile XControl timerIconControl;
    private volatile XControl webserverIconControl;
    private volatile XFixedText webserverStatusLabel;
    private final Runnable runnerZustandListener = this::runnerZustandAktualisieren;
    private final Runnable webserverStatusListener = this::webserverStatusAktualisieren;
    private final Runnable versionUpdateCallback = this::versionLabelAktualisieren;

    public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
        SheetRunner.addStateChangeListener(runnerZustandListener);
        WebServerManager.get().addStatusListener(webserverStatusListener);
        try {
            ReleaseUpdateService.get().addStatusListener(versionUpdateCallback);
        } catch (IllegalStateException e) {
            logger.debug("ReleaseUpdateService nicht initialisiert – Sidebar-Version statisch");
        }
    }

    @Override
    protected void felderHinzufuegen() {
        XControl versionControl = GuiFactory.createLabel(getGuiFactoryCreateParam(), getPluginVersion(),
                new Rectangle(0, 0, 200, 20), null);
        if (versionControl == null) {
            return;
        }
        versionLabel = Lo.qi(XFixedText.class, versionControl);
        getLayout().addLayout(new ControlLayout(versionControl), 1);

        XControl turnierSystemControl = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                turnierSystemAnzeige(),
                new Rectangle(0, 0, 200, 20), null);
        if (turnierSystemControl != null) {
            turnierSystemLabel = Lo.qi(XFixedText.class, turnierSystemControl);
            getLayout().addLayout(new ControlLayout(turnierSystemControl), 1);
        }

        var schrittBildUrl = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext())
                .getImageUrlDir() + "sidebar-fortschritt.png";
        XControl schrittIconControl = GuiFactory.createBildControl(
                getGuiFactoryCreateParam(), schrittBildUrl, new Rectangle(0, 0, 20, 20), null);

        XControl turnierSchrittControl = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                turnierSchrittAnzeige(),
                new Rectangle(0, 0, 200, 20), null);
        if (turnierSchrittControl != null) {
            turnierSchrittLabel = Lo.qi(XFixedText.class, turnierSchrittControl);
            var schrittZeile = new HorizontalLayout();
            if (schrittIconControl != null) {
                schrittZeile.addLayout(new ControlLayout(schrittIconControl, 20), 0);
            }
            schrittZeile.addLayout(new ControlLayout(turnierSchrittControl), 1);
            getLayout().addLayout(schrittZeile, 1);
        }

        var timerState = TimerManager.get().getAktuellerZustand();
        var timerImageDir = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext()).getImageUrlDir();
        XControl timerIconCtrl = GuiFactory.createBildControl(
                getGuiFactoryCreateParam(), timerImageDir + timerIconDateiname(timerState),
                new Rectangle(0, 0, 20, 20), null);
        timerIconControl = timerIconCtrl;

        XControl timerControl = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                timerAnzeige(timerState),
                new Rectangle(0, 0, 200, 20), null);
        if (timerControl != null) {
            timerLabel = Lo.qi(XFixedText.class, timerControl);
            var timerZeile = new HorizontalLayout();
            if (timerIconCtrl != null) {
                timerZeile.addLayout(new ControlLayout(timerIconCtrl, 20), 0);
            }
            timerZeile.addLayout(new ControlLayout(timerControl), 1);
            getLayout().addLayout(timerZeile, 1);
            TimerManager.get().addListener(this);
        }

        var wsImageDir = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext()).getImageUrlDir();
        XControl wsIconCtrl = GuiFactory.createBildControl(
                getGuiFactoryCreateParam(), wsImageDir + webserverIconDateiname(),
                new Rectangle(0, 0, 20, 20), null);
        webserverIconControl = wsIconCtrl;

        XControl wsStatusCtrl = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                webserverStatusAnzeige(),
                new Rectangle(0, 0, 200, 20), null);
        if (wsStatusCtrl != null) {
            webserverStatusLabel = Lo.qi(XFixedText.class, wsStatusCtrl);
            var wsZeile = new HorizontalLayout();
            if (wsIconCtrl != null) {
                wsZeile.addLayout(new ControlLayout(wsIconCtrl, 20), 0);
            }
            wsZeile.addLayout(new ControlLayout(wsStatusCtrl), 1);
            getLayout().addLayout(wsZeile, 1);
        }

    }

    @Override
    public void onChange(TimerState state) {
        var label = timerLabel;
        if (label != null) {
            try {
                label.setText(timerAnzeige(state));
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des Timer-Labels", e);
            }
        }
        var icon = timerIconControl;
        if (icon != null) {
            try {
                var model = Lo.qi(XPropertySet.class, icon.getModel());
                if (model != null) {
                    var url = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext())
                            .getImageUrlDir() + timerIconDateiname(state);
                    model.setPropertyValue("ImageURL", url);
                }
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des Timer-Icons", e);
            }
        }
    }

    @Override
    protected void felderAktualisieren(ITurnierEvent eventObj) {
        var label = turnierSystemLabel;
        if (label != null) {
            try {
                label.setText(turnierSystemAnzeige());
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des Turniersystem-Labels", e);
            }
        }
        var schrittLabel = turnierSchrittLabel;
        if (schrittLabel != null) {
            try {
                schrittLabel.setText(turnierSchrittAnzeige());
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des TurnierSchritt-Labels", e);
            }
        }
    }

    @Override
    protected void vorFensterDispose() {
        timerLabel = null;
        timerIconControl = null;
        versionLabel = null;
        turnierSystemLabel = null;
        turnierSchrittLabel = null;
        webserverStatusLabel = null;
        webserverIconControl = null;
    }

    @Override
    protected void onDisposing(EventObject event) {
        timerLabel = null;
        timerIconControl = null;
        versionLabel = null;
        turnierSystemLabel = null;
        turnierSchrittLabel = null;
        webserverStatusLabel = null;
        webserverIconControl = null;
        try {
            TimerManager.get().removeListener(this);
        } catch (Exception e) {
            logger.error("Fehler beim Entfernen des TimerListeners", e);
        }
        SheetRunner.removeStateChangeListener(runnerZustandListener);
        WebServerManager.get().removeStatusListener(webserverStatusListener);
        try {
            ReleaseUpdateService.get().removeStatusListener(versionUpdateCallback);
        } catch (IllegalStateException e) {
            // Service nie initialisiert worden – ok.
        }
    }

    private void runnerZustandAktualisieren() {
        if (SheetRunner.isRunning()) {
            return;
        }
        var label = turnierSchrittLabel;
        if (label == null) {
            return;
        }
        try {
            label.setText(turnierSchrittAnzeige());
        } catch (com.sun.star.lang.DisposedException e) {
            logger.debug("Dokument bereits disposed beim TurnierSchritt-Label-Update – ignoriert");
        } catch (Exception e) {
            logger.error("Fehler beim Aktualisieren des TurnierSchritt-Labels nach Runner-Stop", e);
        }
    }

    String getPluginVersion() {
        var context = getCurrentSpreadsheet().getxContext();
        String installiert;
        boolean updateVerfuegbar = false;
        String neu = null;
        try {
            var service = ReleaseUpdateService.get();
            installiert = service.getInstallierteVersion()
                    .orElse(ExtensionsHelper.from(context).getVersionNummer());
            updateVerfuegbar = service.isUpdateVerfuegbar()
                    || GlobalProperties.get().isNewVersionCheckImmerTrue();
            if (updateVerfuegbar) {
                neu = service.getNeuesteVersionTag().orElse(null);
            }
        } catch (IllegalStateException e) {
            installiert = ExtensionsHelper.from(context).getVersionNummer();
        }
        if (installiert == null) {
            return "–";
        }
        if (updateVerfuegbar && neu != null) {
            return I18n.get("sidebar.info.version.neu", installiert, neu);
        }
        return I18n.get("sidebar.info.version", installiert);
    }

    private void versionLabelAktualisieren() {
        var label = versionLabel;
        if (label != null) {
            try {
                label.setText(getPluginVersion());
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des Versions-Labels", e);
            }
        }
    }

    String turnierSystemAnzeige() {
        var system = getTurnierSystemAusDocument();
        var bezeichnung = (system != null) ? system.getBezeichnung() : "";
        return I18n.get("sidebar.info.turniersystem", bezeichnung);
    }

    String turnierSchrittAnzeige() {
        return TurnierStatusErmittler.ermitteln(getCurrentSpreadsheet());
    }

    private String timerAnzeige(TimerState state) {
        var anzeige = state.bezeichnung().isBlank()
                ? state.anzeige()
                : state.anzeige() + " " + state.bezeichnung();
        return I18n.get("sidebar.info.timer", anzeige);
    }

    private String timerIconDateiname(TimerState state) {
        return switch (state.zustand()) {
            case LAEUFT -> "toolbar-timer-start.png";
            case PAUSIERT -> "toolbar-timer-pause.png";
            default -> "toolbar-timer-stop.png";
        };
    }

    private String webserverStatusAnzeige() {
        return WebServerManager.get().isLaeuft()
                ? I18n.get("sidebar.info.webserver.aktiv")
                : I18n.get("sidebar.info.webserver.gestoppt");
    }

    private String webserverIconDateiname() {
        return WebServerManager.get().isLaeuft()
                ? "toolbar-webserver-starten.png"
                : "toolbar-webserver-stoppen.png";
    }

    private void webserverStatusAktualisieren() {
        var label = webserverStatusLabel;
        if (label != null) {
            try {
                label.setText(webserverStatusAnzeige());
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des Webserver-Status-Labels", e);
            }
        }
        var icon = webserverIconControl;
        if (icon != null) {
            try {
                var model = Lo.qi(XPropertySet.class, icon.getModel());
                if (model != null) {
                    var url = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext())
                            .getImageUrlDir() + webserverIconDateiname();
                    model.setPropertyValue("ImageURL", url);
                }
            } catch (Exception e) {
                logger.error("Fehler beim Aktualisieren des Webserver-Icons", e);
            }
        }
    }
}
