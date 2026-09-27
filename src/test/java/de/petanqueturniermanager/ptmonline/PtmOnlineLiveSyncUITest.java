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
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.onlinesync.sheet.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.ptmonline.live.LiveStandQuellen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerTurnierTestDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;

/**
 * Live-Übertragung eines Schweizer Turniers an einen lokalen PTM-Online-Ersatz: Paarungen, Freilos und Bahnen
 * kommen mit den Online-Ids der Teams an – auch im Anzeigemodus „Teamname“, in dem die Spielrunde keine
 * Nummern, sondern Namensformeln enthält. Ergebnisse und Rangliste entsprechen dem Turnierdokument.
 */
class PtmOnlineLiveSyncUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final int ANZ_TEAMS = 7;

    private PtmOnlineTestServer server;

    @BeforeEach
    void vorbereiten() throws Exception {
        RandomSource.setSeed(42L);
        MessageBox.setDialogeUeberspringen(true);
        server = new PtmOnlineTestServer(TURNIER_ID, "{\"registrations\":[]}");
        PtmOnlineLiveSync.vergessen(TURNIER_ID);
        new SchweizerMeldeListeSheetTestDaten(wkingSpreadsheet, ANZ_TEAMS).doRun();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
    }

    @AfterEach
    void aufraeumen() {
        server.close();
        MessageBox.setDialogeUeberspringen(false);
        RandomSource.reset();
    }

    @Test
    void rundeMitNummernKommtMitPaarungenFreilosUndBahnenAn() throws Exception {
        SchweizerSpielrundeSheetNaechste runde = ersteRunde(SpielplanTeamAnzeige.NR);

        uebertragen();

        JsonArray matches = server.runden().get(1);
        assertThat(server.runden()).containsOnlyKeys(1);
        assertThat(paare(matches)).isEqualTo(paareImBlatt(runde));
        assertThat(alleIds(matches)).as("jedes Team genau einmal eingeteilt")
                .containsExactlyInAnyOrderElementsOf(alleOnlineIds());
        assertThat(matches).filteredOn(match -> !istFreilos(match)).allSatisfy(match -> assertThat(
                match.getAsJsonObject().get("court").getAsString()).as("Bahn").matches("\\d+"));
        assertThat(server.geloeschteRunden()).as("nicht mehr vorhandene Folgerunde online gelöscht").containsExactly(2);
    }

    @Test
    void rundeMitTeamnamenWirdUeberDieNamensformelnZugeordnet() throws Exception {
        SchweizerSpielrundeSheetNaechste runde = ersteRunde(SpielplanTeamAnzeige.NAME);

        uebertragen();

        JsonArray matches = server.runden().get(1);
        assertThat(matches).hasSize((ANZ_TEAMS + 1) / 2);
        assertThat(matches).filteredOn(PtmOnlineLiveSyncUITest::istFreilos).hasSize(1);
        assertThat(alleIds(matches)).containsExactlyInAnyOrderElementsOf(alleOnlineIds());
        List<List<String>> namenImBlatt = namenImBlatt(runde);
        for (List<String> paar : paare(matches)) {
            List<String> namen = new ArrayList<>();
            for (String id : paar) {
                namen.add(runde.getMeldeListe().getTeamNameByNr(teamNr(id)));
            }
            assertThat(namenImBlatt).as("Paarung %s wie im Blatt", paar).contains(namen);
        }
    }

    @Test
    void beiPausiertemSyncWirdNichtsUebertragen() throws Exception {
        ersteRunde(SpielplanTeamAnzeige.NR);
        PtmOnlineVerbindung verbindung = alleTeamsVerbinden();
        verbindung.mapping().setPausiert(true);

        PtmOnlineLiveSync.uebertragen(verbindung,
                LiveStandQuellen.fuer(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null).orElseThrow());

        assertThat(server.runden()).isEmpty();
        assertThat(server.geloeschteRunden()).isEmpty();
        assertThat(server.rangliste()).isNull();
    }

    @Test
    void turnierMitErgebnissenUebertraegtAlleRundenUndDieRanglisteDesDokuments() throws Exception {
        new SchweizerTurnierTestDaten(wkingSpreadsheet, ANZ_TEAMS, SpielplanTeamAnzeige.NR).generate();
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());

        uebertragen();

        assertThat(server.runden()).containsOnlyKeys(1, 2, 3);
        assertThat(server.runden().values()).allSatisfy(matches -> assertThat(matches)
                .filteredOn(match -> !istFreilos(match)).allSatisfy(match -> {
                    assertThat(match.getAsJsonObject().has("scoreA")).as("Ergebnis übertragen").isTrue();
                    assertThat(match.getAsJsonObject().has("scoreB")).isTrue();
                }));
        assertThat(ranglisteOnline()).isEqualTo(ranglisteImBlatt());
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

    /** Platz, Online-Id und Siege je Zeile des Ranglisten-Blatts. */
    private List<List<String>> ranglisteImBlatt() throws Exception {
        var blatt = new SchweizerRanglisteSheet(wkingSpreadsheet).getXSpreadSheet();
        int erste = SchweizerRanglisteSheet.ERSTE_DATEN_ZEILE;
        RangeData daten = RangeHelper.from(blatt, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(SchweizerRanglisteSheet.TEAM_NR_SPALTE, erste, SchweizerRanglisteSheet.SIEGE_SPALTE,
                        erste + ANZ_TEAMS - 1))
                .getDataFromRange();
        List<List<String>> zeilen = new ArrayList<>();
        for (RowData zeile : daten) {
            zeilen.add(List.of(String.valueOf(zeile.get(SchweizerRanglisteSheet.PLATZ_SPALTE).getIntVal(0)),
                    onlineId(zeile.get(SchweizerRanglisteSheet.TEAM_NR_SPALTE).getIntVal(0)),
                    String.valueOf(zeile.get(SchweizerRanglisteSheet.SIEGE_SPALTE).getIntVal(0))));
        }
        return zeilen;
    }

    private SchweizerSpielrundeSheetNaechste ersteRunde(SpielplanTeamAnzeige anzeige) throws Exception {
        SchweizerSpielrundeSheetNaechste runde = new SchweizerSpielrundeSheetNaechste(wkingSpreadsheet);
        runde.getKonfigurationSheet().setSpielplanTeamAnzeige(anzeige);
        runde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.N);
        runde.doRun();
        return runde;
    }

    private void uebertragen() throws Exception {
        PtmOnlineLiveSync.uebertragen(alleTeamsVerbinden(),
                LiveStandQuellen.fuer(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null).orElseThrow());
    }

    /** Verbindet das Dokument und ordnet jedem Team die Online-Id {@code online-<Team-Nr>} zu. */
    private PtmOnlineVerbindung alleTeamsVerbinden() throws Exception {
        PtmOnlineRegistrationMapping mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet,
                TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Live-Testturnier";
        turnier.type = "schweizer";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);

        MeldelisteZiel ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        Map<Integer, Integer> zeileProTeam = PtmOnlineSpielrundeSync.zeileProTeam(ziel);
        Map<Integer, String> uuidProZeile = PtmOnlineSpielrundeSync.lokaleUuids(ziel, zeileProTeam.values());
        List<NeueZuordnung> zuordnungen = new ArrayList<>();
        for (Map.Entry<Integer, Integer> team : zeileProTeam.entrySet()) {
            String uuid = uuidProZeile.get(team.getValue());
            zuordnungen.add(PtmOnlineRegistrationMapping.neueZuordnung(uuid, ziel.formelTeamNrAusLokalerUuid(uuid),
                    "Team " + team.getKey(), registration(onlineId(team.getKey()))));
        }
        mapping.addMappings(zuordnungen);
        return new PtmOnlineVerbindung(server.zugangsdaten(), ziel, ziel, mapping, TURNIER_ID, null);
    }

    /** Paarungen der Spielrunde als Online-Ids (Nummern-Modus). */
    private List<List<String>> paareImBlatt(SchweizerSpielrundeSheetNaechste runde) throws Exception {
        List<List<String>> paare = new ArrayList<>();
        for (RowData zeile : teamSpalten(runde)) {
            int teamA = zeile.get(0).getIntVal(0);
            if (teamA <= 0) {
                break;
            }
            int teamB = zeile.get(1).getIntVal(0);
            paare.add(teamB > 0 ? List.of(onlineId(teamA), onlineId(teamB)) : List.of(onlineId(teamA)));
        }
        return paare;
    }

    /** Angezeigte Teamnamen je Paarung (Namens-Modus). */
    private List<List<String>> namenImBlatt(SchweizerSpielrundeSheetNaechste runde) throws Exception {
        List<List<String>> paare = new ArrayList<>();
        for (RowData zeile : teamSpalten(runde)) {
            String teamA = zeile.get(0).getStringVal();
            if (teamA == null || teamA.isBlank()) {
                break;
            }
            String teamB = zeile.get(1).getStringVal();
            paare.add(teamB == null || teamB.isBlank() ? List.of(teamA) : List.of(teamA, teamB));
        }
        return paare;
    }

    private RangeData teamSpalten(SchweizerSpielrundeSheetNaechste runde) throws Exception {
        int erste = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE;
        return RangeHelper.from(runde.getXSpreadSheet(), wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                RangePosition.from(SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE, erste,
                        SchweizerAbstractSpielrundeSheet.TEAM_B_SPALTE, erste + ANZ_TEAMS))
                .getDataFromRange();
    }

    private static List<List<String>> paare(JsonArray matches) {
        List<List<String>> paare = new ArrayList<>();
        for (JsonElement element : matches) {
            List<String> paar = new ArrayList<>(ids(element.getAsJsonObject().getAsJsonArray("teamA")));
            paar.addAll(ids(element.getAsJsonObject().getAsJsonArray("teamB")));
            paare.add(paar);
        }
        return paare;
    }

    private static List<String> alleIds(JsonArray matches) {
        return paare(matches).stream().flatMap(List::stream).toList();
    }

    private static List<String> ids(JsonArray array) {
        return StreamSupport.stream(array.spliterator(), false).map(JsonElement::getAsString).toList();
    }

    private static boolean istFreilos(JsonElement match) {
        return match.getAsJsonObject().getAsJsonArray("teamB").isEmpty();
    }

    private static List<String> alleOnlineIds() {
        List<String> ids = new ArrayList<>();
        for (int nr = 1; nr <= ANZ_TEAMS; nr++) {
            ids.add(onlineId(nr));
        }
        return ids;
    }

    private static String onlineId(int teamNr) {
        return "online-" + teamNr;
    }

    private static int teamNr(String onlineId) {
        return Integer.parseInt(onlineId.substring("online-".length()));
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
