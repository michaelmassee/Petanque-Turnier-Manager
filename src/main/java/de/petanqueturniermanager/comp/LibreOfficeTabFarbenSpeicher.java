/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 * Speichert die globalen Tab-Farben (Sheet-Register-Farben) im LibreOffice-Konfigurationssystem.
 */
final class LibreOfficeTabFarbenSpeicher {

	private static final Logger logger = LogManager.getLogger(LibreOfficeTabFarbenSpeicher.class);

	private static final String NODE_PATH = "/org.openoffice.Office.Custom.PetanqueTurnierManager/TabFarben";

	/** Reihenfolge und Namen entsprechen exakt den Props in PetanqueTurnierManager.xcs, Gruppe TabFarben. */
	static final List<String> PROPERTY_NAMEN = List.of(
			"Meldeliste", "Teilnehmer", "Spielrunde", "Rangliste", "Direktvergleich",
			"KoTurnierbaum", "Cadrage", "PouleVorrunde", "PouleVorrundenRangliste",
			"KaskadenKo", "SupermeleeTeamPaarungen");

	private final XComponentContext context;

	LibreOfficeTabFarbenSpeicher(XComponentContext context) {
		this.context = context;
	}

	Map<String, Integer> laden() {
		XPropertySet props = null;
		try {
			props = konfiguration(false);
			Map<String, Integer> farben = new LinkedHashMap<>();
			for (String name : PROPERTY_NAMEN) {
				farben.put(name, intWert(props, name, 0));
			}
			return farben;
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Tab-Farben konnten nicht gelesen werden", e);
		} finally {
			dispose(props);
		}
	}

	void speichern(Map<String, Integer> farben) {
		XPropertySet props = null;
		try {
			props = konfiguration(true);
			for (String name : PROPERTY_NAMEN) {
				Integer wert = farben.get(name);
				if (wert != null) {
					props.setPropertyValue(name, wert);
				}
			}
			commit(props);
		} catch (Exception e) {
			throw new IllegalStateException("LibreOffice Tab-Farben konnten nicht gespeichert werden", e);
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

	private static int intWert(XPropertySet props, String name, int defaultWert) {
		try {
			return AnyConverter.toInt(props.getPropertyValue(name));
		} catch (Exception e) {
			logger.debug("Tab-Farben-Property {} nicht lesbar, verwende Default {}", name, defaultWert, e);
			return defaultWert;
		}
	}
}
