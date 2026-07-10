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

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class OpenAiClient {

    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final KiOptionen optionen;

    public OpenAiClient(KiOptionen optionen) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .build(), optionen);
    }

    OpenAiClient(HttpClient httpClient, KiOptionen optionen) {
        this.httpClient = httpClient;
        this.optionen = optionen;
    }

    public String erstelleAntwort(String prompt) throws IOException, InterruptedException {
        if (optionen.apiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API-Key ist nicht konfiguriert");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .header("Authorization", "Bearer " + optionen.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson(prompt)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAI API Fehler " + response.statusCode() + ": " + response.body());
        }
        return responseText(response.body());
    }

    String requestJson(String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", optionen.model());
        root.addProperty("store", false);
        root.addProperty("input", prompt);
        JsonObject text = new JsonObject();
        text.addProperty("format", "text");
        root.add("text", text);
        return GSON.toJson(root);
    }

    private URI endpoint() {
        String base = optionen.baseUrl().endsWith("/")
                ? optionen.baseUrl().substring(0, optionen.baseUrl().length() - 1)
                : optionen.baseUrl();
        return URI.create(base + "/responses");
    }

    static String responseText(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null) {
            return "";
        }
        if (root.has("output_text")) {
            return root.get("output_text").getAsString();
        }
        JsonArray output = root.has("output") ? root.getAsJsonArray("output") : new JsonArray();
        StringBuilder text = new StringBuilder();
        for (var item : output) {
            JsonObject obj = item.getAsJsonObject();
            JsonArray content = obj.has("content") ? obj.getAsJsonArray("content") : new JsonArray();
            for (var contentItem : content) {
                JsonObject contentObj = contentItem.getAsJsonObject();
                if (contentObj.has("text")) {
                    text.append(contentObj.get("text").getAsString());
                }
            }
        }
        return text.toString();
    }
}
