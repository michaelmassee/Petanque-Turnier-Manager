/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerRankingModus;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;

/**
 * Ein verbundenes Turnierdokument ist Master – ab dem Verbinden, nicht erst ab seinem Start: Online lassen sich das
 * Turnier nicht mehr starten, keine Runden auslosen und keine Ergebnisse eintragen oder melden; die Spieleinstellungen
 * (Formation, Ranglistenmodus) kommen aus dem Dokument, und die Live-Ansicht zeigt Rangliste und Bahn wie bei einem
 * online durchgeführten Turnier.
 */
class PtmOnlineDokumentIstMasterTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final int HTTP_OK = 200;
    private static final int HTTP_KONFLIKT = 409;
    private static final int SPIELERKONTO = 1;
    /** Team mit Konto (Spieler 1), damit die Live-Ansicht als angemeldeter Spieler abrufbar ist. */
    private static final Person[] TEAM_MIT_KONTO = { Person.konto("Kim", "Konto", SPIELERKONTO),
            Person.gast("Kai", "Kern"), Person.gast("Kurt", "Kolb") };

    @Test
    void nachDemVerbindenSindOnlineStartUndAuslosungGesperrt() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        vollstaendigeTeams(6);
        verbinden();
        abgleichen();

        assertThat(online.turnierStarten(turnierId)).as("Online-Start").isEqualTo(HTTP_KONFLIKT);
        assertThat(online.rundeAuslosen(turnierId)).as("Online-Auslosung").isEqualTo(HTTP_KONFLIKT);
        assertThat(online.turnier(turnierId).get("status").getAsString()).isEqualTo("registration");
        assertThat(online.anmeldungen(turnierId)).as("kein automatischer Check-in durch einen Online-Start")
                .allSatisfy(a -> assertThat(a.get("participation").getAsString()).isEqualTo("inactive"));
    }

    @Test
    void dokumentStartetUndErgebnisseKoennenOnlineNichtGesetztWerden() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT, mitErgebnismeldung());
        Map<String, Person[]> teams = vollstaendigeTeams(5);
        String idMitKonto = online.anmelden(turnierId, TEAM_MIT_KONTO);
        verbinden();
        abgleichen();
        assertThat(online.turnierStarten(turnierId)).isEqualTo(HTTP_KONFLIKT);
        for (Person[] team : teams.values()) {
            einchecken(zeile(team[0]));
        }
        einchecken(zeile(TEAM_MIT_KONTO[0]));

        schweizerRundeAuslosen();

        warteBis("Turnier aus dem Dokument gestartet",
                () -> "running".equals(online.turnier(turnierId).get("status").getAsString()));
        warteBis("Runde 1 online", () -> spieleOnline().size() == paareImBlatt().size());
        assertThat(spieleOnline()).as("Bahn aus dem Dokument")
                .allSatisfy(spiel -> assertThat(spiel.has("court") && !spiel.get("court").isJsonNull()).isTrue());
        String partieId = spieleOnline().getFirst().get("id").getAsString();
        assertThat(online.ergebnisSetzen(turnierId, partieId, 13, 7)).as("Ergebnis der Turnierleitung online")
                .isEqualTo(HTTP_KONFLIKT);
        assertThat(online.ergebnisMelden(idMitKonto, SPIELERKONTO, 13, 7))
                .as("Spielermeldung nur als offene Meldung an das Dokument (E-27)").isEqualTo(HTTP_OK);
        assertThat(spieleOnline()).as("online weiterhin ohne Ergebnis")
                .allSatisfy(spiel -> assertThat(!spiel.has("scoreA") || spiel.get("scoreA").isJsonNull()).isTrue());
        assertThat(online.offeneErgebnismeldungen(turnierId)).as("wartet auf die Turnierleitung").hasSize(1);
        JsonObject live = online.live(idMitKonto, SPIELERKONTO);
        assertThat(live.getAsJsonObject("tournament").get("documentBound").getAsBoolean()).isTrue();
        assertThat(live.getAsJsonObject("live").getAsJsonObject("currentMatch").get("court").getAsString())
                .as("Bahn in der Live-Ansicht").isNotBlank();
    }

    @Test
    void spieleinstellungenDesDokumentsUeberschreibenOnline() throws Exception {
        JsonObject abweichend = new JsonObject();
        abweichend.addProperty("formation", "doublette");
        abweichend.addProperty("schweizerRankingMode", "ohne_buchholz");
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT, abweichend);
        SchweizerKonfigurationSheet konfiguration = new SchweizerKonfigurationSheet(wkingSpreadsheet);
        konfiguration.setRankingModus(SchweizerRankingModus.MIT_BUCHHOLZ);

        verbinden();
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("Formation und Ranglistenmodus des Dokuments online", () -> {
            JsonObject turnier = online.turnier(turnierId);
            return "triplette".equals(turnier.get("formation").getAsString())
                    && "mit_buchholz".equals(turnier.get("schweizerRankingMode").getAsString());
        });

        konfiguration.setRankingModus(SchweizerRankingModus.OHNE_BUCHHOLZ);
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("geänderter Ranglistenmodus online", () -> "ohne_buchholz"
                .equals(online.turnier(turnierId).get("schweizerRankingMode").getAsString()));
        assertThat(online.turnierBearbeiten(turnierId, online.turnier(turnierId)))
                .as("Bearbeiten der Einstellungen im Web").isEqualTo(HTTP_KONFLIKT);
    }

    @Test
    void ranglisteKommtMitBuchholzUndBleibtNachDemZuruecksetzen() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        Map<String, Person[]> teams = vollstaendigeTeams(5);
        String idMitKonto = online.anmelden(turnierId, TEAM_MIT_KONTO);
        verbinden();
        abgleichen();
        for (Person[] team : teams.values()) {
            einchecken(zeile(team[0]));
        }
        einchecken(zeile(TEAM_MIT_KONTO[0]));
        schweizerRundeAuslosen();
        wieImSheetRunner(new SchweizerRanglisteSheet(wkingSpreadsheet)::doRun);
        PtmOnlineLiveBeobachter.anstossen(wkingSpreadsheet.getWorkingSpreadsheetDocument());

        warteBis("Rangliste mit Buchholz online", () -> {
            JsonArray rangliste = online.rangliste(turnierId);
            return rangliste.size() == teams.size() + 1 && alleMit(rangliste, "bhz") && alleMit(rangliste, "fbhz");
        });
        JsonArray liveRangliste = online.live(idMitKonto, SPIELERKONTO).getAsJsonObject("live")
                .getAsJsonArray("ranking");
        assertThat(alleMit(liveRangliste, "bhz")).as("Buchholz in der Live-Ansicht").isTrue();

        // Ohne Ergebnis darf die Turnierleitung den Start zurücksetzen; die Rangliste bleibt die des Dokuments.
        online.startZuruecksetzen(turnierId);

        assertThat(online.rangliste(turnierId)).as("Snapshot des Dokuments").hasSize(teams.size() + 1);
    }

    private static JsonObject mitErgebnismeldung() {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("playerResultReportingEnabled", true);
        return einstellungen;
    }

    private static boolean alleMit(JsonArray eintraege, String feld) {
        for (JsonElement eintrag : eintraege) {
            JsonObject objekt = eintrag.getAsJsonObject();
            if (!objekt.has(feld) || objekt.get(feld).isJsonNull()) {
                return false;
            }
        }
        return !eintraege.isEmpty();
    }

    /** Partien der ersten Online-Runde (leer, solange sie nicht übertragen ist). */
    private List<JsonObject> spieleOnline() throws Exception {
        List<JsonObject> spiele = new ArrayList<>();
        List<JsonObject> runden = online.runden(turnierId);
        if (!runden.isEmpty()) {
            runden.getFirst().getAsJsonArray("matches").forEach(spiel -> spiele.add(spiel.getAsJsonObject()));
        }
        return spiele;
    }
}
