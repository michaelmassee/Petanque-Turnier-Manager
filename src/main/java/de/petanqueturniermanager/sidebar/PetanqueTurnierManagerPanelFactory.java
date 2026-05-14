package de.petanqueturniermanager.sidebar;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XSingleComponentFactory;
import com.sun.star.registry.XRegistryKey;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.ui.XSidebar;
import com.sun.star.ui.XUIElement;
import com.sun.star.ui.XUIElementFactory;
import com.sun.star.uno.XComponentContext;

import com.sun.star.lib.uno.helper.Factory;

import de.petanqueturniermanager.comp.PetanqueTurnierMngrSingleton;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.sidebar.info.InfoSidebarPanel;
import de.petanqueturniermanager.sidebar.sheets.SheetListeSidebarPanel;

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

	public static XSingleComponentFactory __getComponentFactory(String sImplementationName) {
		if (sImplementationName.equals(IMPLEMENTATION_NAME)) {
			return Factory.createComponentFactory(PetanqueTurnierManagerPanelFactory.class, SERVICE_NAMES);
		}
		return null;
	}

	public static boolean __writeRegistryServiceInfo(XRegistryKey xRegistryKey) {
		return Factory.writeRegistryServiceInfo(IMPLEMENTATION_NAME, SERVICE_NAMES, xRegistryKey);
	}

	private final XComponentContext xContext;

	public PetanqueTurnierManagerPanelFactory(final XComponentContext xContext) {
		logger.info("PetanqueTurnierManagerPanelFactory konstruiert");
		this.xContext = xContext;
		PetanqueTurnierMngrSingleton.init(xContext);
	}

	@Override
	public XUIElement createUIElement(final String sResourceURL, final PropertyValue[] aArgumentList)
			throws NoSuchElementException, IllegalArgumentException {
		logger.info("createUIElement: {}", sResourceURL);

		if (!sResourceURL.startsWith(URL_PREFIX)) {
			throw new NoSuchElementException(sResourceURL, this);
		}

		XWindow xParentWindow = null;
		XSidebar xSidebar = null;
		XFrame xFrame = null;
		for (PropertyValue aValue : aArgumentList) {
			if (aValue.Name.equals("ParentWindow")) {
				xParentWindow = Lo.qi(XWindow.class, aValue.Value);
			} else if (aValue.Name.equals("Sidebar")) {
				xSidebar = Lo.qi(XSidebar.class, aValue.Value);
			} else if (aValue.Name.equals("Frame")) {
				xFrame = Lo.qi(XFrame.class, aValue.Value);
			}
		}

		try {
			if (xParentWindow != null && xSidebar != null) {
				if (sResourceURL.length() <= URL_PREFIX.length()) {
					logger.error("createUIElement: URL zu kurz: {}", sResourceURL);
					throw new NoSuchElementException(sResourceURL, this);
				}
				WorkingSpreadsheet currentSpreadsheet = erzeugeWorkingSpreadsheetFuerFrame(xFrame);
				String panelId = sResourceURL.substring(URL_PREFIX.length() + 1);
				logger.debug("createUIElement: panelId={}", panelId);
				return switch (panelId) {
					case "InfoPanel" ->
						new InfoSidebarPanel(currentSpreadsheet, xParentWindow, sResourceURL, xSidebar);
					case "SheetListePanel" ->
						new SheetListeSidebarPanel(currentSpreadsheet, xParentWindow, sResourceURL, xSidebar);
					default -> {
						logger.error("createUIElement: Unbekannte panelId '{}'", panelId);
						throw new NoSuchElementException(sResourceURL, this);
					}
				};
			} else {
				logger.error("createUIElement: ParentWindow={}, Sidebar={}", xParentWindow, xSidebar);
			}
		} catch (NoSuchElementException e) {
			throw e;
		} catch (Exception e) {
			logger.error("Fehler beim Erstellen des Panels '{}': {}", sResourceURL, e.getMessage(), e);
		}

		return null;
	}

	/**
	 * Erzeugt das {@link WorkingSpreadsheet} aus dem Frame, an dem die Sidebar tatsächlich hängt.
	 * <p>
	 * Wichtig: Der no-arg-Konstruktor von {@link WorkingSpreadsheet} ermittelt das Desktop-aktive
	 * Dokument – das ist beim Erzeugen des Sidebar-Panels für ein NEU geöffnetes Calc-Fenster
	 * oft noch das zuvor aktive (Turnier-)Dokument. LibreOffice gibt uns aber das Argument
	 * {@code "Frame"} mit (siehe {@code sfx2/source/sidebar/SidebarController.cxx}), woraus wir
	 * Doc und View für genau dieses Fenster ableiten.
	 */
	private WorkingSpreadsheet erzeugeWorkingSpreadsheetFuerFrame(XFrame xFrame) {
		if (xFrame != null) {
			XController controller = xFrame.getController();
			if (controller != null) {
				XModel xModel = controller.getModel();
				XSpreadsheetDocument xDoc = Lo.qi(XSpreadsheetDocument.class, xModel);
				if (xDoc != null) {
					return new WorkingSpreadsheet(xContext, xDoc);
				}
			}
			logger.debug("erzeugeWorkingSpreadsheetFuerFrame: Frame ohne Spreadsheet-Controller – Fallback");
		}
		return new WorkingSpreadsheet(xContext);
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
