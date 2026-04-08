package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Kapselt einen HTTP-Server für einen Composite View (mehrere Panels auf einer Seite).
 * <p>
 * Stellt dieselben Endpunkte wie {@link WebServerInstanz} bereit:
 * <ul>
 *   <li>{@code GET /} – React-App ({@code static/index.html} aus Classpath)</li>
 *   <li>{@code GET /assets/*} und {@code GET /images/*} – statische Ressourcen</li>
 *   <li>{@code GET /events} – SSE-Stream mit {@link CompositeSseNachricht}s</li>
 * </ul>
 */
public class CompositeViewInstanz implements SseElternInstanz {

    private static final Logger logger = LogManager.getLogger(CompositeViewInstanz.class);

    private static final int KEEPALIVE_INTERVALL_SEKUNDEN = 15;
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=UTF-8";
    private static final String STATIC_RESOURCE_PREFIX = "/de/petanqueturniermanager/webserver/static";

    private volatile CompositeViewKonfiguration konfiguration;
    private final HttpServer httpServer;
    private final ScheduledExecutorService keepAliveExecutor;
    private final CopyOnWriteArrayList<SseVerbindung> sseVerbindungen = new CopyOnWriteArrayList<>();

    /**
     * Gecachter vollständiger Zustand aller Panels als JSON.
     * Wird bei jeder neuen SSE-Verbindung sofort gesendet.
     */
    private volatile String cachedInitJson;
    private volatile boolean laeuft = false;

    public CompositeViewInstanz(CompositeViewKonfiguration konfiguration) throws IOException {
        this.konfiguration = konfiguration;
        httpServer = HttpServer.create(new InetSocketAddress(konfiguration.port()), 10);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext("/debug/sse", this::handleDebugSse);
        httpServer.createContext("/", this::handleStatischOderRoot);
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "PTM-CompositeView-KeepAlive-" + konfiguration.port());
            t.setDaemon(true);
            return t;
        });
    }

    /** Startet den HTTP-Server und den Keep-Alive-Hintergrundthread. */
    public void starten() {
        httpServer.start();
        laeuft = true;
        keepAliveExecutor.scheduleAtFixedRate(
                () -> sseVerbindungen.forEach(SseVerbindung::keepAlive),
                KEEPALIVE_INTERVALL_SEKUNDEN,
                KEEPALIVE_INTERVALL_SEKUNDEN,
                TimeUnit.SECONDS);
        logger.info("CompositeView-Server gestartet auf Port {}", konfiguration.port());
    }

    /** Stoppt den HTTP-Server und schließt alle offenen SSE-Verbindungen. */
    public void stoppen() {
        laeuft = false;
        keepAliveExecutor.shutdownNow();
        sseVerbindungen.forEach(SseVerbindung::schliessen);
        sseVerbindungen.clear();
        httpServer.stop(0);
        logger.info("CompositeView-Server gestoppt auf Port {}", konfiguration.port());
    }

    public boolean laeuft() {
        return laeuft;
    }

    public synchronized void setCachedInitJson(String json) {
        this.cachedInitJson = json;
    }

    public String getCachedInitJson() {
        return cachedInitJson;
    }

    public void sseNachrichtPushen(String json) {
        sseVerbindungen.forEach(v -> v.senden(json));
    }

    public void verbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
        logger.debug("SSE-Verbindung auf Port {} entfernt, aktiv: {}",
                konfiguration.port(), sseVerbindungen.size());
    }

    public CompositeViewKonfiguration getKonfiguration() {
        return konfiguration;
    }

    /** Aktualisiert die Konfiguration und pusht sofort den neuen gecachten Zustand. */
    public synchronized void setKonfiguration(CompositeViewKonfiguration neueKonfiguration, String neuesInitJson) {
        this.konfiguration = neueKonfiguration;
        if (neuesInitJson != null) {
            setCachedInitJson(neuesInitJson);
            sseNachrichtPushen(neuesInitJson);
        }
    }

    // ── HTTP-Handler ────────────────────────────────────────────────────────────

    private void handleStatischOderRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            serviereRessource(exchange, "/index.html", CONTENT_TYPE_HTML);
        } else if (path.startsWith("/assets/")) {
            String dateiname = path.substring("/assets/".length());
            serviereRessource(exchange, "/assets/" + dateiname, ermittleContentType(dateiname));
        } else if (path.startsWith("/images/")) {
            String dateiname = path.substring("/images/".length());
            serviereRessource(exchange, "/images/" + dateiname, ermittleContentType(dateiname));
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleEvents(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_SSE);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        var verbindung = new SseVerbindung(os, this);
        sseVerbindungen.add(verbindung);
        logger.debug("Neue SSE-Verbindung auf CompositeView-Port {}, aktiv: {}",
                konfiguration.port(), sseVerbindungen.size());

        try {
            os.write("retry: 30000\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            verbindungEntfernen(verbindung);
            return;
        }
        verbindung.sendeInitNachricht();
    }

    private void handleDebugSse(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String json = cachedInitJson != null ? cachedInitJson : "{\"info\":\"kein cachedInitJson vorhanden\"}";
        byte[] body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void serviereRessource(HttpExchange exchange, String relativerPfad, String contentType)
            throws IOException {
        String ressourcePfad = (STATIC_RESOURCE_PREFIX + relativerPfad).replaceFirst("^/", "");
        InputStream gefunden = getClass().getClassLoader().getResourceAsStream(ressourcePfad);
        if (gefunden == null) {
            gefunden = getClass().getResourceAsStream("/" + ressourcePfad);
        }
        if (gefunden == null) {
            logger.warn("Classpath-Ressource nicht gefunden: {}", ressourcePfad);
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        try (InputStream in = gefunden) {
            byte[] body = in.readAllBytes();
            var headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType);
            headers.set("Cache-Control", "no-cache");
            headers.set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static String ermittleContentType(String dateiname) {
        if (dateiname.endsWith(".js")) return "text/javascript; charset=UTF-8";
        if (dateiname.endsWith(".css")) return "text/css; charset=UTF-8";
        if (dateiname.endsWith(".html")) return CONTENT_TYPE_HTML;
        if (dateiname.endsWith(".svg")) return "image/svg+xml";
        if (dateiname.endsWith(".png")) return "image/png";
        if (dateiname.endsWith(".ico")) return "image/x-icon";
        return "application/octet-stream";
    }
}
