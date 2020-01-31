package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.lib.uno.helper.Factory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.ui.XSidebar;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.sidebar.config.allgemein.ConfigSidebarPanel;
import de.petanqueturniermanager.sidebar.config.color.ColorSidebarPanel;

/**
 * This is the factory that creates the sidebar panel.
 */
public class PetanqueTurnierManagerPanelFactory implements XUIElementFactory, XServiceInfo {
	private static final Logger logger = LogManager.getLogger(PetanqueTurnierManagerPanelFactory.class);

	public static final String __serviceName = "de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory";
	private static final String msURLhead = "private:resource/toolpanel/PetanqueTurnierManagerPanelFactory";
	private static final String IMPLEMENTATION_NAME = PetanqueTurnierManagerPanelFactory.class.getName();
	private static final String[] SERVICE_NAMES = { __serviceName };

	private final WorkingSpreadsheet currentSpreadsheet;

	// fuer jeden Sheet wird ein Panel erstellt
	public PetanqueTurnierManagerPanelFactory(final XComponentContext xContext) {
		logger.debug("PetanqueTurnierManagerPanelFactory constructor");
		currentSpreadsheet = new WorkingSpreadsheet(xContext);
		PetanqueTurnierMngrSingleton.init(xContext);
	}

	// -----------------------------------------------------------------------------------------------
	/**
	 * kommt zuerst
	 *
	 * @param xRegistryKey
	 * @return
	 */
	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, xRegistryKey);
	}

	/**
	 * Gives a factory for creating the service.<br>
	 * This method is called by the <code>JavaLoader</code><br>
	 *
	 * @return Returns a <code>XSingleServiceFactory</code> for creating the component.<br>
	 * @see com.sun.star.comp.loader.JavaLoader<br>
	 * @param sImplementationName The implementation name of the component.<br>
	 */

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		logger.debug("__getComponentFactory " + sImplementationName);

		XSingleComponentFactory xFactory = null;

		if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
			xFactory = Factory.createComponentFactory(PetanqueTurnierManagerPanelFactory.class, SERVICE_NAMES);
		}
		return xFactory;
	}
	// ----------------------------------------------------------------------------------------------------------

	/**
	 * The main factory method has two parts: - Extract and check some values from the given arguments - Check the sResourceURL and create a panel for it.<br>
	 */
	@Override
	public XUIElement createUIElement(final String sResourceURL, final PropertyValue[] aArgumentList) throws NoSuchElementException, IllegalArgumentException {
		logger.debug("createUIElement " + sResourceURL);

		// Reject all resource URLs that don't have the right prefix.
		if (!sResourceURL.startsWith(msURLhead)) {
			throw new NoSuchElementException(sResourceURL, this);
		}

		// Retrieve the parent window from the given argument list.
		XWindow xParentWindow = null;
		XSidebar xSidebar = null;
		logger.debug("processing " + aArgumentList.length + " arguments");
		for (final PropertyValue aValue : aArgumentList) {
			if (aValue.Name.equals("ParentWindow")) {
				try {
					xParentWindow = (XWindow) AnyConverter.toObject(XWindow.class, aValue.Value);
				} catch (IllegalArgumentException aException) {
					logger.error(aException);
				}
			} else if (aValue.Name.equals("Sidebar")) {
				try {
					xSidebar = (XSidebar) AnyConverter.toObject(XSidebar.class, aValue.Value);
				} catch (IllegalArgumentException aException) {
					logger.error(aException);
				}
			}

		}

		// Create the panel.
		try {
			if (xParentWindow != null && xSidebar != null) {
				final String sElementName = sResourceURL.substring(msURLhead.length() + 1);
				if (sElementName.equals("InfoPanel")) {
					logger.debug("New InfoSidebarPanel");
					return new InfoSidebarPanel(currentSpreadsheet, xParentWindow, sResourceURL, xSidebar);
				} else if (sElementName.equals("ConfigPanel")) {
					logger.debug("New ConfigSidebarPanel");
					return new ConfigSidebarPanel(currentSpreadsheet, xParentWindow, sResourceURL, xSidebar);
				} else if (sElementName.equals("ColorPanel")) {
					logger.debug("New ColorSidebarPanel");
					return new ColorSidebarPanel(currentSpreadsheet, xParentWindow, sResourceURL, xSidebar);
				}
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return null;
	}

	@Override
	public String getImplementationName() {
		return IMPLEMENTATION_NAME;
	}

	@Override
	public String[] getSupportedServiceNames() {
		return SERVICE_NAMES;
	}

	@Override
	public boolean supportsService(final String sServiceName) {
		for (final String sSupportedServiceName : SERVICE_NAMES) {
			if (sSupportedServiceName.equals(sServiceName)) {
				return true;
			}
		}
		return false;
	}
}
