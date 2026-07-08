/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

/**
 * Speichert die Turnier-Startseite-Optionen im LibreOffice-Konfigurationssystem.
 */
final class LibreOfficeStartseiteSpeicher {

	private static final Logger logger = LogManager.getLogger(LibreOfficeStartseiteSpeicher.class);

	private static final String NODE_PATH = "/org.openoffice.Office.Custom.PetanqueTurnierManager/Startseite";
	private static final String PROP_PORT = "Port";
	private static final String PROP_ACTIVE = "Active";
	private static final String PROP_ZOOM = "Zoom";
	/**
	 * @deprecated Nur für den einmaligen Legacy-Import relevant; kann mit {@link #istLegacyImportErledigt()}
	 *             und {@link #importiereLegacy(StartseiteOptionen)} entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String PROP_LEGACY_IMPORTED = "LegacyPropertiesImported";

	private final XComponentContext context;

	LibreOfficeStartseiteSpeicher(XComponentContext context) {
		this.context = context;
	}

	StartseiteOptionen laden() {
		XPropertySet props = null;
		try {
			props = konfiguration(false);
			return new StartseiteOptionen(
					portWert(props, PROP_PORT, GlobalProperties.STARTSEITE_DEFAULT_PORT),
					booleanWert(props, PROP_ACTIVE, false),
					zoomWert(props, PROP_ZOOM, GlobalProperties.DEFAULT_ZOOM));
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Turnier-Startseite-Optionen konnten nicht gelesen werden", e);
		} finally {
			dispose(props);
		}
	}

	/**
	 * @deprecated Nur für den einmaligen Legacy-Import relevant; kann entfernt werden, sobald
	 *             davon auszugehen ist, dass keine Alt-Installation mehr importiert werden muss.
	 */
	@Deprecated(forRemoval = true)
	boolean istLegacyImportErledigt() {
		XPropertySet props = null;
		try {
			props = konfiguration(false);
			return booleanWert(props, PROP_LEGACY_IMPORTED, false);
		} catch (Exception e) {
			logger.warn("Turnier-Startseite-Legacy-Importstatus konnte nicht gelesen werden", e);
			return false;
		} finally {
			dispose(props);
		}
	}

	void speichern(StartseiteOptionen optionen) {
		XPropertySet props = null;
		try {
			props = konfiguration(true);
			props.setPropertyValue(PROP_PORT, Integer.valueOf(optionen.port()));
			props.setPropertyValue(PROP_ACTIVE, Boolean.valueOf(optionen.aktiv()));
			props.setPropertyValue(PROP_ZOOM, Integer.valueOf(optionen.zoom()));
			props.setPropertyValue(PROP_LEGACY_IMPORTED, Boolean.TRUE);
			commit(props);
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Turnier-Startseite-Optionen konnten nicht gespeichert werden", e);
		} finally {
			dispose(props);
		}
	}

	/**
	 * @deprecated Nur für den einmaligen Legacy-Import relevant; kann entfernt werden, sobald
	 *             davon auszugehen ist, dass keine Alt-Installation mehr importiert werden muss.
	 */
	@Deprecated(forRemoval = true)
	void importiereLegacy(StartseiteOptionen legacyOptionen) {
		speichern(legacyOptionen);
	}

	private XPropertySet konfiguration(boolean schreiben) throws com.sun.star.uno.Exception {
		if (context == null) {
			throw new com.sun.star.uno.Exception("Kein LibreOffice-Kontext vorhanden", this);
		}
		XMultiComponentFactory factory = context.getServiceManager();
		Object provider = factory.createInstanceWithContext("com.sun.star.configuration.ConfigurationProvider", context);
		XMultiServiceFactory serviceFactory = UnoRuntime.queryInterface(XMultiServiceFactory.class, provider);
		PropertyValue nodePath = new PropertyValue();
		nodePath.Name = "nodepath";
		nodePath.Value = NODE_PATH;
		String service = schreiben
				? "com.sun.star.configuration.ConfigurationUpdateAccess"
				: "com.sun.star.configuration.ConfigurationAccess";
		Object config = serviceFactory.createInstanceWithArguments(service, new Object[] { nodePath });
		XPropertySet props = UnoRuntime.queryInterface(XPropertySet.class, config);
		if (props == null) {
			throw new com.sun.star.uno.Exception("Konfiguration hat kein XPropertySet", this);
		}
		return props;
	}

	private static void commit(XPropertySet props) throws com.sun.star.uno.Exception {
		XChangesBatch batch = UnoRuntime.queryInterface(XChangesBatch.class, props);
		if (batch == null) {
			throw new com.sun.star.uno.Exception("Konfiguration hat kein XChangesBatch", props);
		}
		batch.commitChanges();
	}

	private static void dispose(XPropertySet props) {
		if (props == null) {
			return;
		}
		XComponent component = UnoRuntime.queryInterface(XComponent.class, props);
		if (component != null) {
			component.dispose();
		}
	}

	private static boolean booleanWert(XPropertySet props, String name, boolean defaultWert) {
		try {
			return AnyConverter.toBoolean(props.getPropertyValue(name));
		} catch (Exception e) {
			logger.debug("Boolean-Property {} nicht lesbar, verwende Default {}", name, defaultWert, e);
			return defaultWert;
		}
	}

	private static int portWert(XPropertySet props, String name, int defaultWert) {
		try {
			int port = AnyConverter.toInt(props.getPropertyValue(name));
			return GlobalProperties.normierePort(port, defaultWert);
		} catch (Exception e) {
			logger.debug("Port-Property {} nicht lesbar, verwende Default {}", name, defaultWert, e);
			return defaultWert;
		}
	}

	private static int zoomWert(XPropertySet props, String name, int defaultWert) {
		try {
			return AnyConverter.toInt(props.getPropertyValue(name));
		} catch (Exception e) {
			logger.debug("Zoom-Property {} nicht lesbar, verwende Default {}", name, defaultWert, e);
			return defaultWert;
		}
	}
}
