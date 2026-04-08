package de.petanqueturniermanager.webserver;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Gson-Adapter für das sealed interface {@link SplitKnoten}.
 * <p>
 * Serialisierungsformat:
 * <ul>
 *   <li>Blatt: {@code {"panel":0}}</li>
 *   <li>Teilung: {@code {"richtung":"H","groesse":50,"links":{...},"rechts":{...}}}</li>
 * </ul>
 */
public class SplitKnotenAdapter implements JsonSerializer<SplitKnoten>, JsonDeserializer<SplitKnoten> {

    @Override
    public JsonElement serialize(SplitKnoten knoten, Type typeOfSrc, JsonSerializationContext ctx) {
        var obj = new JsonObject();
        switch (knoten) {
            case SplitBlatt blatt -> obj.addProperty("panel", blatt.panel());
            case SplitTeilung teilung -> {
                obj.addProperty("richtung", teilung.richtung());
                obj.addProperty("groesse", teilung.groesse());
                obj.add("links", ctx.serialize(teilung.links(), SplitKnoten.class));
                obj.add("rechts", ctx.serialize(teilung.rechts(), SplitKnoten.class));
            }
        }
        return obj;
    }

    @Override
    public SplitKnoten deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx)
            throws JsonParseException {
        if (!json.isJsonObject()) {
            throw new JsonParseException("SplitKnoten muss ein JSON-Objekt sein");
        }
        JsonObject obj = json.getAsJsonObject();
        if (obj.has("panel")) {
            return new SplitBlatt(obj.get("panel").getAsInt());
        }
        if (!obj.has("richtung")) {
            throw new JsonParseException("SplitKnoten hat weder 'panel' noch 'richtung'");
        }
        var richtung = obj.get("richtung").getAsString();
        var groesse = obj.has("groesse") ? obj.get("groesse").getAsInt() : 50;
        SplitKnoten links = ctx.deserialize(obj.get("links"), SplitKnoten.class);
        SplitKnoten rechts = ctx.deserialize(obj.get("rechts"), SplitKnoten.class);
        return new SplitTeilung(richtung, groesse, links, rechts);
    }
}
