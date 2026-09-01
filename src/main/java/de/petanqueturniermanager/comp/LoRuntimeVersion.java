package de.petanqueturniermanager.comp;

import com.sun.star.beans.PropertyState;
import com.sun.star.beans.PropertyValue;
import com.sun.star.container.XHierarchicalNameAccess;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Liest das aktuell laufende LibreOffice-Produkt (Name + Version) via UNO-Konfiguration aus.
 * Wird von {@link StartupInfoLogger} (Startup-Banner) und {@link LoVersionChecker}
 * (Build-vs-Laufzeit-Vergleich) gemeinsam genutzt.
 */
public final class LoRuntimeVersion {

    static final String UNBEKANNT = "?";

    private LoRuntimeVersion() {
    }

    /** Name und Version des laufenden LO-Produkts (z.B. "LibreOffice", "24.8.3.2"). */
    public record LoProdukt(String name, String version) {
    }

    /** Liefert Produktname und {@code ooSetupVersionAboutBox} bzw. Fallbacks. */
    public static LoProdukt ermittleProdukt(XComponentContext context) {
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
            return new LoProdukt(name, version);
        } catch (Exception e) {
            return new LoProdukt("LibreOffice", UNBEKANNT);
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
}
