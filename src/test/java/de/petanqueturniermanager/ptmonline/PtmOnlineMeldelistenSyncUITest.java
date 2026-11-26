/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.LibreOfficePtmOnlineSpeicher;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.ptmonline.auftrag.SyncAuftrag;
import de.petanqueturniermanager.ptmonline.auftrag.versand.SyncAntwort;
import de.petanqueturniermanager.ptmonline.auftrag.versand.VersandErgebnis;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/** Echte Calc-Zeilen und Auftragsergebnisse; die HTTP-Antworten kommen ohne Netzwerk direkt in den Bestand. */
class PtmOnlineMeldelistenSyncUITest extends BaseCalcUITest {

    private final PtmOnlineMeldelistenSync sync = new PtmOnlineMeldelistenSync();
    private final AuftragsBestand bestand = AuftragsBestand.leer();
    private final Map<String, Integer> zeilen = new LinkedHashMap<>();
    private final Map<String, String> uuids = new LinkedHashMap<>();
    private MeldelisteZiel ziel;
    private PtmOnlineRegistrationMapping mapping;
    private PtmOnlineVerbindung verbindung;

    @BeforeEach
    void vorbereiten() throws Exception {
        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.TETE, false, false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        for (String name : List.of("Anna", "Ben", "Clara")) {
            ziel.schreibeBlock(List.of(new SpielerMitVerein(0, name, "Test", null, null, List.of(), List.of(), null)));
        }
        new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).upDateSheet();
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        mapping.sicherstellen();
        for (var spieler : ziel.leseAlleSpielerRoh()) {
            String uuid = ziel.getOderErzeugeLokaleUuid(spieler.zeile1Basiert());
            zeilen.put(spieler.vorname(), spieler.zeile1Basiert());
            uuids.put(spieler.vorname(), uuid);
            mapping.addMapping(uuid, "online-" + spieler.vorname(), 1,
                    spieler.vorname(), "confirmed");
        }
        verbindung = new PtmOnlineVerbindung(new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test",
                "http://127.0.0.1", true), ziel, ziel, mapping, "t1", null);
        assertThat(sync.erfasse(bestand, verbindung)).isFalse();
        assertThat(bestand.zuSenden(false)).isEmpty();
    }

    @Test
    void loeschungDirektNachAbgleichBrauchtKeinenVorherigenHintergrundlauf() throws Exception {
        var frisch = new PtmOnlineMeldelistenSync();
        frisch.uebernehmen(new PtmOnlineStatusAuftrag("t1", PtmOnlineCheckin.leseStand(verbindung)),
                mapping.getOnlineIdsProUuid());
        ziel.entferneMeldung(zeilen.get("Ben"));

        assertThat(frisch.erfasse(bestand, verbindung)).isTrue();
        assertThat(frisch.erfasse(bestand, verbindung)).isFalse();

        assertStorno("Ben", 1);
    }

    /**
     * Gelöscht, gespeichert und geschlossen vor dem nächsten Lauf: Nach dem Öffnen gibt es weder den flüchtigen
     * Zustand noch die Nr-Zelle der Zeile. Die Online-ID kommt aus dem im Dokument gespeicherten Stand.
     */
    @Test
    void loeschungWirdNachErneutemOeffnenErkannt() throws Exception {
        ziel.entferneMeldung(zeilen.get("Ben"));
        var nachOeffnen = new PtmOnlineMeldelistenSync();
        var neuesMapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        var neueVerbindung = new PtmOnlineVerbindung(new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test",
                "http://127.0.0.1", true), ziel, ziel, neuesMapping, "t1", null);

        assertThat(nachOeffnen.erfasse(bestand, neueVerbindung)).isTrue();
        assertThat(nachOeffnen.erfasse(bestand, neueVerbindung)).isFalse();

        assertStorno("Ben", 1);
    }

    /**
     * Manuell importiert, gleich wieder gelöscht, gespeichert und geschlossen – ohne Hintergrundlauf dazwischen: Die
     * Zuordnung des Abgleichs muss schon im Dokument stehen.
     */
    @Test
    void loeschungDirektNachManuellemImportWirdNachErneutemOeffnenErkannt() throws Exception {
        ziel.schreibeBlock(List.of(new SpielerMitVerein(0, "Dora", "Test", null, null, List.of(), List.of(), null)));
        new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet).upDateSheet();
        int doraZeile = ziel.leseAlleSpielerRoh().stream().filter(spieler -> "Dora".equals(spieler.vorname()))
                .findFirst().orElseThrow().zeile1Basiert();
        mapping.addMapping(ziel.getOderErzeugeLokaleUuid(doraZeile), "online-Dora", 1, "Dora", "confirmed");
        PtmOnlineMeldelistenSync.merkeImDokument(mapping,
                new PtmOnlineStatusAuftrag("t1", PtmOnlineCheckin.leseStand(verbindung)), mapping.getOnlineIdsProUuid());
        ziel.entferneMeldung(doraZeile);

        var nachOeffnen = new PtmOnlineMeldelistenSync();
        var neueVerbindung = new PtmOnlineVerbindung(new LibreOfficePtmOnlineSpeicher.Zugangsdaten("test",
                "http://127.0.0.1", true), ziel, ziel,
                new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null), "t1", null);
        assertThat(nachOeffnen.erfasse(bestand, neueVerbindung)).isTrue();
        assertThat(nachOeffnen.erfasse(bestand, neueVerbindung)).isFalse();

        assertStorno("Dora", 1);
    }

    @Test
    void leereZwischenzeileStorniertNurDieTatsaechlichGeloeschteMeldung() throws Exception {
        ziel.entferneMeldung(zeilen.get("Ben"));

        assertThat(sync.erfasse(bestand, verbindung)).isTrue();
        assertThat(sync.erfasse(bestand, verbindung)).isFalse();

        assertStorno("Ben", 1);
        assertThat(ziel.leseAlleSpielerRoh()).extracting(spieler -> spieler.vorname()).containsExactly("Anna", "Clara");
    }

    @Test
    void loeschungWaehrendOffenerNamenskorrekturWirdNachDerenAnnahmeStorniert() throws Exception {
        JsonObject kontext = new JsonObject();
        kontext.addProperty("lokaleUuid", uuids.get("Ben"));
        var aenderung = bestand.erzeuge(AuftragsArt.ANMELDUNG_AENDERN, "PUT", "/test", "{}", kontext.toString());
        ziel.entferneMeldung(zeilen.get("Ben"));

        for (int i = 0; i < 3; i++) {
            assertThat(sync.erfasse(bestand, verbindung)).isFalse();
            assertThat(bestand.zuSenden(false)).containsExactly(aenderung);
        }
        JsonObject antwort = new JsonObject();
        antwort.add("registration", registration("Ben", 2));
        anwenden(aenderung, new SyncAntwort(200, antwort.toString(), false));

        sync.erfasse(bestand, verbindung);

        assertStorno("Ben", 2);
    }

    @Test
    void revisionskonfliktWiederholtStornoBisZurBestaetigungOhneDoppelauftrag() throws Exception {
        ziel.entferneMeldung(zeilen.get("Ben"));
        sync.erfasse(bestand, verbindung);
        sync.erfasse(bestand, verbindung);
        SyncAuftrag erster = assertStorno("Ben", 1);
        for (int i = 0; i < 3; i++) {
            assertThat(sync.erfasse(bestand, verbindung)).isFalse();
            assertThat(bestand.zuSenden(false)).containsExactly(erster);
        }

        JsonObject details = new JsonObject();
        details.addProperty("code", "execution_conflict");
        details.add("registration", registration("Ben", 5));
        JsonObject antwort = new JsonObject();
        antwort.add("details", details);
        anwenden(erster, new SyncAntwort(409, antwort.toString(), false));
        assertThat(mapping.getExecutionRevisionenProUuid()).containsEntry(uuids.get("Ben"), 5);

        sync.erfasse(bestand, verbindung);

        SyncAuftrag wiederholt = assertStorno("Ben", 5);
        assertThat(wiederholt.auftragsId()).isNotEqualTo(erster.auftragsId());
        assertThat(wiederholt.zaehler()).isGreaterThan(erster.zaehler());
        anwenden(wiederholt, new SyncAntwort(200, "{}", false));
        for (int i = 0; i < 3; i++) {
            assertThat(sync.erfasse(bestand, verbindung)).isFalse();
            assertThat(bestand.zuSenden(false)).isEmpty();
        }
        assertThat(ZuordnungsVermerke.lese(mapping.getZusaetzeProUuid().get(uuids.get("Ben")).vermerk())
                .hat(ZuordnungsVermerke.LOKAL_ENTFERNT)).isTrue();
    }

    @Test
    void fachlichAbgelehnterStornoWartetAufKlaerungStattEndlosNeuZuSenden() throws Exception {
        ziel.entferneMeldung(zeilen.get("Ben"));
        sync.erfasse(bestand, verbindung);
        sync.erfasse(bestand, verbindung);
        anwenden(assertStorno("Ben", 1), new SyncAntwort(409,
                "{\"details\":{\"code\":\"registration_drawn\"}}", false));

        for (int i = 0; i < 3; i++) {
            assertThat(sync.erfasse(bestand, verbindung)).isFalse();
            assertThat(bestand.zuSenden(false)).isEmpty();
        }
        assertThat(PtmOnlineAuftraege.abgelehnteFaelle(bestand)).hasSize(1);
    }

    private SyncAuftrag assertStorno(String name, int revision) {
        assertThat(bestand.zuSenden(false)).singleElement().satisfies(auftrag -> {
            assertThat(auftrag.art()).isEqualTo(AuftragsArt.ANMELDUNG_STORNIEREN);
            JsonObject body = JsonParser.parseString(auftrag.body()).getAsJsonObject();
            assertThat(body.get("onlineRegistrationId").getAsString()).isEqualTo("online-" + name);
            assertThat(body.get("expectedExecutionRevision").getAsInt()).isEqualTo(revision);
        });
        return bestand.zuSenden(false).getFirst();
    }

    private void anwenden(SyncAuftrag auftrag, SyncAntwort antwort) throws Exception {
        bestand.gesendet(new VersandErgebnis(auftrag, antwort, antwort.erfolgreich()));
        PtmOnlineAuftraege.anwenden(bestand, mapping);
    }

    private static JsonObject registration(String name, int revision) {
        JsonObject registration = new JsonObject();
        registration.addProperty("id", "online-" + name);
        registration.addProperty("status", "confirmed");
        registration.addProperty("executionRevision", revision);
        return registration;
    }
}
