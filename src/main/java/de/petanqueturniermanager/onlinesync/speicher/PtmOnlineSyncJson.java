/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

/**
 * JSON-Form des Auftragspuffers und der Zuordnungstabelle in den Dokument-Properties. Unlesbare Einträge (z.&nbsp;B.
 * von Hand verändert) werden beim Lesen übersprungen und protokolliert; ein unlesbarer Gesamtwert gilt als leer.
 * Body und Kontext eines Auftrags werden als Zeichenkette unverändert übernommen – der Body ist Teil des
 * Idempotenz-Hashs.
 */
final class PtmOnlineSyncJson {

    private static final Logger LOGGER = LogManager.getLogger(PtmOnlineSyncJson.class);

    private static final String ID = "id";
    private static final String ZAEHLER = "zaehler";
    private static final String ART = "art";
    private static final String METHODE = "methode";
    private static final String PFAD = "pfad";
    private static final String BODY = "body";
    private static final String KONTEXT = "kontext";
    private static final String ZUSTAND = "zustand";
    private static final String GRUND = "grund";
    private static final String ZEITPUNKT = "zeitpunkt";

    private static final String UUID = "uuid";
    private static final String BEZEICHNUNG = "bezeichnung";
    private static final String ONLINE_STATUS = "onlineStatus";
    private static final String TARIFE = "tarife";
    private static final String FRAGEN = "fragen";
    private static final String REVISION = "revision";
    private static final String ROH_STATUS = "rohStatus";
    private static final String BESETZUNG = "besetzung";
    private static final String VERMERK = "vermerk";
    private static final String TEAMNAME = "teamname";
    private static final String ONLINE_ID = "onlineId";

    private PtmOnlineSyncJson() {
    }

    static String auftraegeAlsJson(List<AuftragsBestand.Eintrag> eintraege) {
        JsonArray liste = new JsonArray();
        for (AuftragsBestand.Eintrag eintrag : eintraege) {
            SyncAuftrag auftrag = eintrag.auftrag();
            JsonObject objekt = new JsonObject();
            objekt.addProperty(ID, auftrag.auftragsId());
            objekt.addProperty(ZAEHLER, auftrag.zaehler());
            objekt.addProperty(ART, auftrag.art().name());
            objekt.addProperty(METHODE, auftrag.methode());
            objekt.addProperty(PFAD, auftrag.pfad());
            objekt.addProperty(BODY, auftrag.body());
            objekt.addProperty(KONTEXT, auftrag.kontext());
            objekt.addProperty(ZUSTAND, eintrag.zustand().name());
            objekt.addProperty(GRUND, eintrag.grund());
            objekt.addProperty(ZEITPUNKT, eintrag.zeitpunkt().toString());
            liste.add(objekt);
        }
        return liste.toString();
    }

    static List<AuftragsBestand.Eintrag> auftraegeAusJson(String json) {
        return lese(json, PtmOnlineSyncJson::auftrag);
    }

    static String zuordnungenAlsJson(PtmOnlineZuordnungsTabelle tabelle) {
        JsonArray liste = new JsonArray();
        for (PtmOnlineZuordnung zuordnung : tabelle.zuordnungen()) {
            JsonObject objekt = new JsonObject();
            objekt.addProperty(UUID, zuordnung.lokaleUuid());
            objekt.addProperty(BEZEICHNUNG, zuordnung.lokaleBezeichnung());
            objekt.addProperty(ONLINE_STATUS, zuordnung.onlineStatus());
            objekt.addProperty(TARIFE, zuordnung.tarife());
            objekt.addProperty(FRAGEN, zuordnung.fragen());
            objekt.addProperty(REVISION, zuordnung.revision());
            objekt.addProperty(ROH_STATUS, zuordnung.rohStatus());
            objekt.addProperty(BESETZUNG, zuordnung.besetzung());
            objekt.addProperty(VERMERK, zuordnung.vermerk());
            objekt.addProperty(TEAMNAME, zuordnung.teamname());
            liste.add(objekt);
        }
        return liste.toString();
    }

    static PtmOnlineZuordnungsTabelle zuordnungenAusJson(String json) {
        return new PtmOnlineZuordnungsTabelle(lese(json, PtmOnlineSyncJson::zuordnung));
    }

    /** Online-ID je lokaler UUID als Liste von Paaren; die Reihenfolge bleibt erhalten. */
    static String onlineIdsAlsJson(Map<String, String> onlineIdProUuid) {
        JsonArray liste = new JsonArray();
        onlineIdProUuid.forEach((uuid, onlineId) -> {
            JsonObject objekt = new JsonObject();
            objekt.addProperty(UUID, uuid);
            objekt.addProperty(ONLINE_ID, onlineId);
            liste.add(objekt);
        });
        return liste.toString();
    }

    static Map<String, String> onlineIdsAusJson(String json) {
        Map<String, String> ergebnis = new LinkedHashMap<>();
        lese(json, PtmOnlineSyncJson::onlineIdPaar).forEach(paar -> ergebnis.putIfAbsent(paar.getKey(), paar.getValue()));
        return ergebnis;
    }

    private static Map.Entry<String, String> onlineIdPaar(JsonObject objekt) {
        String uuid = text(objekt, UUID);
        String onlineId = text(objekt, ONLINE_ID);
        if (uuid.isBlank() || onlineId.isBlank()) {
            throw new IllegalArgumentException("Online-ID-Zuordnung ohne UUID oder Online-ID");
        }
        return Map.entry(uuid, onlineId);
    }

    private static AuftragsBestand.Eintrag auftrag(JsonObject objekt) {
        SyncAuftrag auftrag = new SyncAuftrag(text(objekt, ID), zahl(objekt, ZAEHLER, 0),
                AuftragsArt.valueOf(text(objekt, ART)), text(objekt, METHODE), text(objekt, PFAD), text(objekt, BODY),
                text(objekt, KONTEXT));
        return new AuftragsBestand.Eintrag(auftrag, AuftragsBestand.Zustand.valueOf(text(objekt, ZUSTAND)),
                text(objekt, GRUND), Instant.parse(text(objekt, ZEITPUNKT)));
    }

    private static PtmOnlineZuordnung zuordnung(JsonObject objekt) {
        String uuid = text(objekt, UUID);
        if (uuid.isBlank()) {
            throw new IllegalArgumentException("Zuordnung ohne lokale UUID");
        }
        return new PtmOnlineZuordnung(uuid, text(objekt, BEZEICHNUNG), text(objekt, ONLINE_STATUS),
                text(objekt, TARIFE), text(objekt, FRAGEN), (int) zahl(objekt, REVISION, 1),
                text(objekt, ROH_STATUS), text(objekt, BESETZUNG), text(objekt, VERMERK), text(objekt, TEAMNAME));
    }

    private static <T> List<T> lese(String json, Function<JsonObject, T> eintrag) {
        List<T> ergebnis = new ArrayList<>();
        if (json.isBlank()) {
            return ergebnis;
        }
        JsonArray liste;
        try {
            liste = JsonParser.parseString(json).getAsJsonArray();
        } catch (JsonParseException | IllegalStateException e) {
            LOGGER.warn("PTM-Online: unlesbarer Sync-Zustand in den Dokument-Properties, wird als leer behandelt", e);
            return ergebnis;
        }
        for (JsonElement element : liste) {
            try {
                ergebnis.add(eintrag.apply(element.getAsJsonObject()));
            } catch (IllegalArgumentException | IllegalStateException | UnsupportedOperationException
                    | DateTimeParseException e) {
                LOGGER.warn("PTM-Online: unlesbarer Eintrag im Sync-Zustand übersprungen: {}", element, e);
            }
        }
        return ergebnis;
    }

    private static long zahl(JsonObject objekt, String feld, long standard) {
        JsonElement wert = objekt.get(feld);
        return wert == null || wert.isJsonNull() ? standard : wert.getAsLong();
    }

    private static String text(JsonObject objekt, String feld) {
        JsonElement wert = objekt.get(feld);
        return wert == null || wert.isJsonNull() ? "" : wert.getAsString();
    }
}
