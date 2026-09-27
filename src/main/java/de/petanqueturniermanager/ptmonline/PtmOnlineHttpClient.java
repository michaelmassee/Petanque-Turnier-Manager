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

    final HttpResponse<String> put(String path, String jsonBody) throws IOException, InterruptedException {
        return send(authorized(HttpRequest.newBuilder(uri(path)))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(jsonBody)));
    }

    final HttpResponse<String> delete(String path) throws IOException, InterruptedException {
        return send(authorized(HttpRequest.newBuilder(uri(path))).DELETE());
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
