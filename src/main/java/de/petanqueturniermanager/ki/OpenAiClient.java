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

public final class OpenAiClient extends AbstractHttpKiClient {

    public OpenAiClient(KiOptionen optionen) {
        super(optionen);
    }

    OpenAiClient(HttpClient httpClient, KiOptionen optionen) {
        super(httpClient, optionen);
    }

    @Override
    String anbieterName() {
        return "OpenAI";
    }

    @Override
    URI endpoint() {
        return URI.create(basisUrl() + "/responses");
    }

    @Override
    URI modelleEndpoint() {
        return URI.create(basisUrl() + "/models");
    }

    @Override
    HttpRequest.Builder authHeader(HttpRequest.Builder builder) {
        return builder.header("Authorization", "Bearer " + optionen.apiKey());
    }

    @Override
    String requestJson(String prompt) {
        JsonObject root = new JsonObject();
        root.addProperty("model", optionen.model());
        root.addProperty("store", false);
        root.addProperty("input", prompt);
        JsonObject text = new JsonObject();
        JsonObject format = new JsonObject();
        format.addProperty("type", "text");
        text.add("format", format);
        root.add("text", text);
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
