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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class ClaudeClient implements KiClient {

    private static final Gson GSON = new Gson();
    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;

    private final HttpClient httpClient;
    private final KiOptionen optionen;

    public ClaudeClient(KiOptionen optionen) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .build(), optionen);
    }

    ClaudeClient(HttpClient httpClient, KiOptionen optionen) {
        this.httpClient = httpClient;
        this.optionen = optionen;
    }

    @Override
    public String erstelleAntwort(String prompt) throws IOException, InterruptedException {
        if (!optionen.istApiVollstaendig()) {
            throw new IllegalStateException("Claude API-Konfiguration ist unvollstaendig");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .header("x-api-key", optionen.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson(prompt)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Claude API Fehler " + response.statusCode() + ": " + response.body());
        }
        return responseText(response.body());
    }

    @Override
    public List<String> listeModelle() throws IOException, InterruptedException {
        if (!optionen.istApiVollstaendig()) {
            throw new IllegalStateException("Claude API-Konfiguration ist unvollstaendig");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(basisUrl() + "/v1/models"))
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .header("x-api-key", optionen.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Claude API Fehler " + response.statusCode() + ": " + response.body());
        }
        return modelIds(response.body());
    }

    static List<String> modelIds(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null || !root.has("data")) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (var item : root.getAsJsonArray("data")) {
            JsonObject obj = item.getAsJsonObject();
            if (obj.has("id")) {
                ids.add(obj.get("id").getAsString());
            }
        }
        Collections.sort(ids);
        return ids;
    }

    String requestJson(String prompt) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);
        JsonArray messages = new JsonArray();
        messages.add(message);
        JsonObject root = new JsonObject();
        root.addProperty("model", optionen.model());
        root.addProperty("max_tokens", MAX_TOKENS);
        root.add("messages", messages);
        return GSON.toJson(root);
    }

    private URI endpoint() {
        return URI.create(basisUrl() + "/v1/messages");
    }

    private String basisUrl() {
        return optionen.baseUrl().endsWith("/")
                ? optionen.baseUrl().substring(0, optionen.baseUrl().length() - 1)
                : optionen.baseUrl();
    }

    static String responseText(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null || !root.has("content")) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (var item : root.getAsJsonArray("content")) {
            JsonObject itemObj = item.getAsJsonObject();
            if (itemObj.has("text")) {
                text.append(itemObj.get("text").getAsString());
            }
        }
        return text.toString();
    }
}
