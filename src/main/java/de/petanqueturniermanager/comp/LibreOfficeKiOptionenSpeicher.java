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

import de.petanqueturniermanager.ki.KiOptionen;

final class LibreOfficeKiOptionenSpeicher {

    private static final Logger logger = LogManager.getLogger(LibreOfficeKiOptionenSpeicher.class);

    private static final String NODE_PATH = "/org.openoffice.Office.Custom.PetanqueTurnierManager/KiAssistent";
    private static final String PROP_API_KEY = "ApiKey";
    private static final String PROP_MODEL = "Model";
    private static final String PROP_BASE_URL = "BaseUrl";
    private static final String PROP_TIMEOUT = "TimeoutSeconds";
    private static final String PROP_FULL_CONTEXT = "FullContext";

    private final XComponentContext context;

    LibreOfficeKiOptionenSpeicher(XComponentContext context) {
        this.context = context;
    }

    KiOptionen laden() {
        XPropertySet props = null;
        try {
            props = konfiguration(false);
            return new KiOptionen(
                    stringWert(props, PROP_API_KEY),
                    stringWert(props, PROP_MODEL),
                    stringWert(props, PROP_BASE_URL),
                    intWert(props, PROP_TIMEOUT, KiOptionen.DEFAULT_TIMEOUT_SEKUNDEN),
                    booleanWert(props, PROP_FULL_CONTEXT, true));
        } catch (Exception e) {
            throw new IllegalStateException("LibreOffice KI-Optionen konnten nicht gelesen werden", e);
        } finally {
            dispose(props);
        }
    }

    void speichern(KiOptionen optionen) {
        XPropertySet props = null;
        try {
            props = konfiguration(true);
            props.setPropertyValue(PROP_API_KEY, optionen.apiKey());
            props.setPropertyValue(PROP_MODEL, optionen.model());
            props.setPropertyValue(PROP_BASE_URL, optionen.baseUrl());
            props.setPropertyValue(PROP_TIMEOUT, Integer.valueOf(optionen.timeoutSekunden()));
            props.setPropertyValue(PROP_FULL_CONTEXT, Boolean.valueOf(optionen.vollstaendigenKontextSenden()));
            commit(props);
        } catch (Exception e) {
            throw new IllegalStateException("LibreOffice KI-Optionen konnten nicht gespeichert werden", e);
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
        XComponent component = props == null ? null : UnoRuntime.queryInterface(XComponent.class, props);
        if (component != null) {
            component.dispose();
        }
    }

    private static boolean booleanWert(XPropertySet props, String name, boolean defaultWert) {
        try {
            return AnyConverter.toBoolean(props.getPropertyValue(name));
        } catch (Exception e) {
            logger.debug("Boolean-Property {} nicht lesbar", name, e);
            return defaultWert;
        }
    }

    private static int intWert(XPropertySet props, String name, int defaultWert) {
        try {
            return AnyConverter.toInt(props.getPropertyValue(name));
        } catch (Exception e) {
            logger.debug("Integer-Property {} nicht lesbar", name, e);
            return defaultWert;
        }
    }

    private static String stringWert(XPropertySet props, String name) {
        try {
            return AnyConverter.toString(props.getPropertyValue(name)).trim();
        } catch (Exception e) {
            logger.debug("String-Property {} nicht lesbar", name, e);
            return "";
        }
    }
}
