package de.petanqueturniermanager.timer;

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

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.webserver.LogoBildServieren;
import de.petanqueturniermanager.webserver.SseVerbindung;
import de.petanqueturniermanager.webserver.SseElternInstanz;

/**
 * Dedizierter HTTP-Server für die Timer-Webansicht.
 * <p>
 * Stellt folgende Endpunkte bereit:
 * <ul>
 *   <li>{@code GET /} → {@code timer.html} (Vanilla-HTML Countdown-Seite)</li>
 *   <li>{@code GET /events} → Server-Sent-Events mit {@link TimerSseNachricht} als JSON</li>
 * </ul>
 * <p>
 * Implementiert {@link TimerListener} – Zustandsänderungen werden vom {@link de.petanqueturniermanager.webserver.WebServerManager}
 * delegiert, der einmalig als Listener registriert ist.
 */
public class TimerWebServerInstanz implements TimerListener, SseElternInstanz {

    private static final Logger logger = LogManager.getLogger(TimerWebServerInstanz.class);

    private static final int KEEPALIVE_INTERVALL_SEKUNDEN = 15;
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=UTF-8";
    private static final String TIMER_HTML_RESSOURCE =
            "de/petanqueturniermanager/timer/timer.html";

    private static final Gson GSON = new Gson();

    private static final String ENDPUNKT_TIMER_LOGO = "/timer-logo";

    private final int port;
    private final HttpServer httpServer;
    private final ScheduledExecutorService keepAliveExecutor;
    private final CopyOnWriteArrayList<SseVerbindung> sseVerbindungen = new CopyOnWriteArrayList<>();

    private volatile String cachedInitJson;
    private volatile boolean laeuft = false;
    private volatile TimerState letzterState = TimerState.inaktiv();
    private volatile String logoUrl = "";

    /**
     * Erstellt eine neue Timer-Webserver-Instanz auf dem angegebenen Port.
     *
     * @param port TCP-Port (1024–65535)
     * @throws IOException wenn der Port nicht gebunden werden kann
     */
    public TimerWebServerInstanz(int port) throws IOException {
        this.port = port;
        httpServer = HttpServer.create(new InetSocketAddress(port), 10);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext(ENDPUNKT_TIMER_LOGO, this::handleTimerLogo);
        httpServer.createContext("/", this::handleRoot);
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "PTM-Timer-WebServer-KeepAlive-" + port);
            t.setDaemon(true);
            return t;
        });
        cachedInitJson = zuStateJson(TimerState.inaktiv());
    }

    /** Startet den HTTP-Server und den Keep-Alive-Thread. */
    public void starten() {
        httpServer.start();
        laeuft = true;
        keepAliveExecutor.scheduleAtFixedRate(
                () -> sseVerbindungen.forEach(SseVerbindung::keepAlive),
                KEEPALIVE_INTERVALL_SEKUNDEN,
                KEEPALIVE_INTERVALL_SEKUNDEN,
                TimeUnit.SECONDS);
        logger.info("Timer-Webserver gestartet auf Port {}", port);
    }

    /** Stoppt den HTTP-Server und schließt alle SSE-Verbindungen. */
    public void stoppen() {
        laeuft = false;
        keepAliveExecutor.shutdownNow();
        sseVerbindungen.forEach(SseVerbindung::schliessen);
        sseVerbindungen.clear();
        httpServer.stop(0);
        logger.info("Timer-Webserver gestoppt auf Port {}", port);
    }

    /** Gibt zurück ob dieser Server läuft. */
    public boolean laeuft() {
        return laeuft;
    }

    /** Gibt den konfigurierten Port zurück. */
    public int getPort() {
        return port;
    }

    // ── TimerListener ──────────────────────────────────────────────────────────

    @Override
    public void onChange(TimerState state) {
        if (!laeuft) {
            return;
        }
        letzterState = state;
        var json = zuStateJson(state);
        cachedInitJson = json;
        sseVerbindungen.forEach(v -> v.senden(json));
    }

    /**
     * Setzt die Logo-URL und pusht sofort ein aktualisiertes SSE-Event an alle verbundenen Clients.
     *
     * @param url gespeicherte Logo-URL (file://, http(s):// oder leer)
     */
    public void setLogoUrl(String url) {
        this.logoUrl = url != null ? url : "";
        if (laeuft) {
            var json = zuStateJson(letzterState);
            cachedInitJson = json;
            sseVerbindungen.forEach(v -> v.senden(json));
        }
    }

    // ── SseElternInstanz ───────────────────────────────────────────────────────

    @Override
    public String getCachedInitJson() {
        return cachedInitJson;
    }

    @Override
    public void verbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
    }

    // ── HTTP-Handler ───────────────────────────────────────────────────────────

    private void handleRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        InputStream ressource = getClass().getClassLoader().getResourceAsStream(TIMER_HTML_RESSOURCE);
        if (ressource == null) {
            logger.warn("timer.html nicht gefunden im Classpath: {}", TIMER_HTML_RESSOURCE);
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        try (InputStream in = ressource) {
            byte[] body = in.readAllBytes();
            exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_HTML);
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
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
        try {
            os.write("retry: 30000\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            verbindungEntfernen(verbindung);
            return;
        }
        verbindung.sendeInitNachricht();
    }

    private void handleTimerLogo(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        LogoBildServieren.serviere(exchange, logoUrl);
    }

    // ── Hilfsmethoden ─────────────────────────────────────────────────────────

    private String zuStateJson(TimerState state) {
        var browserLogoUrl = LogoBildServieren.zuBrowserUrl(logoUrl, ENDPUNKT_TIMER_LOGO);
        var nachricht = new TimerSseNachricht(
                state.anzeige(),
                state.sekunden(),
                state.zustand().name(),
                statusText(state.zustand()),
                state.bezeichnung(),
                state.hintergrundFarbe(),
                browserLogoUrl);
        return GSON.toJson(nachricht);
    }

    private static String statusText(TimerZustand zustand) {
        return switch (zustand) {
            case LAEUFT   -> I18n.get("timer.zustand.laeuft");
            case PAUSIERT -> I18n.get("timer.zustand.pausiert");
            case BEENDET  -> I18n.get("timer.zustand.beendet");
            case INAKTIV  -> I18n.get("timer.zustand.inaktiv");
        };
    }
}
