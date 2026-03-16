package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.ui.XSidebar;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.info.InfoSidebarPanel;

/**
 * Factory für Sidebar-Panels.
 * <p>
 * Die Registrierung beim {@code UIElementFactoryManager} erfolgt deklarativ
 * via {@code registry/org/openoffice/Office/UI/UIElementFactoryManager.xcu}.
 */
public class PetanqueTurnierManagerPanelFactory implements XUIElementFactory, XServiceInfo {

	private static final Logger logger = LogManager.getLogger(PetanqueTurnierManagerPanelFactory.class);

	private static final String IMPLEMENTATION_NAME = PetanqueTurnierManagerPanelFactory.class.getName();
	private static final String SERVICE_NAME = "de.petanqueturniermanager.sidebar.PetanqueTurnierManagerPanelFactory";
	private static final String[] SERVICE_NAMES = { SERVICE_NAME };
	public static final String URL_PREFIX = "private:resource/toolpanel/PetanqueTurnierManagerPanelFactory";

	private final XComponentContext xContext;

	public PetanqueTurnierManagerPanelFactory(final XComponentContext xContext) {
		logger.debug("PetanqueTurnierManagerPanelFactory constructor");
		this.xContext = xContext;
		PetanqueTurnierMngrSingleton.init(xContext);
	}

	@Override
	public XUIElement createUIElement(final String sResourceURL, final PropertyValue[] aArgumentList)
			throws NoSuchElementException, IllegalArgumentException {
		logger.debug("createUIElement {}", sResourceURL);

		if (!sResourceURL.startsWith(URL_PREFIX)) {
			throw new NoSuchElementException(sResourceURL, this);
		}

		XWindow xParentWindow = null;
		XSidebar xSidebar = null;
		for (PropertyValue aValue : aArgumentList) {
			if (aValue.Name.equals("ParentWindow")) {
				xParentWindow = Lo.qi(XWindow.class, aValue.Value);
			} else if (aValue.Name.equals("Sidebar")) {
				xSidebar = Lo.qi(XSidebar.class, aValue.Value);
			}
		}

		try {
			if (xParentWindow != null && xSidebar != null) {
				WorkingSpreadsheet currentSpreadsheet = new WorkingSpreadsheet(xContext);
				logger.debug("Neues InfoSidebarPanel");
				return new InfoSidebarPanel(currentSpreadsheet, xParentWindow, sResourceURL, xSidebar);
			} else {
				logger.error("createUIElement: ParentWindow={}, Sidebar={}", xParentWindow, xSidebar);
			}
		} catch (Exception e) {
			logger.error("Fehler beim Erstellen des Panels '{}': {}", sResourceURL, e.getMessage(), e);
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
		for (final String name : SERVICE_NAMES) {
			if (name.equals(sServiceName)) {
				return true;
			}
		}
		return false;
	}
}
