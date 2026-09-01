package de.petanqueturniermanager.comp;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

/**
 * Prüft beim Start, ob die laufende LibreOffice-Version von der Version abweicht,
 * gegen die das Plugin gebaut wurde (Property {@code lo.build.version} in
 * {@code build-lo-version.properties}, von Gradle-Task {@code generateLoVersionInfo}
 * über {@code soffice --version} erzeugt).
 *
 * <p>Rein diagnostisch: bei abweichender Major-Version wird nur eine WARN-Zeile
 * geloggt (siehe {@link LoVersionVergleicher}), kein User-Dialog.
 */
public final class LoVersionChecker {

    private static final Logger logger = LogManager.getLogger(LoVersionChecker.class);
    private static final String BUILD_VERSION_RESOURCE = "/de/petanqueturniermanager/comp/build-lo-version.properties";

    private static volatile String buildVersionCache;

    private LoVersionChecker() {
    }

    public static void pruefeUndLoggeKompatibilitaet(XComponentContext context) {
        try {
            String buildVersion = ermittleBuildVersion();
            String laufendeVersion = LoRuntimeVersion.ermittleProdukt(context).version();
            if (LoVersionVergleicher.istMajorAbweichend(buildVersion, laufendeVersion)) {
                logger.warn("LO-Versions-Abweichung: Plugin wurde mit LibreOffice {} gebaut, "
                        + "läuft aber auf LibreOffice {} - mögliche UNO-API-Inkompatibilität.",
                        buildVersion, laufendeVersion);
            } else {
                logger.debug("LO-Versions-Check: Build={}, Laufzeit={}", buildVersion, laufendeVersion);
            }
        } catch (Exception e) {
            // Darf den Startup niemals scheitern lassen.
            logger.warn("LO-Versions-Check konnte nicht durchgeführt werden: {}", e.getMessage(), e);
        }
    }

    /** Liefert die zur Build-Zeit eingebettete LO-Version bzw. "unbekannt", statisch gecacht. */
    static String ermittleBuildVersion() {
        String cached = buildVersionCache;
        if (cached != null) {
            return cached;
        }
        String version = leseBuildVersionAusResource();
        buildVersionCache = version;
        return version;
    }

    private static String leseBuildVersionAusResource() {
        try (InputStream in = LoVersionChecker.class.getResourceAsStream(BUILD_VERSION_RESOURCE)) {
            if (in == null) {
                return "unbekannt";
            }
            Properties props = new Properties();
            props.load(in);
            return props.getProperty("lo.build.version", "unbekannt");
        } catch (IOException | RuntimeException e) {
            logger.debug("build-lo-version.properties konnte nicht gelesen werden: {}", e.getMessage());
            return "unbekannt";
        }
    }
}
