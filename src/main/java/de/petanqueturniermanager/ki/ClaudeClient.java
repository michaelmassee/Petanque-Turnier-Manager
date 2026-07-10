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

public final class ClaudeClient extends AbstractHttpKiClient {

    private static final String ANTHROPIC_VERSION = "2023-06-01";
    private static final int MAX_TOKENS = 4096;

    public ClaudeClient(KiOptionen optionen) {
        super(optionen);
    }

    ClaudeClient(HttpClient httpClient, KiOptionen optionen) {
        super(httpClient, optionen);
    }

    @Override
    String anbieterName() {
        return "Claude";
    }

    @Override
    URI endpoint() {
        return URI.create(basisUrl() + "/v1/messages");
    }

    @Override
    URI modelleEndpoint() {
        return URI.create(basisUrl() + "/v1/models");
    }

    @Override
    HttpRequest.Builder authHeader(HttpRequest.Builder builder) {
        return builder
                .header("x-api-key", optionen.apiKey())
                .header("anthropic-version", ANTHROPIC_VERSION);
    }

    @Override
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
