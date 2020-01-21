/**
 * Erstellung 21.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;

/**
 * @author Michael Massee
 *
 */
public class ConfigSidebarContent extends BaseSidebarContent {

	/**
	 * @param workingSpreadsheet
	 * @param parentWindow
	 */
	public ConfigSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow) {
		super(workingSpreadsheet, parentWindow);
	}

	@Override
	protected void disposing(EventObject event) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void updateFields(ITurnierEvent eventObj) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void initFields() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void addFields() {
		// TODO Auto-generated method stub
	}
}
