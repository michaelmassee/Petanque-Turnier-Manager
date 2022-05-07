/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.info;

import com.sun.star.awt.XWindow;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.BaseSidebarPanel;

/**
 * @author Michael Massee
 *
 */
public class InfoSidebarPanel extends BaseSidebarPanel {

	public InfoSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl,
			XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, resourceUrl, xSidebar);
	}

	@Override
	protected BaseSidebarContent newContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow,
			XSidebar xSidebar) {
		return new InfoSidebarContent(workingSpreadsheet, parentWindow, xSidebar);
	}
}
