/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.basesheet.konfiguration.KonfigurationSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 */
public abstract class BaseConfigSidebarContent extends BaseSidebarContent {
	static final Logger logger = LogManager.getLogger(BaseConfigSidebarContent.class);

	private static final Predicate<ConfigProperty<?>> KONFIG_PROP_FILTER = konfigprop -> konfigprop.isInSideBar() && !konfigprop.isInSideBarInfoPanel();

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

	private void addFields(boolean forceMustLayout) {

		// Turnier vorhanden ?
		TurnierSystem turnierSystemAusDocument = getTurnierSystemAusDocument();
		if (turnierSystemAusDocument == null || turnierSystemAusDocument == TurnierSystem.KEIN) {
			// kein Turnier
			turnierFields = false;
			if (forceMustLayout) {
				requestLayout();
			}
			return;
		}

		logger.debug("addFields");

		List<ConfigProperty<?>> konfigProperties = KonfigurationSingleton.getKonfigProperties(getCurrentSpreadsheet());
		if (konfigProperties == null) {
			// kein Turnier vorhanden
			return;
		}

		AddConfigElementsToWindow addConfigElementsToWindow = new AddConfigElementsToWindow(getGuiFactoryCreateParam(), getCurrentSpreadsheet(), getLayout());
		setChangingLayout(true);
		try {
			konfigProperties.stream().filter(KONFIG_PROP_FILTER).filter(getKonfigFieldFilter()).collect(Collectors.toList())
					.forEach(konfigprop -> addConfigElementsToWindow.addPropToPanel(konfigprop));
		} finally {
			setChangingLayout(false);
		}

		// Request layout of the sidebar.
		// Call this method when one of the panels wants to change its size due to late
		// initialization or different content after a context change.
		// Only in InfoPanel
		requestLayout();
		turnierFields = true;
	}

	protected abstract java.util.function.Predicate<ConfigProperty<?>> getKonfigFieldFilter();

}
