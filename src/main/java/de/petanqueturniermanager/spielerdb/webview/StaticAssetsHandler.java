package de.petanqueturniermanager.spielerdb.webview;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

/**
 * Liefert die React-Bundle-Assets aus dem Classpath
 * ({@code de/petanqueturniermanager/spielerdb/webview/static/}). Catch-all für
 * alles, was nicht unter {@code /api} liegt.
 */
final class StaticAssetsHandler implements HttpHandler {

    private static final Logger logger = LogManager.getLogger(StaticAssetsHandler.class);
    private static final String RESOURCE_PREFIX = "de/petanqueturniermanager/spielerdb/webview/static";
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";

    StaticAssetsHandler() {}

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            String relativ = "/".equals(path) || path.isEmpty() ? "/index.html" : path;
            String contentType = ermittleContentType(relativ);
            byte[] body = leseRessource(relativ);
            if (body == null) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            exchange.getResponseHeaders().set("Content-Type", contentType);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private byte @Nullable [] leseRessource(String relativ) throws IOException {
        String pfad = RESOURCE_PREFIX + relativ;
        try (InputStream in = ladeRessource(pfad)) {
            if (in == null) {
                logger.debug("Asset nicht gefunden: {}", pfad);
                return null;
            }
            return in.readAllBytes();
        }
    }

    @Nullable
    private InputStream ladeRessource(String pfad) {
        ClassLoader cl = getClass().getClassLoader();
        InputStream in = cl == null ? null : cl.getResourceAsStream(pfad);
        if (in != null) {
            return in;
        }
        return getClass().getResourceAsStream("/" + pfad);
    }

    private static String ermittleContentType(String pfad) {
        if (pfad.endsWith(".html")) return CONTENT_TYPE_HTML;
        if (pfad.endsWith(".js")) return "text/javascript; charset=UTF-8";
        if (pfad.endsWith(".css")) return "text/css; charset=UTF-8";
        if (pfad.endsWith(".svg")) return "image/svg+xml";
        if (pfad.endsWith(".png")) return "image/png";
        if (pfad.endsWith(".ico")) return "image/x-icon";
        if (pfad.endsWith(".json")) return "application/json; charset=UTF-8";
        return "application/octet-stream";
    }
}
