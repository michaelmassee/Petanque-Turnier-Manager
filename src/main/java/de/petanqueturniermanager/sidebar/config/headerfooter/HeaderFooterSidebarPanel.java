/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config.headerfooter;

import com.sun.star.awt.XWindow;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.BaseSidebarPanel;

/**
 * @author Michael Massee
 *
 */
public class HeaderFooterSidebarPanel extends BaseSidebarPanel {

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 * @param resourceUrl
	 */
	public HeaderFooterSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, resourceUrl, xSidebar);
	}

	@Override
	protected ComponentBase newContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		return new HeaderFooterSidebarContent(workingSpreadsheet, parentWindow, xSidebar);
	}

}
