/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.HttpURLConnection;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.ptmonline.auftrag.AuftragsArt;

/**
 * Ablehnung einer Online-Anmeldung durch die Turnierleitung (E-26) im Zusammenspiel mit dem verbundenen Dokument:
 * P-79 (abgelehnte offene Anmeldung) und P-80 (Ablehnen nur über die eigene Aktion, Wiederaufnahme, Ablehnung einer
 * bereits importierten Meldung). Gegen den lokal gestarteten Worker.
 * <p>
 * Nicht hier geprüft, weil der Test-Worker ohne Mailversand läuft: die E-Mail an das Team und der Live-Link mit Token
 * aus der Anmeldebestätigung. Beides deckt {@code worker.registration-rejection.test.js} bzw.
 * {@code worker.live-link.test.js} im Online-Projekt ab.
 */
class PtmOnlineAblehnungZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final String TEAMNAME = "Boule-Asse";
    private static final String GRUND = "Turnier ist voll.";
    /** Spieler 1 der abgelehnten Anmeldung hat ein Konto: Postfach und Live-Ansicht sind prüfbar. */
    private static final int SPIELERKONTO = 7;

    private static final Person[] TEAM_MIT_KONTO = { Person.konto("Rita", "Roth", SPIELERKONTO),
            Person.gast("Rolf", "Rahn"), Person.gast("Rosa", "Ries") };
    private static final Person[] TEAM_A = { Person.gast("Anna", "Adler"), Person.gast("Arno", "Amsel"),
            Person.gast("Ada", "Auer") };
    private static final Person[] TEAM_B = { Person.gast("Bert", "Berg"), Person.gast("Bea", "Busch"),
            Person.gast("Bodo", "Brand") };

    @Test
    void abgelehnteOffeneAnmeldungWirdNichtUebernommenUndGibtIhrenTeamnamenFrei() throws Exception {
        turnierMitFreigabeAnlegen();
        String idAbgelehnt = online.anmeldenMitTeamname(turnierId, TEAMNAME, TEAM_MIT_KONTO);
        assertThat(online.anmeldenVersuchen(turnierId, TEAMNAME, TEAM_B)).as("Teamname durch die offene Anmeldung belegt")
                .isEqualTo(HttpURLConnection.HTTP_CONFLICT);
        assertThat(online.liveStatus(idAbgelehnt, SPIELERKONTO)).as("Live-Ansicht vor der Ablehnung")
                .isEqualTo(HttpURLConnection.HTTP_OK);
        verbinden();

        online.ablehnen(idAbgelehnt, GRUND);
        String idNeu = online.anmeldenMitTeamname(turnierId, TEAMNAME, TEAM_B);
        online.statusSetzen(turnierId, idNeu, "confirmed");
        abgleichen();

        JsonObject abgelehnt = online.anmeldung(turnierId, idAbgelehnt).orElseThrow();
        assertThat(abgelehnt.get("status").getAsString()).as("Status in der Verwaltung").isEqualTo("rejected");
        assertThat(abgelehnt.get("rejectionReason").getAsString()).as("Grund in der Verwaltung").isEqualTo(GRUND);
        assertThat(online.postfach(SPIELERKONTO)).as("Postfach-Nachricht an das Team mit Grund")
                .anySatisfy(nachricht -> assertThat(ablehnungsGrund(nachricht)).isEqualTo(GRUND));
        assertThat(online.oeffentlicheMeldeliste(turnierId)).as("öffentliche Meldeliste nur mit dem neuen Team")
                .singleElement().satisfies(teilnehmer -> assertThat(teilnehmer.get("lastName").getAsString())
                        .isEqualTo(TEAM_B[0].nachname()));
        assertThat(online.liveStatus(idAbgelehnt, SPIELERKONTO)).as("Live-Ansicht der abgelehnten Anmeldung")
                .isEqualTo(HttpURLConnection.HTTP_NOT_FOUND);
        assertThat(lokaleMeldungen().values()).as("PTM übernimmt nur das neue Team").containsExactly(namen(TEAM_B));
        assertThat(mapping.istBereitsImportiert(idAbgelehnt)).isFalse();
        assertThat(mapping.istBereitsImportiert(idNeu)).isTrue();
    }

    @Test
    void imBearbeitenDialogLaesstSichNichtAblehnenUndDieWiederaufnahmeLeertDieAblehnung() throws Exception {
        turnierMitFreigabeAnlegen();
        String idA = online.anmelden(turnierId, TEAM_A);
        verbinden();

        assertThat(online.statusSetzenVersuchen(turnierId, idA, "rejected")).as("Ablehnen über Bearbeiten (P-80 a)")
                .isEqualTo(HttpURLConnection.HTTP_BAD_REQUEST);
        assertThat(onlineStatus(idA)).as("Status unverändert").isEqualTo("pending");

        online.ablehnen(idA, GRUND);
        online.statusSetzen(turnierId, idA, "confirmed");
        abgleichen();

        JsonObject wiederAufgenommen = online.anmeldung(turnierId, idA).orElseThrow();
        assertThat(wiederAufgenommen.get("status").getAsString()).as("bestätigt (P-80 b)").isEqualTo("confirmed");
        assertThat(wiederAufgenommen.get("rejectionReason").isJsonNull()).as("Grund geleert").isTrue();
        assertThat(wiederAufgenommen.get("rejectedAt").isJsonNull()).as("Zeitpunkt geleert").isTrue();
        assertThat(lokaleMeldungen().values()).as("wieder aufgenommen: übernommen").containsExactly(namen(TEAM_A));
    }

    @Test
    void perDokumentSyncLaesstSichNichtAblehnen() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        verbinden();
        abgleichen();
        String uuidA = mapping.getLokaleUuid(idA).orElseThrow();

        // PTM selbst sendet nie „rejected“; der Auftrag prüft, dass PTM-Online den Status auch über den Sync abweist.
        JsonObject body = new JsonObject();
        body.addProperty("onlineRegistrationId", idA);
        body.addProperty("expectedExecutionRevision", mapping.getExecutionRevisionenProUuid().get(uuidA));
        body.addProperty("status", "rejected");
        PtmOnlineAuftraege.bestand(wkingSpreadsheet, mapping, spieltagNr).erzeuge(AuftragsArt.ANMELDUNG_STORNIEREN,
                "PUT", TournamentSyncClient.anmeldungPfad(turnierId, uuidA), body.toString(), "{}");
        abgleichen();

        assertThat(konfliktArten()).as("Auftrag abgelehnt, in der Konfliktliste (P-80 a)")
                .contains(KonfliktArt.AUFTRAG_ABGELEHNT);
        assertThat(onlineStatus(idA)).as("Status unverändert").isEqualTo("confirmed");
        assertThat(lokaleMeldungen().values()).containsExactly(namen(TEAM_A));
    }

    @Test
    void ablehnungEinerImportiertenMeldungVorDemCheckInEntferntSie() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        online.anmelden(turnierId, TEAM_B);
        verbinden();
        abgleichen();

        online.statusSetzen(turnierId, idA, "pending");
        online.ablehnen(idA, GRUND);
        abgleichen();

        assertThat(lokaleMeldungen().values()).as("abgelehnt und nicht eingecheckt: entfernt (P-80 c, wie P-74)")
                .containsExactly(namen(TEAM_B));
        assertThat(mapping.istBereitsImportiert(idA)).as("Zuordnung entfernt").isFalse();
        assertThat(konfliktArten()).doesNotContain(KonfliktArt.ONLINE_AUSGESCHLOSSEN);
    }

    @Test
    void ablehnungEinerEingechecktenMeldungSchliesstSieVonDerAuslosungAus() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String idA = online.anmelden(turnierId, TEAM_A);
        online.anmelden(turnierId, TEAM_B);
        // Schweizer lost erst ab sechs Meldungen aus; ohne die abgelehnte bleiben sieben.
        Map<String, Person[]> weitere = vollstaendigeTeams(6);
        verbinden();
        abgleichen();
        String uuidA = mapping.getLokaleUuid(idA).orElseThrow();
        int nrA = ziel.getTeamNrAusZeile(zeile(TEAM_A[0]));
        einchecken(zeile(TEAM_A[0]));
        einchecken(zeile(TEAM_B[0]));
        for (Person[] team : weitere.values()) {
            einchecken(zeile(team[0]));
        }

        online.statusSetzen(turnierId, idA, "pending");
        online.ablehnen(idA, GRUND);
        abgleichen();

        assertThat(lokaleMeldungen().values()).as("Abgleich löscht nichts (P-80 c, wie P-37)").contains(namen(TEAM_A));
        assertThat(ziel.getAktivWertAusZeile(zeile(TEAM_A[0]))).as("online abgelehnt → abgemeldet").isEqualTo(2);
        assertThat(mapping.getRohStatusProUuid().get(uuidA)).as("Online-Status im Sync-Blatt").isEqualTo("rejected");
        assertThat(konfliktArten()).contains(KonfliktArt.ONLINE_AUSGESCHLOSSEN);
        assertThat(schweizerRundeAuslosen()).as("nicht ausgelost").isNotEmpty().doesNotContain(nrA);
    }

    /** Turnier mit Freigabe durch die Turnierleitung und Teamnamen: neue Anmeldungen bleiben offen. */
    private void turnierMitFreigabeAnlegen() throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("approvalRequired", true);
        einstellungen.addProperty("teamNameEnabled", true);
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT, einstellungen);
    }

    private String onlineStatus(String anmeldungId) throws Exception {
        return online.anmeldung(turnierId, anmeldungId).orElseThrow().get("status").getAsString();
    }

    /** Grund einer Ablehnungs-Nachricht dieses Turniers, sonst {@code null}. */
    private String ablehnungsGrund(JsonObject nachricht) {
        if (nachricht.get("eventData") == null || !nachricht.get("eventData").isJsonObject()) {
            return null;
        }
        JsonObject daten = nachricht.getAsJsonObject("eventData");
        boolean passend = "registration_status_changed".equals(text(nachricht, "eventType"))
                && turnierId.equals(text(daten, "tournamentId")) && "rejected".equals(text(daten, "status"));
        return passend ? text(daten, "reason") : null;
    }

    private static String text(JsonObject objekt, String feld) {
        return objekt.has(feld) && !objekt.get(feld).isJsonNull() ? objekt.get(feld).getAsString() : null;
    }
}
