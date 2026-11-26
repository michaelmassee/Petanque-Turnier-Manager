/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.onlinesync.speicher;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;

class PtmOnlineSyncJsonTest {

    private static final Instant ZEITPUNKT = Instant.parse("2026-11-24T18:30:15.123456Z");

    @Test
    void auftraegeBleibenBeimSpeichernUnveraendert() {
        // Body mit Leerzeichen, Zeilenumbruch, Anführungszeichen und Umlaut: Teil des Idempotenz-Hashs.
        String body = "{ \"name\" : \"Müller \\\"Hans\\\"\",\n  \"liste\": [1, 2] }";
        SyncAuftrag runde = new SyncAuftrag("id-1", 7, AuftragsArt.RUNDE, "PUT", "/api/t/runde/1", body,
                "{\"uuid\":\"u1\"}");
        SyncAuftrag start = new SyncAuftrag("id-2", 8, AuftragsArt.START, "POST", "/api/t/start", "", "");
        List<AuftragsBestand.Eintrag> eintraege = List.of(
                new AuftragsBestand.Eintrag(runde, AuftragsBestand.Zustand.OFFEN, "", ZEITPUNKT),
                new AuftragsBestand.Eintrag(start, AuftragsBestand.Zustand.ABGELEHNT, "Turnier läuft", ZEITPUNKT));

        List<AuftragsBestand.Eintrag> gelesen = PtmOnlineSyncJson
                .auftraegeAusJson(PtmOnlineSyncJson.auftraegeAlsJson(eintraege));

        assertThat(gelesen).isEqualTo(eintraege);
        assertThat(gelesen.getFirst().auftrag().body()).isEqualTo(body);
    }

    @Test
    void unlesbareAuftraegeWerdenUebersprungen() {
        String json = "[{\"id\":\"ok\",\"zaehler\":1,\"art\":\"RUNDE\",\"methode\":\"PUT\",\"pfad\":\"/p\","
                + "\"body\":\"{}\",\"kontext\":\"{}\",\"zustand\":\"OFFEN\",\"grund\":\"\","
                + "\"zeitpunkt\":\"2026-11-24T10:00:00Z\"},"
                + "{\"id\":\"falsche-art\",\"zaehler\":2,\"art\":\"GIBTS_NICHT\",\"methode\":\"PUT\",\"pfad\":\"/p\","
                + "\"zustand\":\"OFFEN\",\"zeitpunkt\":\"2026-11-24T10:00:00Z\"},"
                + "{\"id\":\"ohne-zaehler\",\"art\":\"RUNDE\",\"methode\":\"PUT\",\"pfad\":\"/p\","
                + "\"zustand\":\"OFFEN\",\"zeitpunkt\":\"2026-11-24T10:00:00Z\"},"
                + "{\"id\":\"falsche-zeit\",\"zaehler\":3,\"art\":\"RUNDE\",\"methode\":\"PUT\",\"pfad\":\"/p\","
                + "\"zustand\":\"OFFEN\",\"zeitpunkt\":\"gestern\"}, 42]";

        assertThat(PtmOnlineSyncJson.auftraegeAusJson(json)).extracting(eintrag -> eintrag.auftrag().auftragsId())
                .containsExactly("ok");
    }

    @Test
    void leererOderKaputterWertGiltAlsLeer() {
        assertThat(PtmOnlineSyncJson.auftraegeAusJson("")).isEmpty();
        assertThat(PtmOnlineSyncJson.auftraegeAusJson("{kein json")).isEmpty();
        assertThat(PtmOnlineSyncJson.zuordnungenAusJson("{\"kein\":\"array\"}").anzahl()).isZero();
    }

    @Test
    void zuordnungenBleibenBeimSpeichernUnveraendert() {
        PtmOnlineZuordnungsTabelle tabelle = new PtmOnlineZuordnungsTabelle(List.of(
                new PtmOnlineZuordnung("u1", "Hans Müller", "Bestätigt", "Startgeld: 5.00 EUR", "Essen? (Spieler 1)",
                        4, "confirmed", "{\"n\":[\"Hans\"]}", "LOKAL_ENTFERNT", "\"\""),
                new PtmOnlineZuordnung("u2", "Anna", "", "", "", 1, "cancelled", "", "", "")));

        PtmOnlineZuordnungsTabelle gelesen = PtmOnlineSyncJson
                .zuordnungenAusJson(PtmOnlineSyncJson.zuordnungenAlsJson(tabelle));

        assertThat(gelesen).isEqualTo(tabelle);
    }

    @Test
    void zuordnungOhneUuidWirdUebersprungenUndFehlendeRevisionIstEins() {
        String json = "[{\"bezeichnung\":\"ohne uuid\"},{\"uuid\":\"u1\",\"bezeichnung\":\"Hans\"}]";

        PtmOnlineZuordnungsTabelle gelesen = PtmOnlineSyncJson.zuordnungenAusJson(json);

        assertThat(gelesen.zuordnungen()).singleElement().satisfies(zuordnung -> {
            assertThat(zuordnung.lokaleUuid()).isEqualTo("u1");
            assertThat(zuordnung.revision()).isEqualTo(1);
            assertThat(zuordnung.teamname()).isEmpty();
        });
    }

    @Test
    void geseheneOnlineIdsBleibenMitReihenfolgeErhalten() {
        Map<String, String> onlineIds = new LinkedHashMap<>();
        onlineIds.put("u2", "online-2");
        onlineIds.put("u1", "online-1");

        assertThat(PtmOnlineSyncJson.onlineIdsAusJson(PtmOnlineSyncJson.onlineIdsAlsJson(onlineIds)))
                .containsExactly(Map.entry("u2", "online-2"), Map.entry("u1", "online-1"));
    }

    @Test
    void unvollstaendigeOnlineIdsWerdenUebersprungen() {
        String json = "[{\"uuid\":\"u1\",\"onlineId\":\"online-1\"},{\"uuid\":\"u2\"},{\"onlineId\":\"x\"}]";

        assertThat(PtmOnlineSyncJson.onlineIdsAusJson(json)).containsExactly(Map.entry("u1", "online-1"));
        assertThat(PtmOnlineSyncJson.onlineIdsAusJson("")).isEmpty();
    }
}
