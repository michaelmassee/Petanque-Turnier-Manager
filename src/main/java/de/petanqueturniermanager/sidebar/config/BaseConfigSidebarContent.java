/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.konfigdialog.AuswahlConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.config.color.BackgrnColorConfigSidebarElement;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public abstract class BaseConfigSidebarContent extends BaseSidebarContent {
    static final Logger logger = LogManager.getLogger(BaseConfigSidebarContent.class);

    private boolean turnierFields;

    /**
     * @param workingSpreadsheet
     * @param parentWindow
     * @param xSidebar
     */
    public BaseConfigSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
        super(workingSpreadsheet, parentWindow, xSidebar);
    }

    @Override
    protected void disposing(EventObject event) {
    }

    /**
     * event from menu
     */
    @Override
    protected void updateFieldContens(ITurnierEvent eventObj) {
        if (!turnierFields) {
            addFields();
        }
    }

    /**
     * event from new and load
     */
    @Override
    protected void removeAndAddFields() {
        boolean mustLayout = false;
        if (turnierFields) {
            super.removeAllFieldsAndNewBaseWindow();
            mustLayout = true;
            turnierFields = false;
        }
        addFields(mustLayout);
    }

    @Override
    protected void addFields() {
        addFields(false);
    }

    private void addFields(boolean mustLayout) {

        // Turnier vorhanden ?
        TurnierSystem turnierSystemAusDocument = getTurnierSystemAusDocument();
        if (turnierSystemAusDocument == null || turnierSystemAusDocument == TurnierSystem.KEIN) {
            // kein Turnier
            turnierFields = false;
            if (mustLayout) {
                getxSidebar().requestLayout();
            }
            return;
        }

        logger.debug("addFields");

        List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(getCurrentSpreadsheet());
        if (konfigProperties == null) {
            // kein Turnier vorhanden
            return;
        }

        setChangingLayout(true);
        try {
            konfigProperties.stream().filter(konfigprop -> konfigprop.isInSideBar()).filter(getKonfigFieldFilter())
                    .collect(Collectors.toList()).forEach(konfigprop -> addPropToPanel(konfigprop));
        } finally {
            setChangingLayout(false);
        }

        // Request layout of the sidebar.
        // Call this method when one of the panels wants to change its size due to late
        // initialization or different content after a context change.
        getxSidebar().requestLayout();
        turnierFields = true;
    }

    private void addPropToPanel(ConfigProperty<?> configProperty) {

        switch (configProperty.getType()) {
            case STRING:

                if (configProperty instanceof AuswahlConfigProperty) {
                    // ComboBox
                    AuswahlConfigSidebarElement auswahlConfigSidebarElement = new AuswahlConfigSidebarElement(
                            getGuiFactoryCreateParam(), (AuswahlConfigProperty) configProperty, getCurrentSpreadsheet());
                    getLayout().addLayout(auswahlConfigSidebarElement.getLayout(), 1);
                } else {
                    // create textfield mit btn
                    @SuppressWarnings("unchecked")
                    StringConfigSidebarElement stringConfigSidebarElement = new StringConfigSidebarElement(
                            getGuiFactoryCreateParam(), (ConfigProperty<String>) configProperty, getCurrentSpreadsheet());
                    getLayout().addLayout(stringConfigSidebarElement.getLayout(), 1);
                }
                break;
            case BOOLEAN:
                // create checkbox
                @SuppressWarnings("unchecked")
                BooleanConfigSidebarElement booleanConfigSidebarElement = new BooleanConfigSidebarElement(
                        getGuiFactoryCreateParam(), (ConfigProperty<Boolean>) configProperty, getCurrentSpreadsheet());
                getLayout().addLayout(booleanConfigSidebarElement.getLayout(), 1);
                break;
            case COLOR:
                // create colorpicker
                @SuppressWarnings("unchecked")
                BackgrnColorConfigSidebarElement backgrnColorConfigSidebarElement = new BackgrnColorConfigSidebarElement(
                        getGuiFactoryCreateParam(), (ConfigProperty<Integer>) configProperty, getCurrentSpreadsheet());
                getLayout().addLayout(backgrnColorConfigSidebarElement.getLayout(), 1);
                break;
            case INTEGER:
                @SuppressWarnings("unchecked")
                IntegerConfigSidebarElement integerConfigSidebarElement = new IntegerConfigSidebarElement(
                        getGuiFactoryCreateParam(), (ConfigProperty<Integer>) configProperty, getCurrentSpreadsheet());
                getLayout().addLayout(integerConfigSidebarElement.getLayout(), 1);
                break;
            default:
                break;
        }

    }

    protected abstract java.util.function.Predicate<ConfigProperty<?>> getKonfigFieldFilter();

}
