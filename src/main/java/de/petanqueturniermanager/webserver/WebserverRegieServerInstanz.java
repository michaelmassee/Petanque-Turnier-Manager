package de.petanqueturniermanager.webserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.GlobalProperties;
import de.petanqueturniermanager.comp.GlobalProperties.RegieZielRoh;
import de.petanqueturniermanager.helper.i18n.I18n;

public class WebserverRegieServerInstanz {

    private static final Logger logger = LogManager.getLogger(WebserverRegieServerInstanz.class);
    private static final int MAX_STEUERUNG_BODY_BYTES = 16 * 1024;
    private static final String CONTENT_TYPE_HTML = "text/html; charset=UTF-8";
    private static final String CONTENT_TYPE_SSE = "text/event-stream; charset=UTF-8";
    private static final String STATIC_RESOURCE_PREFIX = "/de/petanqueturniermanager/webserver/static";
    private static final String GONG_RESOURCE = "de/petanqueturniermanager/timer/gong.wav";
    private static final Gson GSON = new Gson();

    private final int port;
    private final WebServerManager manager;
    private final HttpServer httpServer;
    private final Map<String, CopyOnWriteArrayList<ZielVerbindung>> verbindungenNachSlug = new ConcurrentHashMap<>();
    private final ScheduledExecutorService splitPersistenzExecutor;
    private final Map<String, PendingSplit> pendingSplits = new ConcurrentHashMap<>();
    private volatile boolean laeuft = false;

    public WebserverRegieServerInstanz(int port, WebServerManager manager) throws IOException {
        this.port = port;
        this.manager = manager;
        httpServer = HttpServer.create(new InetSocketAddress(port), 10);
        httpServer.setExecutor(Executors.newCachedThreadPool());
        httpServer.createContext("/", this::handleRequest);
        splitPersistenzExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
            var t = new Thread(r, "PTM-Webserver-Regie-SplitPersistenz");
            t.setDaemon(true);
            return t;
        });
    }

    public int getPort() {
        return port;
    }

    public boolean laeuft() {
        return laeuft;
    }

    public void starten() {
        httpServer.start();
        laeuft = true;
        splitPersistenzExecutor.scheduleWithFixedDelay(this::persistierePendingSplits,
                600, 600, TimeUnit.MILLISECONDS);
        logger.info("Webserver-Regie gestartet auf Port {}", port);
    }

    public void stoppen() {
        laeuft = false;
        splitPersistenzExecutor.shutdownNow();
        persistierePendingSplits();
        for (var liste : verbindungenNachSlug.values()) {
            for (var verbindung : liste) {
                verbindung.entfernenUndSchliessen();
            }
        }
        verbindungenNachSlug.clear();
        httpServer.stop(0);
        logger.info("Webserver-Regie gestoppt auf Port {}", port);
    }

    private void handleRequest(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if ("/".equals(path) || path.isBlank()) {
            sendeHinweisSeite(exchange, 404, I18n.get("webserver.regie.hinweis.kein.ziel.titel"),
                    I18n.get("webserver.regie.hinweis.kein.ziel.text"));
            return;
        }
        var teile = zerlegePfad(path);
        if (teile.slug().isBlank()) {
            sendeHinweisSeite(exchange, 404, I18n.get("webserver.regie.hinweis.kein.ziel.titel"),
                    I18n.get("webserver.regie.hinweis.kein.ziel.text"));
            return;
        }
        var ziel = zielFuerSlug(teile.slug());
        if (ziel == null || !ziel.aktiv()) {
            sendeHinweisSeite(exchange, 404, I18n.get("webserver.regie.hinweis.ziel.inaktiv.titel"),
                    I18n.get("webserver.regie.hinweis.ziel.inaktiv.text", teile.slug()));
            return;
        }
        String rest = teile.rest();
        if (rest.isEmpty()) {
            redirect(exchange, "/" + teile.slug() + "/");
            return;
        }
        var quelle = manager.laufendeRegieQuelleFuerId(ziel.viewId()).orElse(null);
        if ("/events".equals(rest)) {
            handleEvents(exchange, ziel, quelle);
        } else if ("/steuerung/split".equals(rest)) {
            handleSplitSteuerung(exchange, ziel, quelle);
        } else if ("/".equals(rest)) {
            if (quelle == null) {
                sendeHinweisSeite(exchange, 503, I18n.get("webserver.regie.hinweis.quelle.inaktiv.titel"),
                        I18n.get("webserver.regie.hinweis.quelle.inaktiv.text"));
            } else {
                serviereRessource(exchange, "/index.html", CONTENT_TYPE_HTML);
            }
        } else if (rest.startsWith("/assets/")) {
            serviereRessource(exchange, rest, ermittleContentType(rest));
        } else if (rest.startsWith("/images/")) {
            serviereRessource(exchange, rest, ermittleContentType(rest));
        } else if ("/gong.wav".equals(rest)) {
            serviereClasspathRessource(exchange, GONG_RESOURCE, "audio/wav", "public, max-age=3600");
        } else {
            exchange.sendResponseHeaders(404, -1);
        }
    }

    private void handleEvents(HttpExchange exchange, RegieZielRoh ziel, RegieQuelle quelle) throws IOException {
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
        if (quelle == null) {
            var hinweis = GSON.toJson(CompositeSseNachricht.hinweis(
                    I18n.get("webserver.regie.hinweis.quelle.inaktiv.titel"),
                    I18n.get("webserver.regie.hinweis.quelle.inaktiv.text")));
            try {
                os.write("retry: 3000\n\n".getBytes(StandardCharsets.UTF_8));
                os.write(("data: " + hinweis + "\n\n").getBytes(StandardCharsets.UTF_8));
                os.flush();
            } finally {
                os.close();
            }
            return;
        }

        var parent = new RegieSseEltern(quelle, ziel.splitSteuerungFuerView(), v -> entferneVerbindung(ziel.slug(), quelle, v));
        var verbindung = new SseVerbindung(os, parent, RegieQuelle.REGIE_ROLLE, "");
        quelle.regieVerbindungHinzufuegen(verbindung);
        verbindungenNachSlug.computeIfAbsent(ziel.slug(), s -> new CopyOnWriteArrayList<>())
                .add(new ZielVerbindung(ziel.slug(), quelle, verbindung));
        try {
            os.write("retry: 3000\n\n".getBytes(StandardCharsets.UTF_8));
            os.flush();
        } catch (IOException e) {
            entferneVerbindung(ziel.slug(), quelle, verbindung);
            return;
        }
        verbindung.sendeInitNachricht();
    }

    private void handleSplitSteuerung(HttpExchange exchange, RegieZielRoh ziel, RegieQuelle quelle) throws IOException {
        if (quelle == null || !quelle.unterstuetztSplitSteuerung()) {
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
            String splitJson = quelle.splitSteuerungAnwenden(new String(body, StandardCharsets.UTF_8));
            pendingSplits.put(ziel.id() + "\n" + ziel.viewId(), new PendingSplit(ziel.id(), ziel.viewId(), splitJson));
            verbindungenNachSlug.getOrDefault(ziel.slug(), new CopyOnWriteArrayList<>()).forEach(v -> v.verbindung().senden(splitJson));
            exchange.sendResponseHeaders(204, -1);
        } catch (IllegalArgumentException e) {
            logger.debug("Ungültige Regie-Split-Steuerung für Ziel {}: {}", ziel.slug(), e.getMessage());
            exchange.sendResponseHeaders(400, -1);
        } finally {
            exchange.close();
        }
    }

    private void entferneVerbindung(String slug, RegieQuelle quelle, SseVerbindung verbindung) {
        quelle.regieVerbindungEntfernen(verbindung);
        var liste = verbindungenNachSlug.get(slug);
        if (liste != null) {
            liste.removeIf(v -> v.verbindung() == verbindung);
            if (liste.isEmpty()) {
                verbindungenNachSlug.remove(slug);
            }
        }
    }

    private void persistierePendingSplits() {
        var pending = new ArrayList<>(pendingSplits.values());
        if (pending.isEmpty()) {
            return;
        }
        pendingSplits.clear();
        for (var split : pending) {
            GlobalProperties.get().speichernWebserverRegieSplitSteuerung(
                    split.zielId(), split.viewId(), split.splitJson());
        }
    }

    private RegieZielRoh zielFuerSlug(String slug) {
        return GlobalProperties.get().getWebserverRegieZiele().stream()
                .filter(z -> z.slug().equals(slug))
                .findFirst()
                .orElse(null);
    }

    private static PfadTeile zerlegePfad(String path) {
        String ohneStart = path.startsWith("/") ? path.substring(1) : path;
        int trenner = ohneStart.indexOf('/');
        if (trenner < 0) {
            return new PfadTeile(ohneStart, "");
        }
        return new PfadTeile(ohneStart.substring(0, trenner), ohneStart.substring(trenner));
    }

    private void redirect(HttpExchange exchange, String ziel) throws IOException {
        exchange.getResponseHeaders().set("Location", ziel);
        exchange.sendResponseHeaders(302, -1);
    }

    private void sendeHinweisSeite(HttpExchange exchange, int status, String titel, String text) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        String html = """
                <!doctype html>
                <html lang="de">
                <head><meta charset="utf-8"><title>%s</title></head>
                <body style="font-family:sans-serif;margin:2rem;line-height:1.4">
                <h1>%s</h1>
                <p>%s</p>
                </body></html>
                """.formatted(escapeHtml(titel), escapeHtml(titel), escapeHtml(text));
        byte[] body = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_HTML);
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void serviereRessource(HttpExchange exchange, String relativerPfad, String contentType)
            throws IOException {
        String ressourcePfad = (STATIC_RESOURCE_PREFIX + relativerPfad).replaceFirst("^/", "");
        serviereClasspathRessource(exchange, ressourcePfad, contentType, "no-cache");
    }

    private void serviereClasspathRessource(HttpExchange exchange, String ressourcePfad, String contentType,
            String cacheControl) throws IOException {
        if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(405, -1);
            return;
        }
        InputStream gefunden = getClass().getClassLoader().getResourceAsStream(ressourcePfad);
        if (gefunden == null) {
            gefunden = getClass().getResourceAsStream("/" + ressourcePfad);
        }
        if (gefunden == null) {
            logger.warn("Regie-Classpath-Ressource nicht gefunden: {}", ressourcePfad);
            exchange.sendResponseHeaders(404, -1);
            return;
        }
        try (InputStream in = gefunden) {
            byte[] body = in.readAllBytes();
            var headers = exchange.getResponseHeaders();
            headers.set("Content-Type", contentType);
            headers.set("Cache-Control", cacheControl);
            headers.set("Access-Control-Allow-Origin", "*");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        }
    }

    private static String escapeHtml(String text) {
        return text == null ? "" : text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private static String ermittleContentType(String dateiname) {
        if (dateiname.endsWith(".js")) return "text/javascript; charset=UTF-8";
        if (dateiname.endsWith(".css")) return "text/css; charset=UTF-8";
        if (dateiname.endsWith(".html")) return CONTENT_TYPE_HTML;
        if (dateiname.endsWith(".svg")) return "image/svg+xml";
        if (dateiname.endsWith(".png")) return "image/png";
        if (dateiname.endsWith(".ico")) return "image/x-icon";
        if (dateiname.endsWith(".wav")) return "audio/wav";
        return "application/octet-stream";
    }

    private record PfadTeile(String slug, String rest) {
    }

    private record PendingSplit(String zielId, String viewId, String splitJson) {
    }

    private record ZielVerbindung(String slug, RegieQuelle quelle, SseVerbindung verbindung) {
        void entfernenUndSchliessen() {
            quelle.regieVerbindungEntfernen(verbindung);
            verbindung.schliessen();
        }
    }

    private static final class RegieSseEltern implements SseElternInstanz {
        private final RegieQuelle quelle;
        private final String splitSteuerung;
        private final VerbindungEntferner entferner;

        RegieSseEltern(RegieQuelle quelle, String splitSteuerung, VerbindungEntferner entferner) {
            this.quelle = quelle;
            this.splitSteuerung = splitSteuerung;
            this.entferner = entferner;
        }

        @Override
        public String getCachedInitJson() {
            return quelle.getCachedInitJson();
        }

        @Override
        public String[] getInitZusatzJsons() {
            List<String> result = new ArrayList<>();
            for (String json : quelle.getInitZusatzJsons()) {
                if (!CompositeViewInstanz.istSplitSteuerung(json)) {
                    result.add(json);
                }
            }
            if (splitSteuerung != null && !splitSteuerung.isBlank()) {
                result.add(splitSteuerung);
            }
            return result.toArray(String[]::new);
        }

        @Override
        public void verbindungEntfernen(SseVerbindung verbindung) {
            entferner.entfernen(verbindung);
        }
    }

    @FunctionalInterface
    private interface VerbindungEntferner {
        void entfernen(SseVerbindung verbindung);
    }
}
