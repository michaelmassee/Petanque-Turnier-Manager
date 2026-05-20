package de.petanqueturniermanager.comp;

import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XHierarchicalNameAccess;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.helper.Lo;

/**
 * Schreibt beim ersten Plugin-Start einen Banner-Block mit allen relevanten
 * Laufzeit-Parametern ins Log. Wird genau einmal aus
 * {@link PetanqueTurnierMngrSingleton#init} gerufen — Idempotenz übernimmt das
 * dortige {@code didRun}-Flag, hier ist keine eigene Schutzlogik nötig.
 */
public final class StartupInfoLogger {

    private static final Logger logger = LogManager.getLogger(StartupInfoLogger.class);

    private static final String TRENNER = "=================================================================";
    private static final String UNBEKANNT = "?";

    private StartupInfoLogger() {
    }

    public static void logStartupInfo(XComponentContext context) {
        try {
            String banner = baueBanner(context);
            logger.info(banner);
            logger.info("[STARTUP-TIMING] JVM-Uptime beim Plugin-Init: {} ms", StartupClock.uptimeMs());
        } catch (Exception e) {
            // Banner darf den Startup niemals scheitern lassen
            logger.warn("Startup-Banner konnte nicht geschrieben werden: {}", e.getMessage(), e);
        }
    }

    private static String baueBanner(XComponentContext context) {
        String[] loProdukt = ermittleLibreOfficeProdukt(context);
        return String.join("\n",
                "",
                TRENNER,
                " Pétanque-Turnier-Manager — Startup",
                TRENNER,
                zeile("Plugin-Version",   ermittlePluginVersion(context)),
                zeile("LibreOffice",      loProdukt[0] + " " + loProdukt[1]),
                zeile("Java",             ermittleJava()),
                zeile("OS",               ermittleOs()),
                zeile("Locale (JVM)",     Locale.getDefault().toString()),
                zeile("Locale (LO)",      ermittleLoLocale(context)),
                zeile("User-Home",        getProp("user.home")),
                zeile("User-Profile",     ermittleUserProfile(context)),
                zeile("Working-Dir",      getProp("user.dir")),
                zeile("Heap (max/total)", ermittleHeap()),
                zeile("File-Encoding",    getProp("file.encoding")),
                TRENNER);
    }

    private static String zeile(String label, String wert) {
        // Pad Label auf 18 Zeichen für saubere Spaltenausrichtung
        return String.format(" %-18s: %s", label, wert);
    }

    private static String ermittlePluginVersion(XComponentContext context) {
        try {
            String v = ExtensionsHelper.from(context).getVersionNummer();
            return v != null ? v : UNBEKANNT;
        } catch (Exception e) {
            return UNBEKANNT;
        }
    }

    /** Liefert {@code [ooName, ooSetupVersionAboutBox]} bzw. Fallbacks. */
    private static String[] ermittleLibreOfficeProdukt(XComponentContext context) {
        try {
            XMultiServiceFactory msf = Lo.qi(XMultiServiceFactory.class,
                    context.getServiceManager().createInstanceWithContext(
                            "com.sun.star.configuration.ConfigurationProvider", context));
            PropertyValue pv = new PropertyValue("nodepath", 0,
                    "/org.openoffice.Setup/Product", PropertyState.DIRECT_VALUE);
            Object acc = msf.createInstanceWithArguments(
                    "com.sun.star.configuration.ConfigurationAccess", new Object[] { pv });
            XHierarchicalNameAccess hna = Lo.qi(XHierarchicalNameAccess.class, acc);
            String name = sicherString(hna, "ooName", "LibreOffice");
            String version = sicherString(hna, "ooSetupVersionAboutBox", UNBEKANNT);
            return new String[] { name, version };
        } catch (Exception e) {
            return new String[] { "LibreOffice", UNBEKANNT };
        }
    }

    private static String ermittleLoLocale(XComponentContext context) {
        try {
            XMultiServiceFactory msf = Lo.qi(XMultiServiceFactory.class,
                    context.getServiceManager().createInstanceWithContext(
                            "com.sun.star.configuration.ConfigurationProvider", context));
            PropertyValue pv = new PropertyValue("nodepath", 0,
                    "/org.openoffice.Setup/L10N", PropertyState.DIRECT_VALUE);
            Object acc = msf.createInstanceWithArguments(
                    "com.sun.star.configuration.ConfigurationAccess", new Object[] { pv });
            return sicherString(Lo.qi(XHierarchicalNameAccess.class, acc), "ooLocale", UNBEKANNT);
        } catch (Exception e) {
            return UNBEKANNT;
        }
    }

    private static String sicherString(XHierarchicalNameAccess hna, String name, String fallback) {
        if (hna == null) {
            return fallback;
        }
        try {
            Object v = hna.getByHierarchicalName(name);
            return v != null ? v.toString() : fallback;
        } catch (Exception e) {
            return fallback;
        }
    }

    private static String ermittleJava() {
        return getProp("java.version") + " (" + getProp("java.vendor") + ")";
    }

    private static String ermittleOs() {
        return getProp("os.name") + " " + getProp("os.version") + " (" + getProp("os.arch") + ")";
    }

    private static String ermittleHeap() {
        Runtime rt = Runtime.getRuntime();
        long mb = 1024L * 1024L;
        return (rt.maxMemory() / mb) + " MB / " + (rt.totalMemory() / mb) + " MB";
    }

    /**
     * LibreOffice-User-Profile-Pfad via {@code com.sun.star.util.PathSettings},
     * Property {@code UserConfig}. Liefert eine file://-URL — wird hier roh
     * geloggt, kein zusätzliches Umrechnen.
     */
    private static String ermittleUserProfile(XComponentContext context) {
        try {
            Object pathSettings = context.getServiceManager()
                    .createInstanceWithContext("com.sun.star.util.PathSettings", context);
            com.sun.star.beans.XPropertySet props = Lo.qi(com.sun.star.beans.XPropertySet.class, pathSettings);
            if (props == null) {
                return UNBEKANNT;
            }
            Object v = props.getPropertyValue("UserConfig");
            return v != null ? v.toString() : UNBEKANNT;
        } catch (Exception e) {
            return UNBEKANNT;
        }
    }

    private static String getProp(String key) {
        try {
            String v = System.getProperty(key);
            return v != null ? v : UNBEKANNT;
        } catch (SecurityException e) {
            return UNBEKANNT;
        }
    }
}
