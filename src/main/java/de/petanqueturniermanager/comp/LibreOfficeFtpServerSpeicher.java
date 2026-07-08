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
 * Speichert die zentrale FTP/SFTP-Server-Liste (als JSON) im LibreOffice-Konfigurationssystem.
 */
final class LibreOfficeFtpServerSpeicher {

	private static final Logger logger = LogManager.getLogger(LibreOfficeFtpServerSpeicher.class);

	private static final String NODE_PATH = "/org.openoffice.Office.Custom.PetanqueTurnierManager/FtpServer";
	private static final String PROP_SERVERS_JSON = "ServersJson";

	private final XComponentContext context;

	LibreOfficeFtpServerSpeicher(XComponentContext context) {
		this.context = context;
	}

	String laden() {
		XPropertySet props = null;
		try {
			props = konfiguration(false);
			return stringWert(props, PROP_SERVERS_JSON);
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice FTP-Server-Liste konnte nicht gelesen werden", e);
		} finally {
			dispose(props);
		}
	}

	void speichern(String serversJson) {
		XPropertySet props = null;
		try {
			props = konfiguration(true);
			props.setPropertyValue(PROP_SERVERS_JSON, serversJson == null ? "" : serversJson);
			commit(props);
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice FTP-Server-Liste konnte nicht gespeichert werden", e);
		} finally {
			dispose(props);
		}
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

	private static String stringWert(XPropertySet props, String name) {
		try {
			return AnyConverter.toString(props.getPropertyValue(name)).trim();
		} catch (Exception e) {
			logger.debug("String-Property {} nicht lesbar, verwende leeren Wert", name, e);
			return "";
		}
	}
}
