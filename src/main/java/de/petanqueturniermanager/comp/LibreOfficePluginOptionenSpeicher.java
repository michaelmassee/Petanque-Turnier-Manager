/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

/**
 * Speichert Plugin-Optionen im LibreOffice-Konfigurationssystem.
 */
final class LibreOfficePluginOptionenSpeicher {

	private static final Logger logger = LogManager.getLogger(LibreOfficePluginOptionenSpeicher.class);

	private static final String NODE_PATH = "/org.openoffice.Office.Custom.PetanqueTurnierManager/Settings";
	private static final String PROP_AUTOSAVE = "Autosave";
	private static final String PROP_BACKUP = "Backup";
	private static final String PROP_NEW_VERSION_CHECK = "NewVersionCheck";
	private static final String PROP_PROCESSBOX_SHOW = "ProcessBoxAutomaticallyShow";
	private static final String PROP_PROCESSBOX_CLOSE = "ProcessBoxAutomaticallyClose";
	private static final String PROP_PERFORMANCE_LOGGING = "PerformanceLogging";
	private static final String PROP_LOG_LEVEL = "LogLevel";
	private static final String PROP_LEGACY_IMPORTED = "LegacyPropertiesImported";

	private final XComponentContext context;

	LibreOfficePluginOptionenSpeicher(XComponentContext context) {
		this.context = context;
	}

	PluginOptionen laden() {
		try {
			XPropertySet props = konfiguration(false);
			return new PluginOptionen(
					booleanWert(props, PROP_AUTOSAVE, false),
					booleanWert(props, PROP_BACKUP, false),
					booleanWert(props, PROP_NEW_VERSION_CHECK, false),
					booleanWert(props, PROP_PROCESSBOX_SHOW, true),
					booleanWert(props, PROP_PROCESSBOX_CLOSE, true),
					booleanWert(props, PROP_PERFORMANCE_LOGGING, false),
					stringWert(props, PROP_LOG_LEVEL));
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Plugin-Optionen konnten nicht gelesen werden", e);
		}
	}

	boolean istLegacyImportErledigt() {
		try {
			return booleanWert(konfiguration(false), PROP_LEGACY_IMPORTED, false);
		} catch (Exception e) {
			logger.warn("Legacy-Importstatus konnte nicht gelesen werden", e);
			return false;
		}
	}

	void speichern(PluginOptionen optionen) {
		try {
			XPropertySet props = konfiguration(true);
			props.setPropertyValue(PROP_AUTOSAVE, Boolean.valueOf(optionen.autosave()));
			props.setPropertyValue(PROP_BACKUP, Boolean.valueOf(optionen.backup()));
			props.setPropertyValue(PROP_NEW_VERSION_CHECK, Boolean.valueOf(optionen.newVersionCheck()));
			props.setPropertyValue(PROP_PROCESSBOX_SHOW, Boolean.valueOf(optionen.prozessBoxAutomatischAnzeigen()));
			props.setPropertyValue(PROP_PROCESSBOX_CLOSE, Boolean.valueOf(optionen.prozessBoxAutomatischSchliessen()));
			props.setPropertyValue(PROP_PERFORMANCE_LOGGING, Boolean.valueOf(optionen.performanceLogging()));
			props.setPropertyValue(PROP_LOG_LEVEL, optionen.logLevel());
			props.setPropertyValue(PROP_LEGACY_IMPORTED, Boolean.TRUE);
			commit(props);
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Plugin-Optionen konnten nicht gespeichert werden", e);
		}
	}

	void importiereLegacy(Map<String, String> legacy, PluginOptionen defaults) {
		speichern(new PluginOptionen(
				booleanAusLegacy(legacy, "autosave", defaults.autosave()),
				booleanAusLegacy(legacy, "backup", defaults.backup()),
				booleanAusLegacy(legacy, "newversioncheck", defaults.newVersionCheck()),
				booleanAusLegacy(legacy, "prozessbox.automatisch.anzeigen", defaults.prozessBoxAutomatischAnzeigen()),
				booleanAusLegacy(legacy, "prozessbox.automatisch.schliessen", defaults.prozessBoxAutomatischSchliessen()),
				booleanAusLegacy(legacy, "performance.logging", defaults.performanceLogging()),
				legacy.getOrDefault("loglevel", defaults.logLevel())));
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

	private static boolean booleanAusLegacy(Map<String, String> legacy, String key, boolean defaultWert) {
		String wert = legacy.get(key);
		if (wert == null || wert.isBlank()) {
			return defaultWert;
		}
		return Boolean.parseBoolean(wert.trim());
	}

	private static boolean booleanWert(XPropertySet props, String name, boolean defaultWert) {
		try {
			return AnyConverter.toBoolean(props.getPropertyValue(name));
		} catch (Exception e) {
			return defaultWert;
		}
	}

	private static String stringWert(XPropertySet props, String name) {
		try {
			return AnyConverter.toString(props.getPropertyValue(name)).trim().toLowerCase();
		} catch (Exception e) {
			return "";
		}
	}
}
