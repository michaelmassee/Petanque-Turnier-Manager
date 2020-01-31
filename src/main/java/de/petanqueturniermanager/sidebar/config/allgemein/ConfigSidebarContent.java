/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.allgemein;

import java.util.function.Predicate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
public class ConfigSidebarContent extends BaseConfigSidebarContent {
	static final Logger logger = LogManager.getLogger(ConfigSidebarContent.class);

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param xSidebar
	 */
	public ConfigSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	@Override
	protected Predicate<ConfigProperty<?>> getKonfigFieldFilter() {

		return new java.util.function.Predicate<ConfigProperty<?>>() {
			@Override
			public boolean test(ConfigProperty<?> konfigprop) {
				return konfigprop.getType() != ConfigPropertyType.COLOR;
			}
		};
	}

}
