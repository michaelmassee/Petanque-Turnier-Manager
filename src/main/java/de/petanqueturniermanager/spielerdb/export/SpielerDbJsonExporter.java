package de.petanqueturniermanager.spielerdb.export;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumSet;

import org.jspecify.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.spielerdb.LabelDatensatz;
import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;
import de.petanqueturniermanager.spielerdb.VereinDatensatz;

/**
 * Schreibt einen JSON-Export mit Top-Level-Struktur
 * {@code {meta, spieler, vereine, labels, spielerLabels}}. Nur die im Scope
 * gewählten Arrays werden geschrieben (leere Arrays für nicht gewählte
 * Entities würden Re-Importer in die Irre führen).
 *
 * <p>Feldnamen werden bewusst explizit gesetzt (kein automatisches Mapping
 * aus Record-Komponentennamen), damit ein späterer Refactor der Java-Records
 * das JSON-Schema nicht versehentlich bricht.
 */
public final class SpielerDbJsonExporter implements SpielerDbExporter {

    @Override
    public void export(SpielerDbExportData data, ExportRequest request) throws SpielerDbException {
        EnumSet<ExportEntity> entities = request.entities();
        JsonObject root = new JsonObject();
        root.add("meta", baueMeta(data.meta()));

        if (entities.contains(ExportEntity.SPIELER)) {
            root.add("spieler", baueSpielerArray(data));
        }
        if (entities.contains(ExportEntity.VEREINE)) {
            root.add("vereine", baueVereineArray(data));
        }
        if (entities.contains(ExportEntity.LABELS)) {
            root.add("labels", baueLabelsArray(data));
        }
        if (entities.contains(ExportEntity.SPIELER) && entities.contains(ExportEntity.LABELS)) {
            root.add("spielerLabels", baueJunctionArray(data));
        }

        Gson gson = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
        Path datei = request.target();
        try {
            Path eltern = datei.toAbsolutePath().getParent();
            if (eltern != null) {
                Files.createDirectories(eltern);
            }
            try (BufferedWriter w = Files.newBufferedWriter(datei, StandardCharsets.UTF_8)) {
                gson.toJson(root, w);
            }
        } catch (IOException e) {
            throw new SpielerDbException("JSON-Export fehlgeschlagen: " + datei, e);
        }
    }

    private static JsonObject baueMeta(ExportMeta meta) {
        JsonObject o = new JsonObject();
        o.addProperty("version", meta.version());
        o.addProperty("exportedAt", meta.exportedAt().toString());
        if (meta.appVersion() != null) {
            o.addProperty("appVersion", meta.appVersion());
        }
        return o;
    }

    private static JsonArray baueSpielerArray(SpielerDbExportData data) {
        JsonArray arr = new JsonArray();
        for (SpielerMitVerein s : data.spieler()) {
            JsonObject o = new JsonObject();
            o.addProperty("nr", s.nr());
            o.addProperty("vorname", s.vorname());
            o.addProperty("nachname", s.nachname());
            addNullableInt(o, "vereinNr", s.vereinNr());
            addNullableString(o, "lizenznr", s.lizenznr());
            arr.add(o);
        }
        return arr;
    }

    private static JsonArray baueVereineArray(SpielerDbExportData data) {
        JsonArray arr = new JsonArray();
        for (VereinDatensatz v : data.vereine()) {
            JsonObject o = new JsonObject();
            addNullableInt(o, "nr", v.nr());
            o.addProperty("name", v.name());
            arr.add(o);
        }
        return arr;
    }

    private static JsonArray baueLabelsArray(SpielerDbExportData data) {
        JsonArray arr = new JsonArray();
        for (LabelDatensatz l : data.labels()) {
            JsonObject o = new JsonObject();
            addNullableInt(o, "nr", l.nr());
            o.addProperty("name", l.name());
            arr.add(o);
        }
        return arr;
    }

    private static JsonArray baueJunctionArray(SpielerDbExportData data) {
        JsonArray arr = new JsonArray();
        for (SpielerLabelZuordnung z : data.spielerLabels()) {
            JsonObject o = new JsonObject();
            o.addProperty("spielerNr", z.spielerNr());
            o.addProperty("labelNr", z.labelNr());
            arr.add(o);
        }
        return arr;
    }

    private static void addNullableInt(JsonObject o, String key, @Nullable Integer wert) {
        if (wert == null) {
            o.add(key, com.google.gson.JsonNull.INSTANCE);
        } else {
            o.addProperty(key, wert);
        }
    }

    private static void addNullableString(JsonObject o, String key, @Nullable String wert) {
        if (wert == null) {
            o.add(key, com.google.gson.JsonNull.INSTANCE);
        } else {
            o.addProperty(key, wert);
        }
    }
}
