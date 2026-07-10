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

public final class GeminiClient implements KiClient {

    private static final Gson GSON = new Gson();

    private final HttpClient httpClient;
    private final KiOptionen optionen;

    public GeminiClient(KiOptionen optionen) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .build(), optionen);
    }

    GeminiClient(HttpClient httpClient, KiOptionen optionen) {
        this.httpClient = httpClient;
        this.optionen = optionen;
    }

    @Override
    public String erstelleAntwort(String prompt) throws IOException, InterruptedException {
        if (!optionen.istApiVollstaendig()) {
            throw new IllegalStateException("Gemini API-Konfiguration ist unvollstaendig");
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint())
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .header("x-goog-api-key", optionen.apiKey())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson(prompt)))
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini API Fehler " + response.statusCode() + ": " + response.body());
        }
        return responseText(response.body());
    }

    @Override
    public List<String> listeModelle() throws IOException, InterruptedException {
        if (!optionen.istApiVollstaendig()) {
            throw new IllegalStateException("Gemini API-Konfiguration ist unvollstaendig");
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(basisUrl() + "/models"))
                .timeout(Duration.ofSeconds(optionen.timeoutSekunden()))
                .header("x-goog-api-key", optionen.apiKey())
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Gemini API Fehler " + response.statusCode() + ": " + response.body());
        }
        return modelIds(response.body());
    }

    static List<String> modelIds(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null || !root.has("models")) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        for (var item : root.getAsJsonArray("models")) {
            JsonObject obj = item.getAsJsonObject();
            if (!obj.has("name") || !unterstuetztGenerateContent(obj)) {
                continue;
            }
            String name = obj.get("name").getAsString();
            ids.add(name.startsWith("models/") ? name.substring("models/".length()) : name);
        }
        Collections.sort(ids);
        return ids;
    }

    private static boolean unterstuetztGenerateContent(JsonObject modell) {
        if (!modell.has("supportedGenerationMethods")) {
            return true;
        }
        for (var methode : modell.getAsJsonArray("supportedGenerationMethods")) {
            if ("generateContent".equals(methode.getAsString())) {
                return true;
            }
        }
        return false;
    }

    String requestJson(String prompt) {
        JsonObject part = new JsonObject();
        part.addProperty("text", prompt);
        JsonArray parts = new JsonArray();
        parts.add(part);
        JsonObject content = new JsonObject();
        content.add("parts", parts);
        JsonArray contents = new JsonArray();
        contents.add(content);
        JsonObject root = new JsonObject();
        root.add("contents", contents);
        return GSON.toJson(root);
    }

    private URI endpoint() {
        return URI.create(basisUrl() + "/models/" + optionen.model() + ":generateContent");
    }

    private String basisUrl() {
        return optionen.baseUrl().endsWith("/")
                ? optionen.baseUrl().substring(0, optionen.baseUrl().length() - 1)
                : optionen.baseUrl();
    }

    static String responseText(String body) {
        JsonObject root = GSON.fromJson(body, JsonObject.class);
        if (root == null || !root.has("candidates")) {
            return "";
        }
        StringBuilder text = new StringBuilder();
        for (var candidate : root.getAsJsonArray("candidates")) {
            JsonObject candidateObj = candidate.getAsJsonObject();
            if (!candidateObj.has("content")) {
                continue;
            }
            JsonObject content = candidateObj.getAsJsonObject("content");
            JsonArray parts = content.has("parts") ? content.getAsJsonArray("parts") : new JsonArray();
            for (var part : parts) {
                JsonObject partObj = part.getAsJsonObject();
                if (partObj.has("text")) {
                    text.append(partObj.get("text").getAsString());
                }
            }
        }
        return text.toString();
    }
}
