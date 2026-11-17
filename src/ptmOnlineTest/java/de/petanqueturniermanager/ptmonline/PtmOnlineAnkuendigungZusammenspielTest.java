/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;

import de.petanqueturniermanager.ankuendigung.Ankuendigung;
import de.petanqueturniermanager.ankuendigung.AnkuendigungVersandRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;

/**
 * Ankündigung aus dem Turnierdokument gegen den lokal gestarteten Worker: Sie erscheint am Online-Turnier und lässt
 * sich aus dem Dokument wieder löschen.
 */
class PtmOnlineAnkuendigungZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final Person[] TEAM = { Person.gast("Anke", "Ansage"), Person.gast("Arno", "Ansage") };
    private static final String TEXT = "Runde 1 startet um 10 Uhr\nBitte an Bahn 3 melden";

    @Test
    void ankuendigungErscheintOnlineUndLaesstSichLoeschen() throws Exception {
        turnierAnlegen(Formation.DOUBLETTE, IN_EINEM_MONAT);
        online.anmelden(turnierId, TEAM);
        verbinden();
        abgleichen();

        sendeAnkuendigung(TEXT);

        JsonElement ankuendigung = online.turnier(turnierId).get("announcement");
        assertThat(ankuendigung.isJsonObject()).as("Ankündigung online gesetzt").isTrue();
        assertThat(ankuendigung.getAsJsonObject().get("text").getAsString()).isEqualTo(TEXT);

        sendeAnkuendigung("");

        assertThat(online.turnier(turnierId).get("announcement").isJsonNull()).as("Ankündigung online gelöscht")
                .isTrue();
    }

    private void sendeAnkuendigung(String text) throws Exception {
        String gespeichert = Ankuendigung.speichere(wkingSpreadsheet, text);
        AnkuendigungVersandRunner runner = new AnkuendigungVersandRunner(wkingSpreadsheet, system, gespeichert, null);
        starteUndWarte(runner);
        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Versand ohne Fehler").isFalse();
    }
}
