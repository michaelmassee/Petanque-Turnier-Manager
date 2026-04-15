package de.petanqueturniermanager.webserver;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;

/**
 * Hilfsmethode zum Ausliefern eines Turnierlogos über HTTP.
 * <p>
 * Unterstützt drei Fälle:
 * <ul>
 *   <li>{@code file://…} → liest die Datei aus dem lokalen Dateisystem und sendet sie als Bild</li>
 *   <li>{@code http(s)://…} → sendet einen HTTP-302-Redirect zur externen URL</li>
 *   <li>leer oder unbekannt → HTTP 404</li>
 * </ul>
 */
public final class LogoBildServieren {

    private static final Logger logger = LogManager.getLogger(LogoBildServieren.class);

    private LogoBildServieren() {
    }

    /**
     * Sendet das Logo als HTTP-Antwort.
     *
     * @param exchange aktives {@link HttpExchange}
     * @param logoUrl  gespeicherte Logo-URL (file://, http(s):// oder leer)
     */
    public static void serviere(HttpExchange exchange, String logoUrl) throws IOException {
        if (StringUtils.isBlank(logoUrl)) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        if (logoUrl.startsWith("file://")) {
            serviereLokaleDatei(exchange, logoUrl);
        } else if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
            serviereAlsRedirect(exchange, logoUrl);
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    /**
     * Prüft ob eine Logo-URL auf eine lokale Datei zeigt.
     *
     * @param logoUrl die zu prüfende URL
     * @return {@code true} wenn die URL mit {@code file://} beginnt
     */
    public static boolean istDateiUrl(String logoUrl) {
        return logoUrl != null && logoUrl.startsWith("file://");
    }

    /**
     * Gibt den Webserver-Endpunkt-Pfad zurück, über den eine {@code file://}-Logo-URL
     * vom Browser abgerufen werden soll (z.B. {@code /timer-logo} oder {@code /logo}).
     * <p>
     * Bei externen HTTP(S)-URLs wird die URL direkt zurückgegeben.
     * Bei leerem Wert wird {@code null} zurückgegeben.
     *
     * @param logoUrl         rohe Logo-URL (kann file:// oder http(s):// sein)
     * @param lokalesEndpunkt Endpunkt-Pfad für lokale Dateien (z.B. {@code "/timer-logo"})
     * @return die für den Browser geeignete URL, oder {@code null}
     */
    public static String zuBrowserUrl(String logoUrl, String lokalesEndpunkt) {
        if (StringUtils.isBlank(logoUrl)) {
            return null;
        }
        if (logoUrl.startsWith("file://")) {
            return lokalesEndpunkt;
        }
        if (logoUrl.startsWith("http://") || logoUrl.startsWith("https://")) {
            return logoUrl;
        }
        return null;
    }

    private static void serviereLokaleDatei(HttpExchange exchange, String fileUrl) throws IOException {
        File datei;
        try {
            datei = new File(new URI(fileUrl));
        } catch (URISyntaxException e) {
            logger.warn("Ungültige file://-URL für Logo: {}", fileUrl, e);
            exchange.sendResponseHeaders(400, -1);
            return;
        }

        if (!datei.exists() || !datei.isFile()) {
            logger.warn("Logo-Datei nicht gefunden: {}", datei.getAbsolutePath());
            exchange.sendResponseHeaders(404, -1);
            return;
        }

        String contentType = ermittleContentType(datei.getName());
        byte[] bytes = Files.readAllBytes(datei.toPath());

        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Cache-Control", "max-age=300");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void serviereAlsRedirect(HttpExchange exchange, String url) throws IOException {
        exchange.getResponseHeaders().set("Location", url);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(302, -1);
    }

    /**
     * Kopiert eine Logo-Datei (file://-URL) in das angegebene Zielverzeichnis.
     * Erstellt das Verzeichnis falls nötig.
     *
     * @param fileUrl     die file://-URL der Logo-Datei
     * @param zielOrdner  Zielverzeichnis (wird erstellt wenn nicht vorhanden)
     * @return relativer Dateipfad innerhalb des Zielordners, oder {@code null} bei Fehler
     */
    public static String kopiereInOrdner(String fileUrl, File zielOrdner) {
        if (!istDateiUrl(fileUrl)) {
            return null;
        }
        try {
            File quelle = new File(new URI(fileUrl));
            if (!quelle.exists() || !quelle.isFile()) {
                logger.warn("Logo-Quelldatei nicht gefunden: {}", quelle.getAbsolutePath());
                return null;
            }
            if (!zielOrdner.exists()) {
                zielOrdner.mkdirs();
            }
            File ziel = new File(zielOrdner, quelle.getName());
            try (InputStream in = Files.newInputStream(quelle.toPath());
                 OutputStream out = Files.newOutputStream(ziel.toPath())) {
                in.transferTo(out);
            }
            return zielOrdner.getName() + "/" + quelle.getName();
        } catch (URISyntaxException | IOException e) {
            logger.error("Logo-Datei konnte nicht kopiert werden: {}", fileUrl, e);
            return null;
        }
    }

    private static String ermittleContentType(String dateiname) {
        var name = dateiname.toLowerCase();
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".gif"))  return "image/gif";
        if (name.endsWith(".svg"))  return "image/svg+xml";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".ico"))  return "image/x-icon";
        return "application/octet-stream";
    }
}
