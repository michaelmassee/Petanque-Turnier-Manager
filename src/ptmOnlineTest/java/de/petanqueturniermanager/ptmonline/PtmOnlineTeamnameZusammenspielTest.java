/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Teamnamen im Zusammenspiel PTM ↔ PTM Online (Schweizer Doublette mit Teamname-Spalte), gegen den lokal gestarteten
 * Worker: Import, Abgleich in beide Richtungen wie die Besetzung (E-16), Konflikt bei beidseitiger Änderung und kein
 * Verlust des Online-Teamnamens bei einer reinen Besetzungsänderung.
 */
class PtmOnlineTeamnameZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final Person[] TEAM_A = { Person.gast("Anna", "Adler"), Person.gast("Arno", "Amsel") };
    private static final Person[] TEAM_VOR_ORT = { Person.gast("Lena", "Lokal"), Person.gast("Lars", "Lind") };
    private static final String TEAMNAME_ONLINE = "Les Boulistes";

    @Test
    void importUebernimmtDenOnlineTeamnamen() throws Exception {
        String id = importierteAnmeldung();

        assertThat(lokalerTeamname(TEAM_A[0])).isEqualTo(TEAMNAME_ONLINE);
        assertThat(onlineTeamname(id)).isEqualTo(TEAMNAME_ONLINE);
    }

    @Test
    void nurOnlineGeaenderterTeamnameWirdLokalUebernommen() throws Exception {
        String id = importierteAnmeldung();
        online.teamnameSetzen(turnierId, id, "Carreau-Club");

        abgleichen();

        assertThat(lokalerTeamname(TEAM_A[0])).isEqualTo("Carreau-Club");
        assertThat(konfliktArten()).doesNotContain(KonfliktArt.TEAMNAMENSKONFLIKT);
    }

    @Test
    void nurLokalGeaenderterTeamnameGehtOnline() throws Exception {
        String id = importierteAnmeldung();
        lokalenTeamnamenSetzen(TEAM_A[0], "Pointeure");

        abgleichen();

        assertThat(onlineTeamname(id)).isEqualTo("Pointeure");
        assertThat(lokalerTeamname(TEAM_A[0])).isEqualTo("Pointeure");
    }

    @Test
    void vorOrtErfassteMeldungWirdMitTeamnamenOnlineAngelegt() throws Exception {
        turnierMitTeamnamenAnlegen();
        lokaleMeldung(TEAM_VOR_ORT);
        lokalenTeamnamenSetzen(TEAM_VOR_ORT[0], "Vor-Ort-Team");
        verbinden();

        abgleichen();

        assertThat(online.anmeldungen(turnierId)).singleElement()
                .satisfies(anmeldung -> assertThat(anmeldung.get("teamName").getAsString()).isEqualTo("Vor-Ort-Team"));
    }

    @Test
    void lokaleBesetzungsaenderungBehaeltDenOnlineTeamnamen() throws Exception {
        String id = importierteAnmeldung();
        int zeile = zeile(TEAM_A[0]);
        wieImSheetRunner(() -> ziel.ersetzeSpielerNamen(zeile, List.of(
                spieler(TEAM_A[0]), spieler(Person.gast("Bodo", "Brand")))));

        abgleichen();

        JsonObject anmeldung = online.anmeldung(turnierId, id).orElseThrow();
        assertThat(anmeldung.get("partnerFirstName").getAsString()).as("Besetzung online geändert").isEqualTo("Bodo");
        assertThat(anmeldung.get("teamName").getAsString()).as("Teamname nicht gelöscht").isEqualTo(TEAMNAME_ONLINE);
    }

    @Test
    void beidseitigGeaenderterTeamnameIstKonfliktBisZurEntscheidung() throws Exception {
        String id = importierteAnmeldung();
        online.teamnameSetzen(turnierId, id, "Online-Name");
        lokalenTeamnamenSetzen(TEAM_A[0], "Lokal-Name");

        abgleichen();

        assertThat(konfliktArten()).contains(KonfliktArt.TEAMNAMENSKONFLIKT);
        assertThat(lokalerTeamname(TEAM_A[0])).as("keine Seite gewinnt still").isEqualTo("Lokal-Name");
        assertThat(onlineTeamname(id)).isEqualTo("Online-Name");

        String schluessel = mapping.konfliktListe().leseFaelle().keySet().stream()
                .filter(fall -> fall.startsWith(KonfliktArt.TEAMNAMENSKONFLIKT.name() + "|")).findFirst()
                .orElseThrow();
        mapping.konfliktListe().setzeEntscheidung(schluessel, Entscheidung.LOKAL_BEHALTEN.anzeige());
        abgleichen();

        assertThat(onlineTeamname(id)).as("„lokal behalten“ überträgt den lokalen Teamnamen").isEqualTo("Lokal-Name");
        assertThat(konfliktArten()).doesNotContain(KonfliktArt.TEAMNAMENSKONFLIKT);
    }

    @Test
    void verbindenUebertraegtDenTeamnameModusDesDokuments() throws Exception {
        turnierAnlegen(Formation.DOUBLETTE, IN_EINEM_MONAT);
        assertThat(online.turnier(turnierId).get("teamNameEnabled").getAsBoolean()).isFalse();
        new SchweizerKonfigurationSheet(wkingSpreadsheet).setMeldeListeTeamnameAnzeigen(true);

        verbinden();
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("Dokument ist Master: Teamname-Spalte schaltet den Teamnamen online ein",
                () -> online.turnier(turnierId).get("teamNameEnabled").getAsBoolean());
    }

    /** Turnier mit Teamnamen, eine Online-Anmeldung mit Teamnamen, verbunden und abgeglichen. */
    private String importierteAnmeldung() throws Exception {
        turnierMitTeamnamenAnlegen();
        String id = online.anmeldenMitTeamname(turnierId, TEAMNAME_ONLINE, TEAM_A);
        verbinden();
        abgleichen();
        return id;
    }

    private void turnierMitTeamnamenAnlegen() throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("teamNameEnabled", true);
        turnierAnlegen(Formation.DOUBLETTE, IN_EINEM_MONAT, einstellungen);
    }

    private String lokalerTeamname(Person person) {
        return ziel.leseTeamnamen().orElseThrow().getOrDefault(zeile(person), "");
    }

    private void lokalenTeamnamenSetzen(Person person, String teamname) throws Exception {
        int zeile = zeile(person);
        wieImSheetRunner(() -> ziel.ersetzeTeamname(zeile, teamname));
    }

    private String onlineTeamname(String anmeldungId) throws Exception {
        JsonObject anmeldung = online.anmeldung(turnierId, anmeldungId).orElseThrow();
        return anmeldung.get("teamName").isJsonNull() ? "" : anmeldung.get("teamName").getAsString();
    }

    private static SpielerMitVerein spieler(Person person) {
        return new SpielerMitVerein(0, person.vorname(), person.nachname(), null,
                null, List.of(), List.of(), null);
    }
}
