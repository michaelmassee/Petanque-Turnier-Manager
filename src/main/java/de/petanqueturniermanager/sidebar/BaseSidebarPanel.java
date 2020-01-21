/**
 * Erstellung 21.01.2020 / Michael Massee
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
public abstract class BaseSidebarPanel extends ComponentBase implements XUIElement {

	private final String resourceUrl;
	private final ComponentBase panel;

	public BaseSidebarPanel(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, String resourceUrl) {
		this.resourceUrl = checkNotNull(resourceUrl);
		panel = newContent(workingSpreadsheet, parentWindow);
	}

	@Override
	public final XFrame getFrame() {
		return null;
	}

	@Override
	public final Object getRealInterface() {
		return panel;
	}

	@Override
	public final String getResourceURL() {
		return resourceUrl;
	}

	@Override
	public final short getType() {
		return UIElementType.TOOLPANEL;
	}

	abstract protected ComponentBase newContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow);

}
