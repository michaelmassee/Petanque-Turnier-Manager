/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public final class GeminiClient extends AbstractHttpKiClient {

    public GeminiClient(KiOptionen optionen) {
        super(optionen);
    }

    GeminiClient(HttpClient httpClient, KiOptionen optionen) {
        super(httpClient, optionen);
    }

    @Override
    String anbieterName() {
        return "Gemini";
    }

    @Override
    URI endpoint() {
        return URI.create(basisUrl() + "/models/" + optionen.model() + ":generateContent");
    }

    @Override
    URI modelleEndpoint() {
        return URI.create(basisUrl() + "/models");
    }

    @Override
    HttpRequest.Builder authHeader(HttpRequest.Builder builder) {
        return builder.header("x-goog-api-key", optionen.apiKey());
    }

    @Override
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

    @Override
    String parseResponseText(String body) {
        return responseText(body);
    }

    @Override
    List<String> parseModelIds(String body) {
        return modelIds(body);
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
