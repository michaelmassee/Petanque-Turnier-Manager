/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

import com.google.gson.Gson;

/**
 * Gemeinsame HTTP-Orchestrierung (Request-Aufbau, Statuscode-Pruefung, Basis-URL-Normalisierung)
 * fuer alle KI-Anbieter-Clients. Anbieterspezifisch bleiben nur Endpunkte, Auth-Header und
 * JSON-Formate (Request-Body sowie Antwort-/Modell-Parsing) in den Subklassen.
 */
abstract class AbstractHttpKiClient implements KiClient {

    static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    final KiOptionen optionen;

    AbstractHttpKiClient(KiOptionen optionen) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .build(), optionen);
    }

    AbstractHttpKiClient(HttpClient httpClient, KiOptionen optionen) {
        this.httpClient = httpClient;
        this.optionen = optionen;
    }

    abstract String anbieterName();

    abstract URI endpoint();

    abstract URI modelleEndpoint();

    abstract HttpRequest.Builder authHeader(HttpRequest.Builder builder);

    abstract String requestJson(String prompt);

    abstract String parseResponseText(String body);

    abstract List<String> parseModelIds(String body);

    @Override
    public final String erstelleAntwort(String prompt) throws IOException, InterruptedException {
        pruefeKonfiguration();
        HttpRequest request = authHeader(HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden())))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson(prompt)))
                .build();
        return parseResponseText(sende(request).body());
    }

    @Override
    public final List<String> listeModelle() throws IOException, InterruptedException {
        pruefeKonfiguration();
        HttpRequest request = authHeader(HttpRequest.newBuilder(modelleEndpoint())
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden())))
                .GET()
                .build();
        return parseModelIds(sende(request).body());
    }

    private void pruefeKonfiguration() {
        if (!optionen.istApiVollstaendig()) {
            throw new IllegalStateException(anbieterName() + " API-Konfiguration ist unvollstaendig");
        }
    }

    private HttpResponse<String> sende(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(anbieterName() + " API Fehler " + response.statusCode() + ": " + response.body());
        }
        return response;
    }

    final String basisUrl() {
        return optionen.baseUrl().endsWith("/")
                ? optionen.baseUrl().substring(0, optionen.baseUrl().length() - 1)
                : optionen.baseUrl();
    }
}
