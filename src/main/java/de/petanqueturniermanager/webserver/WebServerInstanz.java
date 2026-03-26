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
 * Kapselt einen HTTP-Server auf einem einzelnen Port mit SSE-Unterstützung.
 * <p>
 * Stellt folgende Endpunkte bereit:
 * <ul>
 *   <li>{@code GET /} – React-App ({@code static/index.html} aus Classpath)</li>
 *   <li>{@code GET /assets/*} – Bundle-Dateien aus {@code static/assets/}</li>
 *   <li>{@code GET /events} – Server-Sent-Events-Stream (JSON-Diffs)</li>
 * </ul>
 * <p>
 * HTTP-Handler-Threads greifen <strong>nur</strong> auf den gecachten JSON-String zu –
 * niemals direkt auf UNO. Der Cache wird ausschließlich im SheetRunner-Thread befüllt.
 * <p>
 * Bei jeder neuen SSE-Verbindung wird sofort der gecachte Init-State gesendet,
 * sodass Browser nach einem Reconnect sofort den vollständigen Tabellenzustand erhalten.
 */
public class WebServerInstanz {

    private static final Logger logger = LogManager.getLogger(WebServerInstanz.class);

    private static final int KEEPALIVE_INTERVALL_SEKUNDEN = 15;
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=UTF-8";
    private static final String STATIC_RESOURCE_PREFIX = "/de/petanqueturniermanager/webserver/static";

    private final PortKonfiguration konfiguration;
    private final HttpServer httpServer;
    private final ScheduledExecutorService keepAliveExecutor;
    private final CopyOnWriteArrayList<SseVerbindung> sseVerbindungen = new CopyOnWriteArrayList<>();

    /**
     * Gecachter vollständiger Tabellenzustand als JSON ({@code SseNachricht} mit {@code typ="init"}).
     * Wird bei jeder neuen SSE-Verbindung sofort gesendet.
     * Schreibzugriff nur synchronisiert.
     */
    private volatile String cachedInitJson;
    private volatile boolean laeuft = false;

    public WebServerInstanz(PortKonfiguration konfiguration) throws IOException {
        this.konfiguration = konfiguration;
        httpServer = HttpServer.create(new InetSocketAddress(konfiguration.port()), 10);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        // Nur zwei Kontexte: SSE-Stream und alles andere (Root + Assets)
        // Ein einziger catch-all-Handler vermeidet Routing-Probleme mit com.sun.net.httpserver
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext("/debug/sse", this::handleDebugSse);
        httpServer.createContext("/", this::handleStatischOderRoot);
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PTM-WebServer-KeepAlive-" + konfiguration.port());
            t.setDaemon(true);
            return t;
        });
    }

    /** Startet den HTTP-Server und den Keep-Alive-Hintergrund-Thread. */
    public void starten() {
        httpServer.start();
        laeuft = true;
        keepAliveExecutor.scheduleAtFixedRate(
                () -> sseVerbindungen.forEach(SseVerbindung::keepAlive),
                KEEPALIVE_INTERVALL_SEKUNDEN,
                KEEPALIVE_INTERVALL_SEKUNDEN,
                TimeUnit.SECONDS);
        logger.info("WebServer gestartet auf Port {}", konfiguration.port());
    }

    /**
     * Stoppt den HTTP-Server und schließt alle offenen SSE-Verbindungen.
     * Nach diesem Aufruf ist die Instanz nicht mehr verwendbar.
     */
    public void stoppen() {
        laeuft = false;
        keepAliveExecutor.shutdownNow();
        sseVerbindungen.forEach(SseVerbindung::schliessen);
        sseVerbindungen.clear();
        httpServer.stop(0);
        logger.info("WebServer gestoppt auf Port {}", konfiguration.port());
    }

    /** Gibt zurück ob dieser Server läuft. */
    public boolean laeuft() {
        return laeuft;
    }

    /**
     * Aktualisiert den gecachten vollständigen Init-State.
     * Synchronisiert, um Out-of-order-Updates bei schnellen Mehrfachklicks zu verhindern.
     *
     * @param json serialisiertes {@code SseNachricht} mit {@code typ="init"}
     */
    public synchronized void setCachedInitJson(String json) {
        this.cachedInitJson = json;
    }

    /** Liefert den zuletzt gecachten Init-State (für neue Verbindungen). */
    public String getCachedInitJson() {
        return cachedInitJson;
    }

    /** Sendet eine SSE-Nachricht an alle verbundenen Browser-Clients. */
    public void sseNachrichtPushen(String json) {
        sseVerbindungen.forEach(v -> v.senden(json));
    }

    /** Entfernt eine abgebrochene SSE-Verbindung aus der Liste. */
    public void verbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
        logger.debug("SSE-Verbindung auf Port {} entfernt, aktiv: {}",
                konfiguration.port(), sseVerbindungen.size());
    }

    public PortKonfiguration getKonfiguration() {
        return konfiguration;
    }

    // ── HTTP-Handler ─────────────────────────────────────────────────────────

    /**
     * Einziger HTTP-Handler für alle nicht-SSE-Requests.
     * <ul>
     *   <li>{@code /} → {@code index.html}</li>
     *   <li>{@code /assets/*} → Bundle-Dateien aus {@code static/assets/}</li>
     *   <li>Alles andere → 404</li>
     * </ul>
     */
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
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    /** Öffnet eine SSE-Verbindung, sendet sofort den Init-State und hält den Stream offen. */
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
        logger.debug("Neue SSE-Verbindung auf Port {}, aktiv: {}",
                konfiguration.port(), sseVerbindungen.size());

        // Reconnect-Intervall und sofortiger Init-State
        try {
            os.write("retry: 30000\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            verbindungEntfernen(verbindung);
            return;
        }
        verbindung.sendeInitNachricht(); // sofort vollständigen Zustand senden
    }

    /** Gibt den gecachten Init-State als rohen JSON-Text aus (nur für Diagnose). */
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

    // ── Classpath-Ressource ausliefern ────────────────────────────────────────

    private void serviereRessource(HttpExchange exchange, String relativerPfad, String contentType)
            throws IOException {
        // Führendes '/' entfernen – ClassLoader.getResourceAsStream() erwartet relativen Pfad
        String ressourcePfad = (STATIC_RESOURCE_PREFIX + relativerPfad).replaceFirst("^/", "");
        InputStream gefunden = getClass().getClassLoader().getResourceAsStream(ressourcePfad);
        if (gefunden == null) {
            // Fallback: Class-basiertes Laden mit absolutem Pfad
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
