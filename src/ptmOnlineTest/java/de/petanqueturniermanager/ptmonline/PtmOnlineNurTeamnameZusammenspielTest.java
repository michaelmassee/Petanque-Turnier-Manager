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
import de.petanqueturniermanager.spielerdb.MeldelisteSpielerDaten;
import de.petanqueturniermanager.spielerdb.MeldelisteZiel.NeueMeldungTeilnahme;
import de.petanqueturniermanager.spielerdb.SpielerMitVerein;

/**
 * Meldeliste „Nur Teamname“ im Zusammenspiel mit PTM Online (Formation „andere“), gegen den lokal gestarteten Worker:
 * Der Teamname ist die Besetzung der Meldung; die Online-Kontaktperson gehört nur zur Anmeldung und bleibt erhalten.
 */
class PtmOnlineNurTeamnameZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final Person KONTAKT = Person.gast("Hans", "Huber");
    private static final String TEAMNAME = "Boule-Asse";

    @Test
    void importUebernimmtDenTeamnamenAlsMeldung() throws Exception {
        importierteAnmeldung();

        assertThat(lokaleTeamnamen()).containsExactly(TEAMNAME);
    }

    @Test
    void lokalGeaenderterTeamnameGehtOnlineUndDieKontaktpersonBleibt() throws Exception {
        String id = importierteAnmeldung();
        int zeile = ziel.findeZeileMitName(TEAMNAME);
        wieImSheetRunner(() -> ziel.ersetzeSpielerNamen(zeile, List.of(teamname("Carreau-Club"))));

        abgleichen();

        JsonObject anmeldung = online.anmeldung(turnierId, id).orElseThrow();
        assertThat(anmeldung.get("teamName").getAsString()).isEqualTo("Carreau-Club");
        assertThat(anmeldung.get("firstName").getAsString()).as("Kontaktperson unverändert").isEqualTo("Hans");
        assertThat(anmeldung.get("lastName").getAsString()).isEqualTo("Huber");
    }

    @Test
    void onlineGeaenderterTeamnameWirdLokalUebernommen() throws Exception {
        String id = importierteAnmeldung();
        online.teamnameSetzen(turnierId, id, "Pointeure");

        abgleichen();

        assertThat(lokaleTeamnamen()).containsExactly("Pointeure");
    }

    @Test
    void vorOrtErfassteMeldungWirdOhneKontaktpersonOnlineAngelegt() throws Exception {
        nurTeamnameTurnierAnlegen();
        wieImSheetRunner(() -> ziel.schreibeBlockUndLiefereZeile(List.of(teamname("Vor-Ort-Team")),
                NeueMeldungTeilnahme.INAKTIV));
        verbinden();

        abgleichen();

        assertThat(online.anmeldungen(turnierId)).singleElement().satisfies(anmeldung -> {
            assertThat(anmeldung.get("teamName").getAsString()).isEqualTo("Vor-Ort-Team");
            assertThat(anmeldung.get("firstName").getAsString()).isEmpty();
        });
        abgleichen();
        assertThat(lokaleTeamnamen()).as("zweiter Abgleich ändert nichts").containsExactly("Vor-Ort-Team");
    }

    /** Turnier „Nur Teamname“, eine Online-Anmeldung mit Kontaktperson, verbunden und abgeglichen. */
    private String importierteAnmeldung() throws Exception {
        nurTeamnameTurnierAnlegen();
        String id = online.anmeldenMitTeamname(turnierId, TEAMNAME, KONTAKT);
        verbinden();
        abgleichen();
        return id;
    }

    private void nurTeamnameTurnierAnlegen() throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("formation", "andere");
        turnierAnlegen(Formation.NUR_TEAMNAME, IN_EINEM_MONAT, einstellungen);
    }

    private List<String> lokaleTeamnamen() {
        return ziel.leseAlleSpielerRoh().stream().map(MeldelisteSpielerDaten::vorname).toList();
    }

    private static SpielerMitVerein teamname(String name) {
        return new SpielerMitVerein(0, name, "", null, null, List.of(), List.of(), null);
    }
}
