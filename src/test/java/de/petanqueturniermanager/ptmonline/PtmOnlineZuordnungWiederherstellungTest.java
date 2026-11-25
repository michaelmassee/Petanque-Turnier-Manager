/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import de.petanqueturniermanager.onlinesync.speicher.NeueZuordnung;
import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;
import de.petanqueturniermanager.ptmonline.dto.ServerZuordnungDto;

/**
 * Wiederherstellung der Zuordnung vom Server (T-21): Der zuletzt abgeglichene Teamname kommt aus dem Mapping, bei
 * einem älteren Server ohne das Feld aus den Online-Anmeldungen. Sonst löschte die nächste Änderung einer Meldeliste
 * ohne Teamname-Spalte ihn online.
 */
class PtmOnlineZuordnungWiederherstellungTest {

    private static final Gson GSON = new Gson();
    private static final String ONLINE_ID = "r1";
    private static final String LOKALE_UUID = "11111111-2222-4333-8444-555555555555";

    @Test
    void teamnameKommtAusDemMapping() {
        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung("Boule-Asse"),
                Map.of());

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).contains("Boule-Asse");
    }

    @Test
    void mappingOhneTeamnamenGiltAlsOhneTeamnamenAbgeglichen() {
        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(""), Map.of());

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).contains("");
    }

    @Test
    void aeltererServerOhneTeamnamenNimmtIhnAusDenAnmeldungen() {
        Map<String, String> teamnamen = PtmOnlineZuordnungWiederherstellung
                .teamnamenProOnlineId(List.of(anmeldung(ONLINE_ID, "Boule-Asse")));

        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(null), teamnamen);

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).contains("Boule-Asse");
        assertThat(zuordnung.onlineId()).isEqualTo(ONLINE_ID);
        assertThat(zuordnung.lokaleUuid()).isEqualTo(LOKALE_UUID);
    }

    @Test
    void aeltererServerUndAnmeldungOhneTeamnamenGiltAlsOhneTeamnamenAbgeglichen() {
        Map<String, String> teamnamen = PtmOnlineZuordnungWiederherstellung
                .teamnamenProOnlineId(List.of(anmeldung(ONLINE_ID, null)));

        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(null), teamnamen);

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).contains("");
    }

    @Test
    void aeltererServerUndFehlendeAnmeldungLassenDenTeamnamenUnbekannt() {
        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(null), Map.of());

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).isEmpty();
    }

    /** @param teamname {@code null}: Feld fehlt wie bei einem älteren Server */
    private static ServerZuordnungDto serverZuordnung(@Nullable String teamname) {
        String teamnameFeld = teamname == null ? "" : ", \"teamName\": \"%s\"".formatted(teamname);
        return GSON.fromJson("""
                {"onlineRegistrationId": "%s", "localRegistrationUuid": "%s", "status": "confirmed",
                 "firstName": "Anna", "lastName": "Adler", "partnerFirstName": "Ben", "partnerLastName": "Berg",
                 "executionRevision": 2, "persons": [], "club": null%s}
                """.formatted(ONLINE_ID, LOKALE_UUID, teamnameFeld), ServerZuordnungDto.class);
    }

    private static RegistrationDto anmeldung(String id, @Nullable String teamname) {
        return GSON.fromJson(teamname == null ? "{\"id\": \"%s\"}".formatted(id)
                : "{\"id\": \"%s\", \"teamName\": \"%s\"}".formatted(id, teamname), RegistrationDto.class);
    }
}
