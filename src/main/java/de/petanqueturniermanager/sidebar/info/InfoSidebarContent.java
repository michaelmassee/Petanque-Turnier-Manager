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
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.timer.TimerListener;
import de.petanqueturniermanager.timer.TimerManager;
import de.petanqueturniermanager.timer.TimerState;

/**
 * Zeigt die installierte Plugin-Version und den aktuellen Turnier-Timer.
 *
 * @author Michael Massee
 */
public class InfoSidebarContent extends BaseSidebarContent implements TimerListener {

    private static final Logger logger = LogManager.getLogger(InfoSidebarContent.class);

    private XFixedText versionLabel;
    private volatile XFixedText timerLabel;

    public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
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
        // Version und Timer ändern sich unabhängig vom Turnier-Event
    }

    @Override
    protected void onDisposing(EventObject event) {
        timerLabel = null;
        versionLabel = null;
        try {
            TimerManager.get().removeListener(this);
        } catch (Exception e) {
            logger.error("Fehler beim Entfernen des TimerListeners", e);
        }
    }

    String getPluginVersion() {
        var version = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext()).getVersionNummer();
        return version != null ? version : "–";
    }

    private String timerAnzeige(TimerState state) {
        var anzeige = state.bezeichnung().isBlank()
                ? state.anzeige()
                : state.anzeige() + " " + state.bezeichnung();
        return I18n.get("sidebar.info.timer", anzeige);
    }
}
