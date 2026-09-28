/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_Update;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Check-in am Supermelee-Spieltag: maßgeblich ist die Aktiv-Spalte des verbundenen Spieltags, nicht die des ersten.
 * Ohne bekannten Stand gehen nur eingecheckte Spieler an PTM-Online.
 */
class PtmOnlineCheckinSupermeleeUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final int ANZ_SPIELER = 3;

    private PtmOnlineTestServer server;
    private int spieltag;

    @BeforeEach
    void vorbereiten() throws Exception {
        RandomSource.setSeed(42L);
        MessageBox.setDialogeUeberspringen(true);
        server = new PtmOnlineTestServer(TURNIER_ID, "{\"registrations\":[]}");
        new SupermeleeTurnierTestDaten(wkingSpreadsheet).generate();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SUPERMELEE.getId());
        spieltag = new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag().getNr();
    }

    @AfterEach
    void aufraeumen() {
        server.close();
        MessageBox.setDialogeUeberspringen(false);
        RandomSource.reset();
    }

    @Test
    void checkinWirdAusDerSpalteDesVerbundenenSpieltagsGelesenUndNurAktiveGehenHinaus() throws Exception {
        assertThat(spieltag).as("Testdaten mit mehreren Spieltagen").isGreaterThan(1);
        PtmOnlineVerbindung verbindung = alleSpielerVerbinden();
        List<Integer> spielerNr = ersteSpielerNummern();
        // Spieltag 1 bewusst gegenläufig, damit eine falsch gewählte Spalte auffällt.
        schreibeAktivSpalte(1, List.of("", "1", "1"));
        schreibeAktivSpalte(spieltag, List.of("1", "2", ""));

        List<PtmOnlineStatusAuftrag.Eintrag> stand = PtmOnlineCheckin.leseStand(verbindung);

        Map<String, OnlineTeilnahme> teilnahme = teilnahmeProOnlineId(verbindung, stand);
        assertThat(teilnahme).containsEntry(onlineId(spielerNr.get(0)), OnlineTeilnahme.AKTIV)
                .containsEntry(onlineId(spielerNr.get(1)), OnlineTeilnahme.AUSGESETZT)
                .containsEntry(onlineId(spielerNr.get(2)), OnlineTeilnahme.INAKTIV);

        PtmOnlineCheckin.Aenderung aenderung = new PtmOnlineCheckin().ermittle(TURNIER_ID, stand).orElseThrow();
        PtmOnlineStatusAbgleich.senden(aenderung.auftrag(), verbindung.mapping(), verbindung.gebundenerClient());

        List<JsonObject> gepusht = server.gepushteErgebnisse();
        assertThat(gepusht).isNotEmpty().allSatisfy(
                eintrag -> assertThat(eintrag.get("participation").getAsString()).isEqualTo("active"));
        assertThat(gepusht).extracting(eintrag -> eintrag.get("id").getAsString())
                .contains(onlineId(spielerNr.get(0)))
                .doesNotContain(onlineId(spielerNr.get(1)), onlineId(spielerNr.get(2)));
    }

    private Map<String, OnlineTeilnahme> teilnahmeProOnlineId(PtmOnlineVerbindung verbindung,
            List<PtmOnlineStatusAuftrag.Eintrag> stand) throws Exception {
        Map<String, String> onlineIds = verbindung.mapping().getOnlineIdsProUuid();
        Map<String, OnlineTeilnahme> ergebnis = new LinkedHashMap<>();
        stand.forEach(eintrag -> ergebnis.put(onlineIds.get(eintrag.lokaleUuid()), eintrag.teilnahme()));
        return ergebnis;
    }

    private XSpreadsheet meldeliste() throws Exception {
        return new MeldeListeSheet_Update(wkingSpreadsheet).getXSpreadSheet();
    }

    private List<Integer> ersteSpielerNummern() throws Exception {
        int erste = MeldeListeKonstanten.ERSTE_DATEN_ZEILE;
        RangeData daten = RangeHelper.from(meldeliste(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(MeldeListeKonstanten.SPIELER_NR_SPALTE, erste, MeldeListeKonstanten.SPIELER_NR_SPALTE,
                        erste + ANZ_SPIELER - 1)).getDataFromRange();
        List<Integer> nummern = new ArrayList<>();
        for (RowData zeile : daten) {
            nummern.add(zeile.get(0).getIntVal(0));
        }
        assertThat(nummern).allSatisfy(nr -> assertThat(nr).isPositive());
        return nummern;
    }

    /** Schreibt die Aktiv-Werte der ersten Spieler als Block in die Spalte des Spieltags. */
    private void schreibeAktivSpalte(int spieltagNr, List<String> werte) throws Exception {
        int spalte = new MeldeListeSheet_Update(wkingSpreadsheet).spieltagSpalte(SpielTagNr.from(spieltagNr));
        RangeData daten = new RangeData();
        werte.forEach(wert -> {
            RowData zeile = daten.addNewRow();
            if (wert.isEmpty()) {
                zeile.newEmpty();
            } else {
                zeile.newInt(Integer.parseInt(wert));
            }
        });
        RangeHelper.from(meldeliste(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                daten.getRangePosition(Position.from(spalte, MeldeListeKonstanten.ERSTE_DATEN_ZEILE)))
                .setDataInRange(daten);
    }

    /** Verbindet den Spieltag und ordnet jedem Spieler die Online-Id {@code online-<Spieler-Nr>} zu. */
    private PtmOnlineVerbindung alleSpielerVerbinden() throws Exception {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
                TurnierSystem.SUPERMELEE, spieltag);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Supermelee-Check-in";
        turnier.type = "supermelee";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);

        MeldelisteZiel ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        Map<Integer, Integer> zeileProSpieler = PtmOnlineSpielrundeSync.zeileProTeam(ziel);
        Map<Integer, String> uuidProZeile = PtmOnlineSpielrundeSync.lokaleUuids(ziel, zeileProSpieler.values());
        List<NeueZuordnung> zuordnungen = new ArrayList<>();
        for (Map.Entry<Integer, Integer> spieler : zeileProSpieler.entrySet()) {
            String uuid = uuidProZeile.get(spieler.getValue());
            zuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(uuid, ziel.formelTeamNrAusLokalerUuid(uuid),
                    "Spieler " + spieler.getKey(), registration(onlineId(spieler.getKey()))));
        }
        mapping.addMappings(zuordnungen);
        return new PtmOnlineVerbindung(server.zugangsdaten(), ziel, ziel, mapping, TURNIER_ID, spieltag);
    }

    private static String onlineId(int spielerNr) {
        return "online-" + spielerNr;
    }

    private static RegistrationDto registration(String id) {
        Map<String, Object> json = new HashMap<>();
        json.put("id", id);
        json.put("firstName", id);
        json.put("status", "confirmed");
        Gson gson = new Gson();
        return gson.fromJson(gson.toJson(json), RegistrationDto.class);
    }
}
