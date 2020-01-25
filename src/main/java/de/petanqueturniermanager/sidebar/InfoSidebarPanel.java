/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.lib.uno.helper.ComponentBase;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public class InfoSidebarPanel extends BaseSidebarPanel {

	public InfoSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl) {
		super(workingSpreadsheet, parentWindow, resourceUrl);
	}

	@Override
	protected ComponentBase newContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		return new InfoSidebarContent(workingSpreadsheet, parentWindow);
	}
}