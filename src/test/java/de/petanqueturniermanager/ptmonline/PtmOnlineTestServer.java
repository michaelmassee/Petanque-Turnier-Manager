/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;

/**
 * Lokaler Ersatz für den Anmeldungs-Endpunkt von PTM-Online in UITests: beantwortet jeden Aufruf von
 * {@code /api/sync/tournaments/<id>/registrations} mit einer festen Anmeldungsliste, zählt Online-Anlagen
 * (PUT) und kann Antworten zurückhalten, um einen Abbruch während eines Serveraufrufs zu testen.
 * Online-Anlagen und Status-Pushes ({@code /results}) werden für Prüfungen aufgezeichnet.
 */
final class PtmOnlineTestServer implements AutoCloseable {

    private static final long MAX_WARTEZEIT_SEKUNDEN = 30;

    private final HttpServer server;
    private final String anmeldungenJson;
    private final AtomicInteger anzahlOnlineAngelegt = new AtomicInteger();
    private final List<JsonObject> onlineAngelegt = new CopyOnWriteArrayList<>();
    private final List<JsonObject> gepushteErgebnisse = new CopyOnWriteArrayList<>();
    private final CountDownLatch abrufAngekommen = new CountDownLatch(1);
    private volatile CountDownLatch antwortFreigabe = new CountDownLatch(0);

    PtmOnlineTestServer(String turnierId, String anmeldungenJson) throws IOException {
        this.anmeldungenJson = anmeldungenJson;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/sync/tournaments/" + turnierId + "/registrations", this::beantworte);
        server.createContext("/api/sync/tournaments/" + turnierId + "/results", this::ergebnisseAnnehmen);
        server.start();
    }

    LibreOfficePtmOnlineSpeicher.Zugangsdaten zugangsdaten() {
        return new LibreOfficePtmOnlineSpeicher.Zugangsdaten("ptm_test",
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    int anzahlOnlineAngelegt() {
        return anzahlOnlineAngelegt.get();
    }

    /** Request-Bodies der Online-Anlagen (PUT), in Eingangsreihenfolge. */
    List<JsonObject> onlineAngelegt() {
        return List.copyOf(onlineAngelegt);
    }

    /** Alle per {@code /results} gepushten Einträge, in Eingangsreihenfolge. */
    List<JsonObject> gepushteErgebnisse() {
        return List.copyOf(gepushteErgebnisse);
    }

    /** Hält alle folgenden Antworten zurück, bis {@link #close()} aufgerufen wird. */
    void antwortenZurueckhalten() {
        antwortFreigabe = new CountDownLatch(1);
    }

    boolean warteAufErstenAbruf() throws InterruptedException {
        return abrufAngekommen.await(MAX_WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS);
    }

    @Override
    public void close() {
        antwortFreigabe.countDown();
        server.stop(0);
    }

    private void beantworte(HttpExchange exchange) throws IOException {
        abrufAngekommen.countDown();
        try {
            antwortFreigabe.await(MAX_WARTEZEIT_SEKUNDEN, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            antworte(exchange, angelegteAnmeldung(exchange));
            return;
        }
        antworte(exchange, anmeldungenJson);
    }

    /** Antwort wie PTM-Online: die angelegte Anmeldung mit neuer Online-Id und den gesendeten Namen. */
    private String angelegteAnmeldung(HttpExchange exchange) throws IOException {
        JsonObject anmeldung = leseBody(exchange).getAsJsonObject();
        onlineAngelegt.add(anmeldung);
        JsonObject registration = anmeldung.deepCopy();
        registration.addProperty("id", "online-" + anzahlOnlineAngelegt.incrementAndGet());
        registration.addProperty("status", "confirmed");
        registration.addProperty("executionRevision", 1);
        JsonObject antwort = new JsonObject();
        antwort.add("registration", registration);
        return antwort.toString();
    }

    private void ergebnisseAnnehmen(HttpExchange exchange) throws IOException {
        JsonArray registrations = leseBody(exchange).getAsJsonObject().getAsJsonArray("registrations");
        registrations.forEach(eintrag -> gepushteErgebnisse.add(eintrag.getAsJsonObject()));
        JsonObject antwort = new JsonObject();
        antwort.addProperty("updatedCount", registrations.size());
        antworte(exchange, antwort.toString());
    }

    private static JsonElement leseBody(HttpExchange exchange) throws IOException {
        return JsonParser.parseString(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
    }

    private static void antworte(HttpExchange exchange, String json) throws IOException {
        byte[] antwort = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, antwort.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(antwort);
        }
    }
}
