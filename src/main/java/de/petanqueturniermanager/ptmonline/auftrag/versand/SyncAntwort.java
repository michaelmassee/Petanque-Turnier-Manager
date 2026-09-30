/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.auftrag.versand;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * HTTP-Antwort von PTM-Online auf einen Schreibauftrag, unabhängig vom Statuscode.
 *
 * @param wiederholt {@code true}, wenn PTM-Online die gespeicherte Antwort eines bereits ausgeführten Auftrags
 *                   geliefert hat (Header {@code X-PTM-Replayed})
 */
public record SyncAntwort(int status, String body, boolean wiederholt) {

    private static final int ERFOLG_MIN = 200;
    private static final int ERFOLG_MAX = 299;

    public SyncAntwort {
        body = body == null ? "" : body;
    }

    public boolean erfolgreich() {
        return status >= ERFOLG_MIN && status <= ERFOLG_MAX;
    }

    /** Antwort als JSON-Objekt; leer, wenn sie keines ist. */
    public Optional<JsonObject> json() {
        try {
            JsonElement element = JsonParser.parseString(body);
            return element.isJsonObject() ? Optional.of(element.getAsJsonObject()) : Optional.empty();
        } catch (JsonParseException e) {
            return Optional.empty();
        }
    }

    /** Fehlercode {@code details.code} einer Ablehnung. */
    public Optional<String> code() {
        return details().map(details -> details.get("code")).filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString);
    }

    /** Ob die Ablehnung ein Feld nennt ({@code details.field}, z.&nbsp;B. „bereits angemeldet“). */
    public boolean nenntFeld() {
        return details().map(details -> details.has("field")).orElse(false);
    }

    /** Fehlertext {@code error} einer Ablehnung. */
    public Optional<String> fehlertext() {
        return json().map(json -> json.get("error")).filter(JsonElement::isJsonPrimitive)
                .map(JsonElement::getAsString);
    }

    private Optional<JsonObject> details() {
        return json().map(json -> json.get("details")).filter(JsonElement::isJsonObject)
                .map(JsonElement::getAsJsonObject);
    }
}
