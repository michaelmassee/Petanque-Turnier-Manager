/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;

/**
 * Lokaler Ersatz für den Anmeldungs-Endpunkt von PTM-Online in UITests: beantwortet jeden Aufruf von
 * {@code /api/sync/tournaments/<id>/registrations} mit einer festen Anmeldungsliste, zählt Online-Anlagen
 * (PUT) und kann Antworten zurückhalten, um einen Abbruch während eines Serveraufrufs zu testen.
 */
final class PtmOnlineTestServer implements AutoCloseable {

    private static final long MAX_WARTEZEIT_SEKUNDEN = 30;

    private final HttpServer server;
    private final String anmeldungenJson;
    private final AtomicInteger anzahlOnlineAngelegt = new AtomicInteger();
    private final CountDownLatch abrufAngekommen = new CountDownLatch(1);
    private volatile CountDownLatch antwortFreigabe = new CountDownLatch(0);

    PtmOnlineTestServer(String turnierId, String anmeldungenJson) throws IOException {
        this.anmeldungenJson = anmeldungenJson;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/sync/tournaments/" + turnierId + "/registrations", this::beantworte);
        server.start();
    }

    LibreOfficePtmOnlineSpeicher.Zugangsdaten zugangsdaten() {
        return new LibreOfficePtmOnlineSpeicher.Zugangsdaten("ptm_test",
                "http://127.0.0.1:" + server.getAddress().getPort());
    }

    int anzahlOnlineAngelegt() {
        return anzahlOnlineAngelegt.get();
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
            anzahlOnlineAngelegt.incrementAndGet();
        }
        byte[] antwort = anmeldungenJson.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, antwort.length);
        try (OutputStream body = exchange.getResponseBody()) {
            body.write(antwort);
        }
    }
}
