package de.petanqueturniermanager.spielerdb.webview;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import de.petanqueturniermanager.spielerdb.LabelRepository;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.SpielerRepository;
import de.petanqueturniermanager.spielerdb.VereinRepository;

/**
 * JSON-API für den read-only Web-Viewer der Spieler-DB. Antworten:
 * <ul>
 *   <li>{@code GET /api/spieler?q=&limit=&offset=} → paginiertes
 *       {@code {items, total, limit, offset}}.</li>
 *   <li>{@code GET /api/spieler/{nr}} → einzelner Spieler oder 404.</li>
 *   <li>{@code GET /api/vereine}, {@code /api/labels}, {@code /api/stats}.</li>
 * </ul>
 * Kein CORS, da same-origin localhost.
 */
final class SpielerDbApiHandler implements HttpHandler {

    private static final Logger logger = LogManager.getLogger(SpielerDbApiHandler.class);

    private static final int DEFAULT_LIMIT = 200;
    private static final int MAX_LIMIT = 1000;
    private static final String CONTENT_TYPE_JSON = "application/json; charset=UTF-8";

    private final SpielerRepository spielerRepo;
    private final VereinRepository vereinRepo;
    private final LabelRepository labelRepo;
    private final String dbPfad;
    private final Gson gson;

    SpielerDbApiHandler(SpielerRepository spielerRepo, VereinRepository vereinRepo,
            LabelRepository labelRepo, String dbPfad) {
        this.spielerRepo = spielerRepo;
        this.vereinRepo = vereinRepo;
        this.labelRepo = labelRepo;
        this.dbPfad = dbPfad;
        this.gson = new GsonBuilder().disableHtmlEscaping().create();
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try (exchange) {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            String path = exchange.getRequestURI().getPath();
            try {
                if ("/api/stats".equals(path)) {
                    sendeJson(exchange, 200, baueStats());
                } else if ("/api/vereine".equals(path)) {
                    sendeJson(exchange, 200, vereinRepo.findAll());
                } else if ("/api/labels".equals(path)) {
                    sendeJson(exchange, 200, labelRepo.findAll());
                } else if ("/api/spieler".equals(path)) {
                    handleSpielerListe(exchange);
                } else if (path.startsWith("/api/spieler/")) {
                    handleSpielerEinzeln(exchange, path.substring("/api/spieler/".length()));
                } else {
                    exchange.sendResponseHeaders(404, -1);
                }
            } catch (SpielerDbException e) {
                logger.error("Spieler-DB-API-Fehler bei {}", path, e);
                sendeFehler(exchange, 500, e.getMessage());
            } catch (RuntimeException e) {
                logger.error("Unerwarteter Fehler bei {}", path, e);
                sendeFehler(exchange, 500, e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }
    }

    private void handleSpielerListe(HttpExchange exchange) throws SpielerDbException, IOException {
        Map<String, String> params = parseQuery(exchange.getRequestURI());
        String q = params.getOrDefault("q", "").strip();
        int limit = leseInt(params.get("limit"), DEFAULT_LIMIT, 1, MAX_LIMIT);
        int offset = leseInt(params.get("offset"), 0, 0, Integer.MAX_VALUE);

        List<SpielerMitVerein> alle = q.isEmpty()
                ? spielerRepo.findAll()
                : spielerRepo.findeMitWildcard(q, true, MAX_LIMIT);

        int total = alle.size();
        int von = Math.min(offset, total);
        int bis = Math.min(von + limit, total);
        List<SpielerMitVerein> seite = alle.subList(von, bis);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", seite);
        body.put("total", total);
        body.put("limit", limit);
        body.put("offset", offset);
        sendeJson(exchange, 200, body);
    }

    private void handleSpielerEinzeln(HttpExchange exchange, String idRoh) throws IOException, SpielerDbException {
        int nr;
        try {
            nr = Integer.parseInt(idRoh);
        } catch (NumberFormatException e) {
            sendeFehler(exchange, 400, "Ungültige Spieler-Nr: " + idRoh);
            return;
        }
        Optional<SpielerMitVerein> treffer = spielerRepo.findById(nr);
        if (treffer.isEmpty()) {
            sendeFehler(exchange, 404, "Spieler nicht gefunden: " + nr);
            return;
        }
        sendeJson(exchange, 200, treffer.get());
    }

    private Map<String, Object> baueStats() throws SpielerDbException {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("spielerCount", spielerRepo.anzahl());
        stats.put("vereineCount", vereinRepo.findAll().size());
        stats.put("labelsCount", labelRepo.findAll().size());
        stats.put("dbPath", dbPfad);
        return stats;
    }

    private void sendeJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] bytes = gson.toJson(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", CONTENT_TYPE_JSON);
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private void sendeFehler(HttpExchange exchange, int status, @Nullable String nachricht) throws IOException {
        Map<String, String> body = Map.of("error", nachricht == null ? "Unbekannter Fehler" : nachricht);
        sendeJson(exchange, status, body);
    }

    /** Liefert default, falls roh null/ungültig; clamped auf [min, max]. */
    private static int leseInt(@Nullable String roh, int defaultWert, int min, int max) {
        if (roh == null || roh.isEmpty()) {
            return defaultWert;
        }
        try {
            return Math.max(min, Math.min(max, Integer.parseInt(roh)));
        } catch (NumberFormatException e) {
            return defaultWert;
        }
    }

    private static Map<String, String> parseQuery(URI uri) {
        String roh = uri.getRawQuery();
        if (roh == null || roh.isEmpty()) {
            return Map.of();
        }
        Map<String, String> ergebnis = new HashMap<>();
        for (String paar : roh.split("&")) {
            int eq = paar.indexOf('=');
            String name = eq < 0 ? paar : paar.substring(0, eq);
            String wert = eq < 0 ? "" : paar.substring(eq + 1);
            ergebnis.put(URLDecoder.decode(name, StandardCharsets.UTF_8),
                    URLDecoder.decode(wert, StandardCharsets.UTF_8));
        }
        return ergebnis;
    }

    /** Test-Helfer: erlaubt direkten Aufruf in Unit-Tests ohne HttpServer. */
    String renderStatsJson() throws SpielerDbException {
        return gson.toJson(baueStats());
    }

    /** Test-Helfer: rendert die Spielerliste mit den gegebenen Query-Parametern. */
    String renderSpielerJson(String query) throws SpielerDbException {
        Map<String, String> params = parseQuery(URI.create("/api/spieler?" + query));
        String q = params.getOrDefault("q", "").strip();
        int limit = leseInt(params.get("limit"), DEFAULT_LIMIT, 1, MAX_LIMIT);
        int offset = leseInt(params.get("offset"), 0, 0, Integer.MAX_VALUE);
        List<SpielerMitVerein> alle = q.isEmpty()
                ? spielerRepo.findAll()
                : spielerRepo.findeMitWildcard(q, true, MAX_LIMIT);
        int total = alle.size();
        int von = Math.min(offset, total);
        int bis = Math.min(von + limit, total);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("items", alle.subList(von, bis));
        body.put("total", total);
        body.put("limit", limit);
        body.put("offset", offset);
        return gson.toJson(body);
    }
}
