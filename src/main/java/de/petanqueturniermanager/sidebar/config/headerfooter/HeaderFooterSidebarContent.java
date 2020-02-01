/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.headerfooter;

import java.util.function.Predicate;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.HeaderFooterConfigProperty;
import de.petanqueturniermanager.sidebar.config.BaseConfigSidebarContent;

/**
 * @author Michael Massee
 *
 */
public class HeaderFooterSidebarContent extends BaseConfigSidebarContent {
	public static final Predicate<ConfigProperty<?>> HEADERFOOTER_FILTER = konfigprop -> konfigprop instanceof HeaderFooterConfigProperty;

	public HeaderFooterSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return HEADERFOOTER_FILTER;
	}
}
