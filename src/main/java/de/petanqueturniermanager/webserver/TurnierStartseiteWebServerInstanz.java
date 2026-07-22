package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.helper.i18n.I18n;

/**
 * Dedizierter HTTP-Server für die Turnier-Startseite. Liefert das gemeinsame React-Bundle
 * aus dem Composite-Static-Verzeichnis aus und pusht {@link StartseiteSseNachricht}s
 * über SSE. Architektur-Vorbild: {@link CompositeViewInstanz} (statische Ressourcen) bzw.
 * {@link de.petanqueturniermanager.timer.TimerWebServerInstanz} (Single-Port).
 */
public class TurnierStartseiteWebServerInstanz implements SseElternInstanz, WebServerSlot, RegieQuelle {

    private static final Logger logger = LogManager.getLogger(TurnierStartseiteWebServerInstanz.class);

    private static final int KEEPALIVE_INTERVALL_SEKUNDEN = 15;
    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=UTF-8";
    private static final String STATIC_RESOURCE_PREFIX = "/de/petanqueturniermanager/webserver/static";

    private final int port;
    private final HttpServer httpServer;
    private final ScheduledExecutorService keepAliveExecutor;
    private final CopyOnWriteArrayList<SseVerbindung> sseVerbindungen = new CopyOnWriteArrayList<>();

    private volatile String cachedInitJson;
    private volatile boolean laeuft = false;
    /** Aktuell konfigurierter Logo-Pfad oder -URL (leer = kein Logo). */
    private volatile String logoQuelle = "";

    public TurnierStartseiteWebServerInstanz(int port) throws IOException {
        this.port = port;
        httpServer = HttpServer.create(new InetSocketAddress(port), 10);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/events", this::handleEvents);
        httpServer.createContext("/turnierlogo", this::handleTurnierlogo);
        httpServer.createContext("/", this::handleStatischOderRoot);
        keepAliveExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "PTM-Startseite-WebServer-KeepAlive-" + port);
            t.setDaemon(true);
            return t;
        });
    }

    public void starten() {
        httpServer.start();
        laeuft = true;
        keepAliveExecutor.scheduleAtFixedRate(
                () -> sseVerbindungen.forEach(SseVerbindung::keepAlive),
                KEEPALIVE_INTERVALL_SEKUNDEN,
                KEEPALIVE_INTERVALL_SEKUNDEN,
                TimeUnit.SECONDS);
        logger.info("Turnier-Startseite-Webserver gestartet auf Port {}", port);
    }

    public void stoppen() {
        laeuft = false;
        keepAliveExecutor.shutdownNow();
        sseVerbindungen.forEach(SseVerbindung::schliessen);
        sseVerbindungen.clear();
        httpServer.stop(0);
        logger.info("Turnier-Startseite-Webserver gestoppt auf Port {}", port);
    }

    @Override
    public boolean laeuft() {
        return laeuft;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getViewId() {
        return WebServerManager.STARTSEITE_VIEW_ID;
    }

    @Override
    public String getAnzeigeName() {
        return I18n.get("startseite.anzeigename");
    }

    public synchronized void setCachedInitJson(String json) {
        this.cachedInitJson = json;
    }

    /** Aktualisiert die für {@code /turnierlogo} ausgelieferte Quelle. */
    public void setLogoQuelle(String quelle) {
        this.logoQuelle = quelle == null ? "" : quelle.trim();
    }

    @Override
    public String getCachedInitJson() {
        return cachedInitJson;
    }

    public void sseNachrichtPushen(String json) {
        sseVerbindungen.forEach(v -> v.senden(json));
    }

    @Override
    public void regieVerbindungHinzufuegen(SseVerbindung verbindung) {
        sseVerbindungen.add(verbindung);
    }

    @Override
    public void regieVerbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
    }

    @Override
    public void verbindungEntfernen(SseVerbindung verbindung) {
        sseVerbindungen.remove(verbindung);
    }

    // ── HTTP-Handler ────────────────────────────────────────────────────────────

    private void handleStatischOderRoot(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isEmpty()) {
            serviereRessource(exchange, "/index.html", WebContentType.HTML);
        } else if (path.startsWith("/assets/")) {
            serviereRessource(exchange, "/assets/" + path.substring("/assets/".length()),
                    WebContentType.fuerDateiname(path));
        } else if (path.startsWith("/images/")) {
            serviereRessource(exchange, "/images/" + path.substring("/images/".length()),
                    WebContentType.fuerDateiname(path));
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
        try {
            os.write("retry: 3000\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            verbindungEntfernen(verbindung);
            return;
        }
        verbindung.sendeInitNachricht();
    }

    @Override
    public void handleTurnierlogo(HttpExchange exchange) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String quelle = logoQuelle;
        if (quelle.isEmpty()) {
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        // Externe http(s)-URL: Weiterleiten an den Browser.
        if (quelle.startsWith("http://") || quelle.startsWith("https://")) {
            exchange.getResponseHeaders().set("Location", quelle);
            exchange.sendResponseHeaders(302, -1);
            return;
        }
        // Lokale Datei: file:// oder absoluter Pfad.
        Path datei;
        try {
            datei = quelle.startsWith("file:")
                    ? Paths.get(URI.create(quelle))
                    : Paths.get(quelle);
        } catch (IllegalArgumentException e) {
            logger.warn("Ungültige Logo-Quelle: {}", quelle, e);
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        if (!Files.isRegularFile(datei) || !Files.isReadable(datei)) {
            logger.warn("Turnierlogo-Datei nicht lesbar: {}", datei);
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        byte[] body = Files.readAllBytes(datei);
        Path dateiname = datei.getFileName();
        var headers = exchange.getResponseHeaders();
        headers.set("Content-Type", WebContentType.fuerDateiname(dateiname != null ? dateiname.toString() : ""));
        headers.set("Cache-Control", "no-cache");
        headers.set("Access-Control-Allow-Origin", "*");
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
}
