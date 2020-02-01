/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.color;

import java.util.function.Predicate;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.konfigdialog.ConfigProperty;
import de.petanqueturniermanager.konfigdialog.ConfigPropertyType;
import de.petanqueturniermanager.sidebar.config.BaseConfigSidebarContent;

/**
 * @author Michael Massee
 *
 */
public class ColorSidebarContent extends BaseConfigSidebarContent {
	public static final Predicate<ConfigProperty<?>> COLOR_FILTER = konfigprop -> konfigprop.getType() == ConfigPropertyType.COLOR;

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param xSidebar
	 */
	public ColorSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {
		return COLOR_FILTER;
	}
}
