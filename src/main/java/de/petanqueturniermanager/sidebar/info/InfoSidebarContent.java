/**
 * Erstellung 12.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.info;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XControl;
import com.sun.star.awt.XFixedText;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.EventObject;
import com.sun.star.ui.XSidebar;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.sidebar.BaseSidebarContent;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.layout.ControlLayout;
import de.petanqueturniermanager.helper.Lo;

/**
 * Zeigt die installierte Plugin-Version als einzelnes Label.
 *
 * @author Michael Massee
 */
public class InfoSidebarContent extends BaseSidebarContent {

	private XFixedText label;

	public InfoSidebarContent(WorkingSpreadsheet workingSpreadsheet, XWindow parentWindow, XSidebar xSidebar) {
		super(workingSpreadsheet, parentWindow, xSidebar);
	}

	@Override
	protected void felderHinzufuegen() {
		XControl labelControl = GuiFactory.createLabel(getGuiFactoryCreateParam(), getPluginVersion(),
				new Rectangle(0, 0, 200, 20), null);
		if (labelControl == null) {
			return;
		}
		label = Lo.qi(XFixedText.class, labelControl);
		getLayout().addLayout(new ControlLayout(labelControl), 1);
		requestLayout();
	}

	@Override
	protected void felderAktualisieren(ITurnierEvent eventObj) {
		// Version ändert sich zur Laufzeit nicht
	}

	@Override
	protected void onDisposing(EventObject event) {
		label = null;
	}

	String getPluginVersion() {
		String version = ExtensionsHelper.from(getCurrentSpreadsheet().getxContext()).getVersionNummer();
		return version != null ? version : "–";
	}
}
