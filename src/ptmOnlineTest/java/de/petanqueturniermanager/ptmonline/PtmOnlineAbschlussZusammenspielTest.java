/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;

/**
 * „Turnier online abschließen“ gegen den lokal gestarteten Worker. Ein privates Turnier ohne Freigabelink brach den
 * Abschluss mit Benachrichtigung früher ab; jetzt fragt PTM nach, aktiviert den Link auf Zustimmung und schließt mit
 * veröffentlichter Endrangliste und Benachrichtigung ab. Im Test beantworten die übersprungenen Dialoge jede Frage
 * mit „Ja“.
 */
class PtmOnlineAbschlussZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final int SPIELERKONTO = 10;
    private static final Person[] TEAM_MIT_KONTO = { Person.konto("Fiona", "Finale", SPIELERKONTO),
            Person.gast("Felix", "Finale"), Person.gast("Frida", "Finale") };
    private static final String EREIGNIS_ENDRANGLISTE = "tournament_ranking_published";

    @Test
    void privatesTurnierAktiviertDenFreigabelinkUndBenachrichtigtDieTeilnehmer() throws Exception {
        rundeMitErgebnissenEinrichten();
        online.sichtbarkeitSetzen(turnierId, "private");
        TournamentSyncClient client = new TournamentSyncClient(server.baseUrl(), server.apiKey());
        assertThat(client.holeFreigabe(turnierId).fehlt()).as("privat, Freigabelink fehlt").isTrue();

        PtmOnlineAbschlussRunner runner = new PtmOnlineAbschlussRunner(wkingSpreadsheet, system, spieltagNr);
        starteUndWarte(runner);

        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Abschluss ohne Fehler").isFalse();
        assertThat(online.turnier(turnierId).get("status").getAsString()).as("online abgeschlossen")
                .isEqualTo("finished");
        assertThat(client.holeFreigabe(turnierId)).as("Freigabelink aktiviert")
                .isEqualTo(new TournamentSyncClient.Freigabe(true, true));
        assertThat(online.postfach(SPIELERKONTO)).as("Teilnehmer über die Endrangliste benachrichtigt")
                .anySatisfy(nachricht -> assertThat(istEndranglisteDiesesTurniers(nachricht)).isTrue());
    }

    @Test
    void oeffentlichesTurnierBrauchtKeinenFreigabelink() throws Exception {
        rundeMitErgebnissenEinrichten();
        TournamentSyncClient client = new TournamentSyncClient(server.baseUrl(), server.apiKey());
        assertThat(client.holeFreigabe(turnierId)).isEqualTo(new TournamentSyncClient.Freigabe(false, true));

        PtmOnlineAbschlussRunner runner = new PtmOnlineAbschlussRunner(wkingSpreadsheet, system, spieltagNr);
        starteUndWarte(runner);

        assertThat(runner.isLetzterLaufFehlgeschlagen()).as("Abschluss ohne Fehler").isFalse();
        assertThat(online.turnier(turnierId).get("status").getAsString()).isEqualTo("finished");
        assertThat(online.postfach(SPIELERKONTO))
                .anySatisfy(nachricht -> assertThat(istEndranglisteDiesesTurniers(nachricht)).isTrue());
    }

    /** Schweizer Triplette mit sechs Teams, verbunden, eingecheckt, Runde 1 ausgelost und alle Ergebnisse eingetragen. */
    private void rundeMitErgebnissenEinrichten() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        online.anmelden(turnierId, TEAM_MIT_KONTO);
        Map<String, Person[]> weitere = vollstaendigeTeams(5);
        verbinden();
        abgleichen();
        einchecken(zeile(TEAM_MIT_KONTO[0]));
        for (Person[] team : weitere.values()) {
            einchecken(zeile(team[0]));
        }
        schweizerRundeAuslosen();
        warteBis("Runde 1 online", () -> !online.runden(turnierId).isEmpty());
        for (int[] paar : paareImBlatt()) {
            ergebnisEintragen(paar[0], 13, 7);
        }
    }

    private boolean istEndranglisteDiesesTurniers(JsonObject nachricht) {
        JsonElement typ = nachricht.get("eventType");
        JsonElement daten = nachricht.get("eventData");
        return typ != null && typ.isJsonPrimitive() && EREIGNIS_ENDRANGLISTE.equals(typ.getAsString())
                && daten != null && daten.isJsonObject()
                && turnierId.equals(daten.getAsJsonObject().get("tournamentId").getAsString());
    }
}
