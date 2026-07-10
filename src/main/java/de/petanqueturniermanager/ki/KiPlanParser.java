/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ki;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public final class KiPlanParser {

    private static final Gson GSON = new Gson();
    private static final Type AKTION_LIST_TYPE = new TypeToken<List<KiAktion>>() { }.getType();
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() { }.getType();

    private KiPlanParser() {
    }

    public static KiPlan parse(String text) {
        String json = extrahiereJson(text);
        JsonObject root = GSON.fromJson(json, JsonObject.class);
        if (root == null) {
            throw new IllegalArgumentException("KI-Antwort enthält keinen JSON-Plan");
        }
        List<KiAktion> actions = root.has("actions")
                ? GSON.fromJson(root.get("actions"), AKTION_LIST_TYPE)
                : List.of();
        List<String> warnings = root.has("warnings")
                ? GSON.fromJson(root.get("warnings"), STRING_LIST_TYPE)
                : List.of();
        return new KiPlan(
                string(root, "summary"),
                !root.has("requiresConfirmation") || root.get("requiresConfirmation").getAsBoolean(),
                actions,
                warnings,
                string(root, "dataPreview"));
    }

    static String extrahiereJson(String text) {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("KI-Antwort ist leer");
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstLineEnd = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstLineEnd >= 0 && lastFence > firstLineEnd) {
                trimmed = trimmed.substring(firstLineEnd + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new IllegalArgumentException("KI-Antwort enthält keinen JSON-Plan");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String string(JsonObject root, String name) {
        return root.has(name) && !root.get(name).isJsonNull() ? root.get(name).getAsString() : "";
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> parameter(KiAktion aktion) {
        return aktion.parameters();
    }
}
