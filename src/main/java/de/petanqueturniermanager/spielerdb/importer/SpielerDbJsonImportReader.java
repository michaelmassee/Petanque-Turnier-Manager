package de.petanqueturniermanager.spielerdb.importer;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

import org.jspecify.annotations.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import de.petanqueturniermanager.spielerdb.SpielerDbException;
import de.petanqueturniermanager.spielerdb.export.ExportEntity;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpieler;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohSpielerLabel;
import de.petanqueturniermanager.spielerdb.importer.ImportRohdaten.RohVerein;

/**
 * Liest eine JSON-Datei mit Top-Level-Struktur
 * {@code {meta, spieler, vereine, labels, spielerLabels}}. Symmetrisch zu
 * {@code SpielerDbJsonExporter}. Fehlt eine Sektion für eine Scope-Entity,
 * schlägt der Reader hart fehl.
 */
public final class SpielerDbJsonImportReader implements SpielerDbImportReader {

    @Override
    public ImportRohdaten read(ImportRequest request) throws SpielerDbException {
        Path datei = request.source();
        if (!Files.isRegularFile(datei)) {
            throw new SpielerDbException("JSON-Datei fehlt: " + datei);
        }
        EnumSet<ExportEntity> entities = request.entities();

        JsonObject root = leseRoot(datei);

        List<RohSpieler> spieler = entities.contains(ExportEntity.SPIELER)
                ? leseSpieler(datei, holeArrayPflicht(datei, root, "spieler"))
                : List.of();
        List<RohVerein> vereine = entities.contains(ExportEntity.VEREINE)
                ? leseVereine(datei, holeArrayPflicht(datei, root, "vereine"))
                : List.of();
        List<RohLabel> labels = entities.contains(ExportEntity.LABELS)
                ? leseLabels(datei, holeArrayPflicht(datei, root, "labels"))
                : List.of();
        List<RohSpielerLabel> junction =
                entities.contains(ExportEntity.SPIELER) && entities.contains(ExportEntity.LABELS)
                        ? leseJunction(datei, holeArrayPflicht(datei, root, "spielerLabels"))
                        : List.of();

        return new ImportRohdaten(spieler, vereine, labels, junction);
    }

    private static JsonObject leseRoot(Path datei) throws SpielerDbException {
        try (Reader r = Files.newBufferedReader(datei, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(r);
            if (!root.isJsonObject()) {
                throw new SpielerDbException("JSON-Wurzel in " + datei + " ist kein Objekt");
            }
            return root.getAsJsonObject();
        } catch (IOException | JsonSyntaxException e) {
            throw new SpielerDbException("JSON-Lesen fehlgeschlagen: " + datei, e);
        }
    }

    private static JsonArray holeArrayPflicht(Path datei, JsonObject root, String key)
            throws SpielerDbException {
        JsonElement el = root.get(key);
        if (el == null || el.isJsonNull()) {
            throw new SpielerDbException("JSON-Datei " + datei + " enthält keine Sektion '" + key + "'");
        }
        if (!el.isJsonArray()) {
            throw new SpielerDbException("JSON-Sektion '" + key + "' in " + datei + " ist kein Array");
        }
        return el.getAsJsonArray();
    }

    private static List<RohSpieler> leseSpieler(Path datei, JsonArray arr)
            throws SpielerDbException {
        List<RohSpieler> erg = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = pflichtObjekt(datei, arr, i, "spieler");
            erg.add(new RohSpieler(
                    nullableInt(o, "nr"),
                    pflichtString(datei, o, "vorname"),
                    pflichtString(datei, o, "nachname"),
                    nullableInt(o, "vereinNr"),
                    null,
                    nullableString(o, "lizenznr")));
        }
        return erg;
    }

    private static List<RohVerein> leseVereine(Path datei, JsonArray arr)
            throws SpielerDbException {
        List<RohVerein> erg = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = pflichtObjekt(datei, arr, i, "vereine");
            erg.add(new RohVerein(nullableInt(o, "nr"), pflichtString(datei, o, "name")));
        }
        return erg;
    }

    private static List<RohLabel> leseLabels(Path datei, JsonArray arr)
            throws SpielerDbException {
        List<RohLabel> erg = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = pflichtObjekt(datei, arr, i, "labels");
            erg.add(new RohLabel(nullableInt(o, "nr"), pflichtString(datei, o, "name")));
        }
        return erg;
    }

    private static List<RohSpielerLabel> leseJunction(Path datei, JsonArray arr)
            throws SpielerDbException {
        List<RohSpielerLabel> erg = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            JsonObject o = pflichtObjekt(datei, arr, i, "spielerLabels");
            Integer sNr = nullableInt(o, "spielerNr");
            Integer lNr = nullableInt(o, "labelNr");
            if (sNr == null || lNr == null) {
                throw new SpielerDbException("Junction-Eintrag " + i
                        + " in " + datei + " hat fehlende NR");
            }
            erg.add(new RohSpielerLabel(sNr, lNr));
        }
        return erg;
    }

    private static JsonObject pflichtObjekt(Path datei, JsonArray arr, int i, String section)
            throws SpielerDbException {
        JsonElement el = arr.get(i);
        if (!el.isJsonObject()) {
            throw new SpielerDbException("JSON-Eintrag " + i + " in Sektion '" + section
                    + "' (" + datei + ") ist kein Objekt");
        }
        return el.getAsJsonObject();
    }

    private static String pflichtString(Path datei, JsonObject o, String key)
            throws SpielerDbException {
        JsonElement el = o.get(key);
        if (el == null || el.isJsonNull()) {
            throw new SpielerDbException("JSON-Feld '" + key + "' fehlt in " + datei);
        }
        return el.getAsString();
    }

    @Nullable
    private static String nullableString(JsonObject o, String key) {
        JsonElement el = o.get(key);
        if (el == null || el.isJsonNull()) {
            return null;
        }
        String s = el.getAsString();
        return s.isEmpty() ? null : s;
    }

    @Nullable
    private static Integer nullableInt(JsonObject o, String key) {
        JsonElement el = o.get(key);
        if (el == null || el == JsonNull.INSTANCE || el.isJsonNull()) {
            return null;
        }
        try {
            return el.getAsInt();
        } catch (NumberFormatException | UnsupportedOperationException e) {
            return null;
        }
    }
}
