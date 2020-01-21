/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.lib.uno.helper.ComponentBase;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public class ConfigSidebarPanel extends BaseSidebarPanel {

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param resourceUrl
	 */
	public ConfigSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl) {
		super(workingSpreadsheet, parentWindow, resourceUrl);
	}

	@Override
	protected ComponentBase newContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		// TODO Auto-generated method stub
		return null;
	}

}
