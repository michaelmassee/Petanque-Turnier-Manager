/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.XWindow;
import com.sun.star.frame.XFrame;
import com.sun.star.lib.uno.helper.ComponentBase;
import com.sun.star.ui.UIElementType;
import com.sun.star.ui.XUIElement;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public class InfoSidebarPanel extends ComponentBase implements XUIElement {
	private final String resourceUrl;
	private final InfoSidebarContent panel;

	public InfoSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl) {
		this.resourceUrl = checkNotNull(resourceUrl);
		panel = new InfoSidebarContent(workingSpreadsheet, parentWindow);
	}

	@Override
	public XFrame getFrame() {
		return null;
	}

	@Override
	public Object getRealInterface() {
		return panel;
	}

	@Override
	public String getResourceURL() {
		return resourceUrl;
	}

	@Override
	public short getType() {
		return UIElementType.TOOLPANEL;
	}

}
