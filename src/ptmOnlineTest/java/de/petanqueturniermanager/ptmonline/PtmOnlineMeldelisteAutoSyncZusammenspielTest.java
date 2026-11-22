/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsBestand;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Automatischer Meldelisten-Abgleich gegen den lokal gestarteten Worker (Schweizer Doublette mit Teamname-Spalte): Das
 * Dokument ist Master – geänderte Namen und Teamnamen, neue und gelöschte Meldungen gehen ohne „Abgleichen“ online,
 * auch über zwischenzeitlich online geänderte Stände hinweg. Eine gleichnamige, noch nicht verknüpfte
 * Online-Anmeldung wird nicht verdoppelt.
 */
class PtmOnlineMeldelisteAutoSyncZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final Person[] TEAM_A = { Person.gast("Anna", "Adler"), Person.gast("Arno", "Amsel") };
    private static final Person[] TEAM_B = { Person.gast("Bert", "Berg"), Person.gast("Bea", "Busch") };
    private static final Person[] TEAM_VOR_ORT = { Person.gast("Lena", "Lokal"), Person.gast("Lars", "Lind") };
    private static final String TEAMNAME = "Les Boulistes";

    @Test
    void geaenderterNameGehtOhneAbgleichOnline() throws Exception {
        String id = importierteAnmeldung();
        int zeile = zeile(TEAM_A[0]);

        wieImSheetRunner(() -> ziel.ersetzeSpielerNamen(zeile, List.of(spieler(TEAM_A[0]),
                spieler(Person.gast("Bodo", "Brand")))));
        anstossen();

        warteBis("geänderter Partner online", () -> "Bodo".equals(feld(id, "partnerFirstName")));
        assertThat(feld(id, "teamName")).as("Teamname bleibt").isEqualTo(TEAMNAME);
    }

    @Test
    void geaenderterTeamnameUeberschreibtAuchEinenOnlineGeaendertenStand() throws Exception {
        String id = importierteAnmeldung();
        online.teamnameSetzen(turnierId, id, "Online-Name");
        int zeile = zeile(TEAM_A[0]);

        wieImSheetRunner(() -> ziel.ersetzeTeamname(zeile, "Lokal-Name"));
        anstossen();

        warteBis("das Dokument ist Master", () -> "Lokal-Name".equals(feld(id, "teamName")));
    }

    @Test
    void neueMeldungWirdOhneAbgleichOnlineAngelegt() throws Exception {
        importierteAnmeldung();
        lokaleMeldung(TEAM_VOR_ORT);
        meldelisteAktualisieren();
        anstossen();

        warteBis("vor Ort erfasste Meldung online", () -> online.anmeldungen(turnierId).stream()
                .anyMatch(anmeldung -> "Lena".equals(text(anmeldung, "firstName"))));
        warteBis("Zuordnung im Dokument", () -> {
            mapping = new PtmOnlineRegistrationMapping(wkingSpreadsheet, system, spieltagNr);
            return mapping.getOnlineIdsProUuid().size() == 2;
        });
    }

    @Test
    void geloeschteMeldungWirdOnlineStorniert() throws Exception {
        String idA = importierteAnmeldung();
        String idB = online.anmelden(turnierId, TEAM_B);
        abgleichen();
        String statusB = feld(idB, "status");
        int zeileA = zeile(TEAM_A[0]);

        wieImSheetRunner(() -> ziel.entferneMeldung(zeileA));
        meldelisteAktualisieren();
        anstossen();

        warteBis("gelöschte Meldung online storniert", () -> "cancelled".equals(feld(idA, "status")));
        assertThat(feld(idB, "status")).as("übrige Meldung unverändert").isEqualTo(statusB);
    }

    @Test
    void gleichnamigeOnlineAnmeldungWirdNichtVerdoppelt() throws Exception {
        importierteAnmeldung();
        online.anmelden(turnierId, TEAM_VOR_ORT);
        lokaleMeldung(TEAM_VOR_ORT);
        meldelisteAktualisieren();
        anstossen();

        warteBis("automatische Anlage abgelehnt", this::anlageAbgelehnt);
        assertThat(online.anmeldungen(turnierId).stream()
                .filter(anmeldung -> "Lena".equals(text(anmeldung, "firstName")))).as("keine Dublette").hasSize(1);
    }

    /** Turnier mit Teamnamen, eine Online-Anmeldung mit Teamnamen, verbunden und abgeglichen. */
    private String importierteAnmeldung() throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("teamNameEnabled", true);
        turnierAnlegen(Formation.DOUBLETTE, IN_EINEM_MONAT, einstellungen);
        String id = online.anmeldenMitTeamname(turnierId, TEAMNAME, TEAM_A);
        verbinden();
        abgleichen();
        return id;
    }

    /** Wie „Meldeliste aktualisieren“: vergibt neuen Zeilen ihre lokale UUID und schließt Lücken. */
    private void meldelisteAktualisieren() throws Exception {
        wieImSheetRunner(new SchweizerMeldeListeSheetUpdate(wkingSpreadsheet)::upDateSheet);
    }

    private void anstossen() {
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());
    }

    private boolean anlageAbgelehnt() {
        Optional<AuftragsBestand> bestand = PtmOnlineAuftraege.geladen(wkingSpreadsheet.getWorkingSpreadsheetDocument(),
                spieltagNr);
        return bestand.map(b -> b.eintraege().stream().anyMatch(eintrag -> eintrag.auftrag()
                .art() == AuftragsArt.ANMELDUNG_ANLEGEN && eintrag.zustand() == AuftragsBestand.Zustand.ABGELEHNT
                && "possible_same_registration".equals(eintrag.grund()))).orElse(false);
    }

    private String feld(String anmeldungId, String feld) throws Exception {
        return text(online.anmeldung(turnierId, anmeldungId).orElseThrow(), feld);
    }

    private static String text(JsonObject anmeldung, String feld) {
        JsonElement wert = anmeldung.get(feld);
        return wert == null || wert.isJsonNull() ? "" : wert.getAsString();
    }

    private static SpielerMitVerein spieler(Person person) {
        return new SpielerMitVerein(0, person.vorname(), person.nachname(), null, null, List.of(), List.of(), null);
    }
}
