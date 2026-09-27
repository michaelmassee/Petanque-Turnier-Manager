/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheetKonstanten;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SupermeleeTurnierTestDaten;

/**
 * Live-Übertragung eines Supermelee-Spieltags: jeder Spieler ist online eine eigene Meldung, eine Partie
 * überträgt die Spieler-Ids beider Teams. Runden und Ergebnisse entsprechen den Spielrunden des Spieltags, die
 * Rangliste der Spieltag-Rangliste.
 */
class PtmOnlineLiveSyncSupermeleeUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final int SPIELER_PRO_TEAM = 3;

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
    void spieltagMitRundenErgebnissenUndRanglisteWirdUebertragen() throws Exception {
        PtmOnlineLiveSync.uebertragen(alleSpielerVerbinden(),
                LiveStandQuellen.fuer(wkingSpreadsheet, TurnierSystem.SUPERMELEE, spieltag).orElseThrow());

        List<Integer> rundenNummern = new ArrayList<>();
        for (int nr = 1; spielrunde(nr) != null; nr++) {
            rundenNummern.add(nr);
        }
        assertThat(rundenNummern).isNotEmpty();
        assertThat(server.runden()).containsOnlyKeys(rundenNummern);
        for (int nr : rundenNummern) {
            assertThat(partien(server.runden().get(nr))).as("Runde %d wie im Blatt", nr).isEqualTo(partienImBlatt(nr));
        }
        assertThat(server.rangliste()).isNotEmpty();
        assertThat(ranglisteOnline()).isEqualTo(ranglisteImBlatt());
    }

    /** Verbindet den Spieltag und ordnet jedem Spieler die Online-Id {@code online-<Spieler-Nr>} zu. */
    private PtmOnlineVerbindung alleSpielerVerbinden() throws Exception {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
                TurnierSystem.SUPERMELEE, spieltag);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Supermelee-Livetest";
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

    private com.sun.star.sheet.XSpreadsheet spielrunde(int nr) {
        return SheetMetadataHelper.findeSheetUndHeile(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                SheetMetadataHelper.schluesselSupermeleeSpielrunde(spieltag, nr),
                SheetNamen.supermeleeSpielrunde(spieltag, nr));
    }

    /** Je Partie: Spieler-Ids Team A, Spieler-Ids Team B und Ergebnis. */
    private List<List<Object>> partienImBlatt(int nr) {
        int erste = SpielrundeSheetKonstanten.ERSTE_DATEN_ZEILE;
        RangeData daten = RangeHelper.from(spielrunde(nr), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(0, erste, SpielrundeSheetKonstanten.LETZTE_SPALTE, erste + 200)).getDataFromRange();
        List<List<Object>> partien = new ArrayList<>();
        for (RowData zeile : daten) {
            List<String> teamA = spielerIds(zeile, SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE);
            if (teamA.isEmpty()) {
                break;
            }
            int ergebnis = SpielrundeSheetKonstanten.ERSTE_SPALTE_ERGEBNISSE;
            partien.add(List.of(teamA, spielerIds(zeile, SpielrundeSheetKonstanten.ERSTE_SPIELERNR_SPALTE + 3),
                    zeile.get(ergebnis).getIntVal(-1) + ":" + zeile.get(ergebnis + 1).getIntVal(-1)));
        }
        return partien;
    }

    private static List<String> spielerIds(RowData zeile, int ersteSpalte) {
        List<String> ids = new ArrayList<>();
        for (int spalte = ersteSpalte; spalte < ersteSpalte + SPIELER_PRO_TEAM; spalte++) {
            int nr = zeile.get(spalte).getIntVal(0);
            if (nr > 0) {
                ids.add(onlineId(nr));
            }
        }
        return ids;
    }

    private static List<List<Object>> partien(JsonArray matches) {
        List<List<Object>> partien = new ArrayList<>();
        for (JsonElement element : matches) {
            var match = element.getAsJsonObject();
            String ergebnis = (match.has("scoreA") ? match.get("scoreA").getAsInt() : -1) + ":"
                    + (match.has("scoreB") ? match.get("scoreB").getAsInt() : -1);
            partien.add(List.of(ids(match.getAsJsonArray("teamA")), ids(match.getAsJsonArray("teamB")), ergebnis));
        }
        return partien;
    }

    /** Platz, Online-Id und Siege je Zeile der übertragenen Rangliste. */
    private List<List<String>> ranglisteOnline() {
        List<List<String>> zeilen = new ArrayList<>();
        for (JsonElement element : server.rangliste()) {
            var eintrag = element.getAsJsonObject();
            zeilen.add(List.of(eintrag.get("place").getAsString(),
                    eintrag.getAsJsonArray("registrationIds").get(0).getAsString(), eintrag.get("wins").getAsString()));
        }
        return zeilen;
    }

    /** Platz, Online-Id und Siege je Zeile der Spieltag-Rangliste; Siege stehen in der ersten Summenspalte. */
    private List<List<String>> ranglisteImBlatt() throws Exception {
        SpieltagRanglisteSheet rangliste = new SpieltagRanglisteSheet(wkingSpreadsheet);
        rangliste.setSpieltagNr(new SuperMeleeKonfigurationSheet(wkingSpreadsheet).getAktiveSpieltag());
        int siegeSpalte = rangliste.getErsteSummeSpalte();
        int erste = SpieltagRanglisteSheet.ERSTE_DATEN_ZEILE;
        RangeData daten = RangeHelper.from(rangliste.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(0, erste, siegeSpalte, erste + 500)).getDataFromRange();
        List<List<String>> zeilen = new ArrayList<>();
        for (RowData zeile : daten) {
            int nr = zeile.get(SpieltagRanglisteSheet.SPIELER_NR_SPALTE).getIntVal(0);
            if (nr <= 0) {
                break;
            }
            zeilen.add(List.of(String.valueOf(zeile.get(SpieltagRanglisteSheet.RANGLISTE_SPALTE).getIntVal(0)),
                    onlineId(nr), String.valueOf(zeile.get(siegeSpalte).getIntVal(0))));
        }
        return zeilen;
    }

    private static List<String> ids(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsString).toList();
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
