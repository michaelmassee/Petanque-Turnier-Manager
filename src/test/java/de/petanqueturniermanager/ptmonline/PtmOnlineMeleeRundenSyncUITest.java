/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.konfiguration.MeleeAnmeldungKonfiguration;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.onlinesync.OnlineTournamentDto;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.SyncBindingDto;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetNew;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeleeAnmeldungUebernehmenSheet;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.spielerdb.MeldelisteZielFactory;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Rundenstart-Sync bei Mêlée-Anmeldung gegen einen lokalen Test-Server: Online gibt es nur
 * Einzelanmeldungen, gespielt wird in lokal gemischten Doublettes. Jede Einzelanmeldung erhält die
 * Teilnahme ihres Teams; ein vor Ort erfasster, aktiver Spieler wird online als Einzelspieler angelegt –
 * nie als Team.
 */
class PtmOnlineMeleeRundenSyncUITest extends BaseCalcUITest {

    private static final String TURNIER_ID = "t1";
    private static final String DOCUMENT_ID = "e9e9caec-e0b1-4fe0-8fee-a229279b9f73";
    private static final String LEASE_TOKEN = "01234567890123456789012345678901";
    private static final List<String> SPIELER = List.of("Hans Müller", "Anna Schmidt", "Eva Klein", "Paul Neu");
    /** Online-Id je Spieler; Paul Neu ist vor Ort erfasst und online noch unbekannt. */
    private static final Map<String, String> ONLINE_ID = Map.of(
            "Hans Müller", "r1", "Anna Schmidt", "r2", "Eva Klein", "r3");

    private PtmOnlineTestServer server;
    private MeldelisteZiel ziel;
    private MeldelisteZiel meldeliste;
    private PtmOnlineRegistrationMapping mapping;

    @BeforeEach
    void meleeTurnierMitGemischtenTeamsAnlegen() throws Exception {
        RandomSource.setSeed(42L);
        MessageBox.setDialogeUeberspringen(true);
        server = new PtmOnlineTestServer(TURNIER_ID, "{\"registrations\":[]}");

        new SchweizerMeldeListeSheetNew(wkingSpreadsheet).createMeldelisteWithParams(Formation.DOUBLETTE, false,
                false);
        docPropHelper.setIntProperty(BasePropertiesSpalte.KONFIG_PROP_NAME_TURNIERSYSTEM,
                TurnierSystem.SCHWEIZER.getId());
        MeleeAnmeldungKonfiguration.einschalten(wkingSpreadsheet);
        new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).generate();
        ziel = MeldelisteZielFactory.fuerPtmOnline(wkingSpreadsheet).orElseThrow();
        verbinden();

        RegistrationImportTask.uebernehmeAnmeldungen(
                List.of(anmeldung("r1", "Hans", "Müller"), anmeldung("r2", "Anna", "Schmidt"),
                        anmeldung("r3", "Eva", "Klein")),
                mapping, ziel,
                () -> MeldelisteZielFactory.aktualisiereZielSynchron(wkingSpreadsheet, TurnierSystem.SCHWEIZER, ziel),
                AbgleichFortschritt.OHNE);
        ziel.schreibeBlockUndLiefereZeile(List.of(spieler("Paul", "Neu")), NeueMeldungTeilnahme.AKTIV);
        alleEinchecken();
        new SchweizerMeleeAnmeldungUebernehmenSheet(wkingSpreadsheet).uebernehmen();
        meldeliste = MeldelisteZielFactory.fuerAktivesSheet(wkingSpreadsheet).orElseThrow();
    }

    @AfterEach
    void aufraeumen() {
        server.close();
        MessageBox.setDialogeUeberspringen(false);
        RandomSource.reset();
    }

    @Test
    void aktiveTeamsMeldenAlleMitgliederAktivUndLegenVorOrtErfasstenEinzelnAn() throws Exception {
        Set<Integer> alleTeams = teamNummern(SPIELER);

        List<String> abgelehnt = synchronisiere(alleTeams, alleTeams, Set.of());

        assertThat(abgelehnt).isEmpty();
        assertThat(teilnahmeProOnlineId()).containsOnlyKeys("r1", "r2", "r3");
        assertThat(teilnahmeProOnlineId().values()).containsOnly("active");
        assertThat(server.gepushteErgebnisse()).allSatisfy(eintrag -> assertThat(eintrag.has("seedingPosition"))
                .as("Mêlée-Spieler ohne Setzposition: nicht die Team-Nr melden").isFalse());
        assertThat(server.onlineAngelegt()).singleElement().satisfies(anmeldung -> {
            assertThat(anmeldung.get("firstName").getAsString()).isEqualTo("Paul");
            assertThat(anmeldung.has("partnerFirstName")).as("Einzelspieler, kein Team").isFalse();
        });
        assertThat(mapping.getLokaleUuid("online-1")).isPresent();
    }

    @Test
    void ausgesetztesTeamMeldetSeineMitgliederAusgesetzt() throws Exception {
        int teamVonHans = teamNr("Hans Müller");
        Set<Integer> alleTeams = teamNummern(SPIELER);
        Set<Integer> ausgesetzt = new HashSet<>(alleTeams);
        ausgesetzt.remove(teamVonHans);

        synchronisiere(alleTeams, Set.of(teamVonHans), ausgesetzt);

        Map<String, String> erwartet = ONLINE_ID.entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue,
                eintrag -> teamNr(eintrag.getKey()) == teamVonHans ? "active" : "withdrawn"));
        assertThat(teilnahmeProOnlineId()).isEqualTo(erwartet);
        assertThat(server.onlineAngelegt()).as("Paul Neu nur angelegt, wenn sein Team spielt")
                .hasSize(teamNr("Paul Neu") == teamVonHans ? 1 : 0);
    }

    private List<String> synchronisiere(Set<Integer> alle, Set<Integer> aktive, Set<Integer> ausgesetzt)
            throws Exception {
        List<LokaleOnlineMeldung> meldungen = PtmOnlineSpielrundeSync.lokaleMeldungen(ziel, meldeliste, alle, aktive,
                ausgesetzt);
        var zugang = server.zugangsdaten();
        TournamentSyncClient client = new TournamentSyncClient(zugang.baseUrl(), zugang.apiKey(), DOCUMENT_ID,
                LEASE_TOKEN);
        return PtmOnlineSpielrundeSync.statusPushenUndNeueAnlegen(ziel, mapping, client, TURNIER_ID, meldungen);
    }

    private Map<String, String> teilnahmeProOnlineId() {
        return server.gepushteErgebnisse().stream().collect(Collectors.toMap(eintrag -> eintrag.get("id").getAsString(),
                eintrag -> eintrag.get("participation").getAsString()));
    }

    private Set<Integer> teamNummern(List<String> namen) {
        return namen.stream().map(this::teamNr).collect(Collectors.toSet());
    }

    private int teamNr(String name) {
        return meldeliste.getTeamNrAusZeile(meldeliste.findeZeileMitName(name));
    }

    private void verbinden() throws Exception {
        mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, TurnierSystem.SCHWEIZER, null);
        OnlineTournamentDto turnier = new OnlineTournamentDto();
        turnier.id = TURNIER_ID;
        turnier.name = "Mêlée-Testturnier";
        turnier.type = "schweizer";
        mapping.verbinden(turnier, new SyncBindingDto(true, DOCUMENT_ID, 1), LEASE_TOKEN);
    }

    /** Eingecheckt-Markierung für alle vier Spieler, als Block geschrieben. */
    private void alleEinchecken() throws Exception {
        RangeData data = new RangeData();
        Stream.generate(() -> MeleeAnmeldungKonstanten.MARKIERUNG).limit(SPIELER.size())
                .forEach(markierung -> data.addNewRow().newString(markierung));
        var meleeSheet = new SchweizerMeleeAnmeldungSheet(wkingSpreadsheet).getXSpreadSheet();
        RangeHelper.from(meleeSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                data.getRangePosition(Position.from(MeleeAnmeldungKonstanten.SPALTE_EINGECHECKT,
                        MeleeAnmeldungKonstanten.ERSTE_DATEN_ZEILE)))
                .setDataInRange(data);
    }

    private static RegistrationDto anmeldung(String id, String vorname, String nachname) {
        JsonObject json = new JsonObject();
        json.addProperty("id", id);
        json.addProperty("firstName", vorname);
        json.addProperty("lastName", nachname);
        json.addProperty("status", "confirmed");
        return new Gson().fromJson(json, RegistrationDto.class);
    }

    private static SpielerMitVerein spieler(String vorname, String nachname) {
        return new SpielerMitVerein(0, vorname, nachname, null, null, List.of(), List.of(), null);
    }
}
