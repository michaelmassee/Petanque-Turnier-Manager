/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.HttpURLConnection;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.ptmonline.PtmOnlineWebApi.Person;
import de.petanqueturniermanager.ptmonline.ui.PtmOnlineLiveQrTestZugang;

/**
 * „QR-Code Live-Ansicht öffnen“ gegen den lokal gestarteten Worker: Aus der Zeile einer Meldung im verbundenen
 * Dokument holt PTM über die Sync-API den persönlichen Live-Link des Teams. Der Link öffnet ohne Anmeldung die
 * Live-Ansicht genau dieser Meldung; gibt es ihn nicht, nennt PTM-Online den Grund maschinenlesbar.
 */
class PtmOnlineLiveQrZusammenspielTest extends BasePtmOnlineZusammenspielTest {

    private static final LocalDate IN_EINEM_MONAT = LocalDate.now().plusMonths(1);
    private static final String LIVE_PFAD = "/live/t/";
    private static final Person[] TEAM = { Person.gast("Quentin", "Qr"), Person.gast("Quirin", "Qr"),
            Person.gast("Quinn", "Qr") };

    private TournamentSyncClient client;

    @BeforeEach
    void clientAnlegen() {
        client = new TournamentSyncClient(server.baseUrl(), server.apiKey());
    }

    @Test
    void liveLinkDerMeldungszeileOeffnetDieLiveAnsichtDiesesTeams() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String anmeldungId = online.anmelden(turnierId, TEAM);
        verbinden();
        abgleichen();

        String onlineId = PtmOnlineLiveQrTestZugang.onlineIdDerZeile(ziel, zeile(TEAM[0])).orElseThrow();
        assertThat(onlineId).as("Online-ID aus der Meldungszeile").isEqualTo(anmeldungId);

        // Derselbe Link, den die Anmeldebestätigung verschickt hat: PTM-Online speichert ihn verschlüsselt.
        String url = client.holeLiveLink(turnierId, onlineId, false);
        assertThat(url).as("persönlicher Live-Link").startsWith(server.baseUrl() + LIVE_PFAD);
        JsonObject live = online.liveUeberToken(token(url));
        assertThat(live.getAsJsonObject("registration").get("id").getAsString()).as("Live-Ansicht dieser Meldung")
                .isEqualTo(anmeldungId);
        assertThat(client.holeLiveLink(turnierId, onlineId, false)).as("erneuter Abruf ändert den Link nicht")
                .isEqualTo(url);
    }

    @Test
    void lokaleMeldungOhneOnlineZuordnungHatKeinenLiveLink() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        int zeile = lokaleMeldung(TEAM);
        verbinden();

        assertThat(PtmOnlineLiveQrTestZugang.onlineIdDerZeile(ziel, zeile)).as("vor dem Abgleich nicht zugeordnet")
                .isEmpty();
    }

    @Test
    void ohneFreigeschalteteLiveAnsichtNenntPtmOnlineDenGrund() throws Exception {
        JsonObject einstellungen = new JsonObject();
        einstellungen.addProperty("liveViewEnabled", false);
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT, einstellungen);
        String anmeldungId = online.anmelden(turnierId, TEAM);
        verbinden();
        abgleichen();

        assertThatThrownBy(() -> client.holeLiveLink(turnierId, anmeldungId, false))
                .isInstanceOfSatisfying(PtmOnlineHttpException.class,
                        e -> assertThat(e.istLiveAnsichtAus()).as("Live-Ansicht aus").isTrue());
    }

    @Test
    void nichtBestaetigteMeldungHatKeinenLiveLink() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        String anmeldungId = online.anmelden(turnierId, TEAM);
        verbinden();
        abgleichen();

        online.statusSetzen(turnierId, anmeldungId, "waitlist");

        assertThatThrownBy(() -> client.holeLiveLink(turnierId, anmeldungId, false))
                .isInstanceOfSatisfying(PtmOnlineHttpException.class,
                        e -> assertThat(e.istMeldungNichtBestaetigt()).as("nicht bestätigt").isTrue());
    }

    @Test
    void meldungEinesAnderenTurniersIstNichtErreichbar() throws Exception {
        turnierAnlegen(Formation.TRIPLETTE, IN_EINEM_MONAT);
        verbinden();
        String anderesTurnier = online.turnierAnlegen("E2E anderes Turnier", "schweizer", "forme", "triplette",
                IN_EINEM_MONAT);
        String fremdeAnmeldung = online.anmelden(anderesTurnier, TEAM);

        assertThatThrownBy(() -> client.holeLiveLink(turnierId, fremdeAnmeldung, false))
                .isInstanceOfSatisfying(PtmOnlineHttpException.class, e -> assertThat(e.getStatusCode())
                        .isEqualTo(HttpURLConnection.HTTP_NOT_FOUND));
    }

    private static String token(String url) {
        return URLDecoder.decode(url.substring(url.indexOf(LIVE_PFAD) + LIVE_PFAD.length()), StandardCharsets.UTF_8);
    }
}
