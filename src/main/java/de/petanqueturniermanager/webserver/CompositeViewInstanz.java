package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Kapselt einen HTTP-Server für einen Composite View (mehrere Panels auf einer Seite).
 * <p>
 * Stellt folgende Endpunkte bereit:
 * <ul>
 *   <li>{@code GET /} – React-App ({@code static/index.html} aus Classpath)</li>
 *   <li>{@code GET /assets/*} und {@code GET /images/*} – statische Ressourcen</li>
 *   <li>{@code GET /events} – SSE-Stream mit {@link CompositeSseNachricht}s</li>
 * </ul>
 */
public class CompositeViewInstanz implements SseElternInstanz, WebServerSlot {

    private static final Logger logger = LogManager.getLogger(CompositeViewInstanz.class);

    private static final int KEEPALIVE_INTERVALL_SEKUNDEN = 15;
    private static final int MAX_STEUERUNG_BODY_BYTES = 16 * 1024;
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=UTF-8";
    private static final String STATIC_RESOURCE_PREFIX = "/de/petanqueturniermanager/webserver/static";
    private static final Gson GSON = new Gson();

    private volatile CompositeViewKonfiguration konfiguration;
    private final HttpServer httpServer;
    private final ScheduledExecutorService keepAliveExecutor;
    private final CopyOnWriteArrayList<SseVerbindung> sseVerbindungen = new CopyOnWriteArrayList<>();

    /**
     * Gecachter vollständiger Zustand aller Panels als JSON.
     * Wird bei jeder neuen SSE-Verbindung sofort gesendet.
     */
    private volatile String cachedInitJson;
    private volatile String aktuelleSplitSteuerungJson;
    private volatile boolean laeuft = false;

    public CompositeViewInstanz(CompositeViewKonfiguration konfiguration) throws IOException {
        this.konfiguration = konfiguration;
        httpServer = HttpServer.create(new InetSocketAddress(konfiguration.port()), 10);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext("/debug/sse", this::handleDebugSse);
        httpServer.createContext("/steuerung", this::handleSteuerung);
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
        aktuelleSplitSteuerungJson = null;
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

    @Override
    public String[] getInitZusatzJsons() {
        String splitJson = aktuelleSplitSteuerungJson;
        String slaveStatusJson = slaveStatusJson();
        if (splitJson == null) {
            return new String[] { slaveStatusJson };
        }
        return new String[] { splitJson, slaveStatusJson };
    }

    public void sseNachrichtPushen(String json) {
        sseVerbindungen.forEach(v -> v.senden(json));
    }

    public void verbindungEntfernen(SseVerbindung verbindung) {
        boolean warSlave = istSlave(verbindung);
        sseVerbindungen.remove(verbindung);
        logger.debug("SSE-Verbindung auf Port {} entfernt, aktiv: {}",
                konfiguration.port(), sseVerbindungen.size());
        if (warSlave) {
            slaveStatusPushen();
        }
    }

    /** @return {@code true} wenn mindestens ein Browser-Client per SSE verbunden ist. */
    public boolean hatAktiveVerbindungen() {
        return !sseVerbindungen.isEmpty();
    }

    @Override
    public int getPort() {
        return konfiguration.port();
    }

    @Override
    public String getAnzeigeName() {
        var name = konfiguration.name();
        if (name != null && !name.isBlank()) {
            return name.trim();
        }
        return I18n.get("webserver.compositeview.anzeigename");
    }

    public CompositeViewKonfiguration getKonfiguration() {
        return konfiguration;
    }

    /** Gibt {@code true} zurück, wenn diese Instanz mindestens ein TIMER-Panel enthält. */
    public boolean hatTimerPanels() {
        return konfiguration.panels().stream().anyMatch(p -> p.typ() == PanelTyp.TIMER);
    }

    /**
     * Aktualisiert die Konfiguration, speichert das gecachte Init-JSON und pusht separat.
     *
     * @param neueKonfiguration neue Panel-Konfiguration
     * @param neuesCachedJson   wird als cachedInitJson gespeichert (für reconnectende Clients); {@code null} = unveränderter Cache
     * @param pushJson          wird sofort an alle offenen SSE-Verbindungen gesendet; {@code null} = kein Push
     */
    public synchronized void setKonfiguration(CompositeViewKonfiguration neueKonfiguration,
            String neuesCachedJson, String pushJson) {
        this.konfiguration = neueKonfiguration;
        if (neuesCachedJson != null) {
            setCachedInitJson(neuesCachedJson);
        }
        if (pushJson != null) {
            sseNachrichtPushen(pushJson);
        }
    }

    /** Aktualisiert die Konfiguration und pusht sofort denselben Zustand als Cache und Live-Push. */
    public synchronized void setKonfiguration(CompositeViewKonfiguration neueKonfiguration, String neuesInitJson) {
        setKonfiguration(neueKonfiguration, neuesInitJson, neuesInitJson);
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
        var params = queryParameter(exchange);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_SSE);
        exchange.getResponseHeaders().set("Cache-Control", "no-cache");
        exchange.getResponseHeaders().set("Connection", "keep-alive");
        exchange.getResponseHeaders().set("X-Accel-Buffering", "no");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, 0);

        OutputStream os = exchange.getResponseBody();
        var verbindung = new SseVerbindung(os, this, params.get("rolle"), params.get("clientId"));
        sseVerbindungen.add(verbindung);
        logger.debug("Neue SSE-Verbindung auf CompositeView-Port {}, aktiv: {}",
                konfiguration.port(), sseVerbindungen.size());

        try {
            os.write("retry: 3000\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            verbindungEntfernen(verbindung);
            return;
        }
        verbindung.sendeInitNachricht();
        if (istSlave(verbindung)) {
            slaveStatusPushen();
        }
    }

    private void handleSteuerung(HttpExchange exchange) throws IOException {
        if (!"/steuerung/split".equals(exchange.getRequestURI().getPath())) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        try {
            byte[] body = exchange.getRequestBody().readNBytes(MAX_STEUERUNG_BODY_BYTES + 1);
            if (body.length > MAX_STEUERUNG_BODY_BYTES) {
                exchange.sendResponseHeaders(413, -1);
                return;
            }
            String json = splitSteuerungJsonAusRequest(new String(body, StandardCharsets.UTF_8));
            aktuelleSplitSteuerungJson = json;
            sseNachrichtPushen(json);
            exchange.sendResponseHeaders(204, -1);
        } catch (IllegalArgumentException e) {
            logger.debug("Ungültige Split-Steuerung auf Port {}: {}", konfiguration.port(), e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        } finally {
            exchange.close();
        }
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

    static String splitSteuerungJsonAusRequest(String requestJson) {
        JsonElement root;
        try {
            root = JsonParser.parseString(requestJson);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("JSON kann nicht gelesen werden", e);
        }
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("Root muss ein Objekt sein");
        }
        JsonElement gruppenElement = root.getAsJsonObject().get("gruppen");
        if (gruppenElement == null || !gruppenElement.isJsonObject()) {
            throw new IllegalArgumentException("gruppen fehlt");
        }
        Map<String, List<Double>> gruppen = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> eintrag : gruppenElement.getAsJsonObject().entrySet()) {
            String pfad = eintrag.getKey();
            if (!pfad.matches("R(?:/[LR])*")) {
                throw new IllegalArgumentException("ungültiger Gruppenpfad");
            }
            JsonElement werteElement = eintrag.getValue();
            if (!werteElement.isJsonArray() || werteElement.getAsJsonArray().size() != 2) {
                throw new IllegalArgumentException("Gruppe muss genau zwei Werte haben");
            }
            double a = wertAlsProzent(werteElement.getAsJsonArray().get(0));
            double b = wertAlsProzent(werteElement.getAsJsonArray().get(1));
            double summe = a + b;
            if (Math.abs(summe - 100.0) > 0.5) {
                throw new IllegalArgumentException("Split-Werte müssen zusammen 100 ergeben");
            }
            gruppen.put(pfad, List.of(a, b));
        }
        return GSON.toJson(new SplitSteuerungNachricht("split_steuerung", gruppen));
    }

    private static double wertAlsProzent(JsonElement wert) {
        if (!wert.isJsonPrimitive() || !wert.getAsJsonPrimitive().isNumber()) {
            throw new IllegalArgumentException("Split-Wert muss numerisch sein");
        }
        double zahl = wert.getAsDouble();
        if (!Double.isFinite(zahl) || zahl < 0 || zahl > 100) {
            throw new IllegalArgumentException("Split-Wert außerhalb 0..100");
        }
        return Math.round(zahl * 100.0) / 100.0;
    }

    private void slaveStatusPushen() {
        sseNachrichtPushen(slaveStatusJson());
    }

    private String slaveStatusJson() {
        long anzahl = sseVerbindungen.stream().filter(CompositeViewInstanz::istSlave).count();
        return GSON.toJson(new SlaveStatusNachricht("slave_status", anzahl));
    }

    private static boolean istSlave(SseVerbindung verbindung) {
        return "slave".equals(verbindung.getRolle());
    }

    private static Map<String, String> queryParameter(HttpExchange exchange) {
        String query = exchange.getRequestURI().getRawQuery();
        Map<String, String> result = new LinkedHashMap<>();
        if (query == null || query.isBlank()) {
            return result;
        }
        for (String teil : query.split("&")) {
            int trenner = teil.indexOf('=');
            String key = trenner >= 0 ? teil.substring(0, trenner) : teil;
            String value = trenner >= 0 ? teil.substring(trenner + 1) : "";
            result.put(urlDecode(key), urlDecode(value));
        }
        return result;
    }

    private static String urlDecode(String wert) {
        return URLDecoder.decode(wert, StandardCharsets.UTF_8);
    }

    record SplitSteuerungNachricht(String typ, Map<String, List<Double>> gruppen) {
    }

    record SlaveStatusNachricht(String typ, long anzahl) {
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
