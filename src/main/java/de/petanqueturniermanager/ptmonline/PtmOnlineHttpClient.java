/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import com.google.gson.Gson;

import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;

/**
 * Gemeinsame HTTP-Orchestrierung (Request-Aufbau, Bearer-Auth, Statuscode-Pruefung) fuer alle
 * Clients gegen die PTM-Online REST-API. Nur freigeschaltete (vom Admin genehmigte) API-Schluessel
 * werden von PTM-Online akzeptiert.
 */
abstract class PtmOnlineHttpClient {

    static final Gson GSON = new Gson();
    static final Duration STANDARD_TIMEOUT = Duration.ofSeconds(30);

    private final HttpClient httpClient;
    private final String baseUrl;
    private final String apiKey;
    private final String syncDocumentId;
    private final String leaseToken;
    private final Duration timeout;

    PtmOnlineHttpClient(String baseUrl, String apiKey) {
        this(HttpClient.newBuilder().connectTimeout(STANDARD_TIMEOUT).build(), baseUrl, apiKey, null, null);
    }

    PtmOnlineHttpClient(HttpClient httpClient, String baseUrl, String apiKey) {
        this(httpClient, baseUrl, apiKey, null, null);
    }

    PtmOnlineHttpClient(HttpClient httpClient, String baseUrl, String apiKey, String syncDocumentId, String leaseToken) {
        this(httpClient, baseUrl, apiKey, syncDocumentId, leaseToken, STANDARD_TIMEOUT);
    }

    /** @param timeout Zeitlimit je Anfrage (Verbindungsaufbau und Antwort) */
    PtmOnlineHttpClient(HttpClient httpClient, String baseUrl, String apiKey, String syncDocumentId, String leaseToken,
            Duration timeout) {
        this.timeout = timeout;
        this.httpClient = httpClient;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.apiKey = apiKey;
        this.syncDocumentId = syncDocumentId;
        this.leaseToken = leaseToken;
    }

    final URI uri(String path) {
        return URI.create(baseUrl + (path.startsWith("/") ? path : "/" + path));
    }

    final HttpResponse<String> get(String path) throws IOException, InterruptedException {
        return send(authorized(HttpRequest.newBuilder(uri(path))).GET());
    }

    final HttpResponse<String> post(String path, String jsonBody) throws IOException, InterruptedException {
        return send(authorized(HttpRequest.newBuilder(uri(path)))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody)));
    }

    /**
     * Sendet einen gezählten Schreibauftrag mit Auftrags-ID und Schreibzähler (T-19, T-23). Anders als die übrigen
     * Aufrufe wirft eine Ablehnung keine Ausnahme: die Antwort wird unabhängig vom Statuscode geliefert, damit der
     * Versand zwischen fachlicher Ablehnung und Wiederholung unterscheiden kann.
     *
     * @throws IOException nur, wenn PTM-Online nicht erreichbar war
     */
    final SyncAntwort sendeAuftrag(SyncAuftrag auftrag) throws IOException, InterruptedException {
        HttpRequest.Builder builder = authorized(HttpRequest.newBuilder(uri(auftrag.pfad())))
                .header("X-PTM-Request-Id", auftrag.auftragsId())
                .header("X-PTM-Sync-Counter", Long.toString(auftrag.zaehler()));
        if (auftrag.body().isEmpty()) {
            builder.method(auftrag.methode(), HttpRequest.BodyPublishers.noBody());
        } else {
            builder.header("Content-Type", "application/json")
                    .method(auftrag.methode(), HttpRequest.BodyPublishers.ofString(auftrag.body()));
        }
        HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        return new SyncAntwort(response.statusCode(), response.body(),
                response.headers().firstValue("X-PTM-Replayed").isPresent());
    }

    private HttpRequest.Builder authorized(HttpRequest.Builder builder) {
        builder.header("Authorization", "Bearer " + apiKey).timeout(timeout);
        if (syncDocumentId != null && leaseToken != null) {
            builder.header("X-PTM-Sync-Document", syncDocumentId).header("X-PTM-Sync-Lease", leaseToken);
        }
        return builder;
    }

    private HttpResponse<String> send(HttpRequest.Builder requestBuilder) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new PtmOnlineHttpException(response.statusCode(), response.body());
        }
        return response;
    }
}
