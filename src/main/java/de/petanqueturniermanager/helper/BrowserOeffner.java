/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Locale;

/**
 * Oeffnet URLs AWT-frei ueber das Betriebssystem.
 */
public final class BrowserOeffner {

    private BrowserOeffner() {
        // nur statisch
    }

    public static void oeffne(URI url) throws IOException {
        oeffne(url.toString());
    }

    public static void oeffne(String url) throws IOException {
        new ProcessBuilder(befehlFuerOs(System.getProperty("os.name"), url)).start();
    }

    static List<String> befehlFuerOs(String osName, String url) {
        String normalisiert = osName == null ? "" : osName.toLowerCase(Locale.ROOT);
        if (normalisiert.contains("win")) {
            return List.of("rundll32", "url.dll,FileProtocolHandler", url);
        }
        if (normalisiert.contains("mac")) {
            return List.of("open", url);
        }
        return List.of("xdg-open", url);
    }
}
