/**
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

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.jedergegenjeden.spielplan.JGJStatusLeser;
import de.petanqueturniermanager.liga.spielplan.LigaStatusLeser;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerPropertiesSpalte;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleePropertiesSpalte;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
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

    private XFixedText versionLabel;
    private XFixedText turnierSystemLabel;
    private XFixedText turnierSchrittLabel;
    private volatile XFixedText timerLabel;
    private final Runnable runnerZustandListener = this::runnerZustandAktualisieren;

    public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
        SheetRunner.addStateChangeListener(runnerZustandListener);
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

        XControl turnierSchrittControl = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                turnierSchrittAnzeige(),
                new Rectangle(0, 0, 200, 20), null);
        if (turnierSchrittControl != null) {
            turnierSchrittLabel = Lo.qi(XFixedText.class, turnierSchrittControl);
            getLayout().addLayout(new ControlLayout(turnierSchrittControl), 1);
        }

        XControl timerControl = GuiFactory.createLabel(getGuiFactoryCreateParam(),
                timerAnzeige(TimerManager.get().getAktuellerZustand()),
                new Rectangle(0, 0, 200, 20), null);
        if (timerControl != null) {
            timerLabel = Lo.qi(XFixedText.class, timerControl);
            getLayout().addLayout(new ControlLayout(timerControl), 1);
            TimerManager.get().addListener(this);
        }

        requestLayout();
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
    protected void onDisposing(EventObject event) {
        timerLabel = null;
        versionLabel = null;
        turnierSystemLabel = null;
        turnierSchrittLabel = null;
        try {
            TimerManager.get().removeListener(this);
        } catch (Exception e) {
            logger.error("Fehler beim Entfernen des TimerListeners", e);
        }
        SheetRunner.removeStateChangeListener(runnerZustandListener);
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
        } catch (Exception e) {
            logger.error("Fehler beim Aktualisieren des TurnierSchritt-Labels nach Runner-Stop", e);
        }
    }

    String getPluginVersion() {
        var version = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext()).getVersionNummer();
        return version != null ? version : "–";
    }

    String turnierSystemAnzeige() {
        var system = getTurnierSystemAusDocument();
        var bezeichnung = (system != null) ? system.getBezeichnung() : "";
        return I18n.get("sidebar.info.turniersystem", bezeichnung);
    }

    String turnierSchrittAnzeige() {
        var system = getTurnierSystemAusDocument();
        if (system == null) {
            return "";
        }
        var docPropHelper = new DocumentPropertiesHelper(getCurrentSpreadsheet());
        return switch (system) {
            case SUPERMELEE -> {
                int spieltag = docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELTAG, 1);
                int runde = docPropHelper.getIntProperty(SuperMeleePropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 1);
                yield I18n.get("sidebar.info.supermelee.schritt", spieltag, runde);
            }
            case SCHWEIZER -> {
                int runde = docPropHelper.getIntProperty(SchweizerPropertiesSpalte.KONFIG_PROP_NAME_SPIELRUNDE, 1);
                yield I18n.get("sidebar.info.spielrunde", runde);
            }
            case LIGA -> {
                var status = LigaStatusLeser.von(getCurrentSpreadsheet()).liesStatus();
                if (!status.spielplanVorhanden()) {
                    yield I18n.get("sidebar.info.liga.meldungen.erfassen");
                }
                if (status.alleGespielt()) {
                    yield I18n.get("sidebar.info.liga.beendet");
                }
                yield I18n.get("sidebar.info.liga.schritt",
                        status.hrGespielt(), status.hrGesamt(),
                        status.rrGespielt(), status.rrGesamt());
            }
            case JGJ -> {
                var status = JGJStatusLeser.von(getCurrentSpreadsheet()).liesStatus();
                if (!status.spielplanVorhanden()) {
                    yield I18n.get("sidebar.info.jgj.meldungen.erfassen");
                }
                if (status.alleGespielt()) {
                    yield I18n.get("sidebar.info.jgj.beendet");
                }
                yield I18n.get("sidebar.info.jgj.schritt",
                        status.hrGespielt(), status.hrGesamt(),
                        status.rrGespielt(), status.rrGesamt());
            }
            default -> "";
        };
    }

    private String timerAnzeige(TimerState state) {
        var anzeige = state.bezeichnung().isBlank()
                ? state.anzeige()
                : state.anzeige() + " " + state.bezeichnung();
        return I18n.get("sidebar.info.timer", anzeige);
    }
}
