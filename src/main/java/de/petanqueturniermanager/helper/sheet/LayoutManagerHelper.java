/**
 * Erstellung 14.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XLayoutManager;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.ui.XUIElement;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * @author Michael Massee
 *
 */
public class LayoutManagerHelper {

	private static final Logger logger = LogManager.getLogger(LayoutManagerHelper.class);

	// code snippet wird nicht verwendet

	// XFrame xFrame = frame.getXFrame();
	// XPropertySet xPropSet = (XPropertySet) Lo.qi(XPropertySet.class, xFrame);
	// XLayoutManager xLayoutManager = (XLayoutManager) Lo.qi(XLayoutManager.class,
	// xPropSet.getPropertyValue("LayoutManager"));

	// public static XUIElement getInfoPanel(XComponentContext xContext) {
	//
	// // XModuleUIConfigurationManagerSupplier suppl = UNO
	// // .XModuleUIConfigurationManagerSupplier(UNO.createUNOService(
	// // "com.sun.star.ui.ModuleUIConfigurationManagerSupplier"));
	// // XUIConfigurationManager cfgMgr = UNO.XUIConfigurationManager(
	// // suppl.getUIConfigurationManager("com.sun.star.text.TextDocument"));
	// // XIndexAccess menubar = UNO
	// // .XIndexAccess(cfgMgr.getSettings(settingsUrl, true));
	//
	// // XUIConfigurationManager
	//
	// return null;
	//
	// }

	public static void listToolbars(XComponentContext xContext) {

		// public static XUIElement getInfoPanel_old(XComponentContext xContext) {
		XDesktop desktop = DocumentHelper.getCurrentDesktop(xContext);
		if (desktop == null) {
			return;
		}
		XFrame frame = desktop.getCurrentFrame();
		if (frame == null) {
			return;
		}
		XPropertySet propSet = Lo.qi(XPropertySet.class, frame);
		if (propSet == null) {
			return;
		}
		XLayoutManager layoutManager;
		try {
			layoutManager = Lo.qi(XLayoutManager.class, propSet.getPropertyValue("LayoutManager"));
		} catch (UnknownPropertyException | WrappedTargetException e) {
			return;
		}

		if (layoutManager == null) {
			return;
		}

		XUIElement[] elements = layoutManager.getElements();
		for (XUIElement el : elements) {
			String resourceURL = el.getResourceURL();
			logger.debug(resourceURL);
		}

		// XUIElement infoSidebarPanel = layoutManager.getElement(msURLhead + "/InfoPanel");
		// return infoSidebarPanel;
	}

}
