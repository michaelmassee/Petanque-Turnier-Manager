/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.allgemein;

import java.util.function.Predicate;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.sidebar.config.BaseConfigSidebarContent;
import de.petanqueturniermanager.sidebar.config.color.ColorSidebarContent;
import de.petanqueturniermanager.sidebar.config.headerfooter.HeaderFooterSidebarContent;

/**
 * @author Michael Massee
 *
 */
public class ConfigSidebarContent extends BaseConfigSidebarContent {
	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param xSidebar
	 */
	public ConfigSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	/**
	 * hier wird der rest angezeigt
	 */
	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return ColorSidebarContent.COLOR_FILTER.negate().and(HeaderFooterSidebarContent.HEADERFOOTER_FILTER.negate());
	}

}
