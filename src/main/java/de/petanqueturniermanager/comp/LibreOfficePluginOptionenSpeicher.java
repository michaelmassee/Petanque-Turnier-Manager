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
	private static final String PROP_AUTO_UPDATE_DIALOG_STARTUP = "AutoUpdateDialogStartup";
	/**
	 * @deprecated Nur für den einmaligen Legacy-Import relevant; kann mit {@link #istLegacyImportErledigt()}
	 *             und {@link #importiereLegacy(PluginOptionen)} entfernt werden.
	 */
	@Deprecated(forRemoval = true)
	private static final String PROP_LEGACY_IMPORTED = "LegacyPropertiesImported";

	private final XComponentContext context;

	LibreOfficePluginOptionenSpeicher(XComponentContext context) {
		this.context = context;
	}

	PluginOptionen laden() {
		XPropertySet props = null;
		try {
			props = konfiguration(false);
			return new PluginOptionen(
					booleanWert(props, PROP_AUTOSAVE, false),
					booleanWert(props, PROP_BACKUP, false),
					booleanWert(props, PROP_NEW_VERSION_CHECK, false),
					booleanWert(props, PROP_PROCESSBOX_SHOW, true),
					booleanWert(props, PROP_PROCESSBOX_CLOSE, true),
					booleanWert(props, PROP_PERFORMANCE_LOGGING, false),
					stringWert(props, PROP_LOG_LEVEL),
					booleanWert(props, PROP_AUTO_UPDATE_DIALOG_STARTUP, true));
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Plugin-Optionen konnten nicht gelesen werden", e);
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
			logger.warn("Legacy-Importstatus konnte nicht gelesen werden", e);
			return false;
		} finally {
			dispose(props);
		}
	}

	void speichern(PluginOptionen optionen) {
		XPropertySet props = null;
		try {
			props = konfiguration(true);
			props.setPropertyValue(PROP_AUTOSAVE, Boolean.valueOf(optionen.autosave()));
			props.setPropertyValue(PROP_BACKUP, Boolean.valueOf(optionen.backup()));
			props.setPropertyValue(PROP_NEW_VERSION_CHECK, Boolean.valueOf(optionen.newVersionCheck()));
			props.setPropertyValue(PROP_PROCESSBOX_SHOW, Boolean.valueOf(optionen.prozessBoxAutomatischAnzeigen()));
			props.setPropertyValue(PROP_PROCESSBOX_CLOSE, Boolean.valueOf(optionen.prozessBoxAutomatischSchliessen()));
			props.setPropertyValue(PROP_PERFORMANCE_LOGGING, Boolean.valueOf(optionen.performanceLogging()));
			props.setPropertyValue(PROP_LOG_LEVEL, optionen.logLevel());
			props.setPropertyValue(PROP_AUTO_UPDATE_DIALOG_STARTUP, Boolean.valueOf(optionen.autoUpdateDialogBeimStart()));
			props.setPropertyValue(PROP_LEGACY_IMPORTED, Boolean.TRUE);
			commit(props);
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Plugin-Optionen konnten nicht gespeichert werden", e);
		} finally {
			dispose(props);
		}
	}

	/**
	 * Übernimmt die aus den Legacy-Properties gelesenen Optionen einmalig in die
	 * LibreOffice-Konfiguration und markiert den Import als erledigt.
	 *
	 * @deprecated Nur für den einmaligen Legacy-Import relevant; kann entfernt werden, sobald
	 *             davon auszugehen ist, dass keine Alt-Installation mehr importiert werden muss.
	 */
	@Deprecated(forRemoval = true)
	void importiereLegacy(PluginOptionen legacyOptionen) {
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

	/**
	 * Gibt die von {@link #konfiguration(boolean)} erzeugte Konfigurations-View wieder frei.
	 * Ohne dispose sammelt configmgr die View-Objekte bis zum GC an.
	 */
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

	private static String stringWert(XPropertySet props, String name) {
		try {
			return AnyConverter.toString(props.getPropertyValue(name)).trim().toLowerCase();
		} catch (Exception e) {
			logger.debug("String-Property {} nicht lesbar, verwende leeren Wert", name, e);
			return "";
		}
	}
}
