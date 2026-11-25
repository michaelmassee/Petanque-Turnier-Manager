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
 * Wiederherstellung der Zuordnung vom Server (T-21): Der zuletzt abgeglichene Teamname kommt aus den
 * Online-Anmeldungen, sonst löschte die nächste Änderung einer Meldeliste ohne Teamname-Spalte ihn online.
 */
class PtmOnlineZuordnungWiederherstellungTest {

    private static final Gson GSON = new Gson();
    private static final String ONLINE_ID = "r1";
    private static final String LOKALE_UUID = "11111111-2222-4333-8444-555555555555";

    @Test
    void wiederhergestellteZuordnungKenntDenOnlineTeamnamen() {
        Map<String, String> teamnamen = PtmOnlineZuordnungWiederherstellung
                .teamnamenProOnlineId(List.of(anmeldung(ONLINE_ID, "Boule-Asse")));

        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(), teamnamen);

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).contains("Boule-Asse");
        assertThat(zuordnung.onlineId()).isEqualTo(ONLINE_ID);
        assertThat(zuordnung.lokaleUuid()).isEqualTo(LOKALE_UUID);
    }

    @Test
    void anmeldungOhneTeamnamenGiltAlsOhneTeamnamenAbgeglichen() {
        Map<String, String> teamnamen = PtmOnlineZuordnungWiederherstellung
                .teamnamenProOnlineId(List.of(anmeldung(ONLINE_ID, null)));

        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(), teamnamen);

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).contains("");
    }

    @Test
    void fehlendeAnmeldungLaesstDenTeamnamenUnbekannt() {
        NeueZuordnung zuordnung = PtmOnlineZuordnungWiederherstellung.zuordnung(serverZuordnung(), Map.of());

        assertThat(AbgeglichenerTeamname.lese(zuordnung.teamname())).isEmpty();
    }

    private static ServerZuordnungDto serverZuordnung() {
        return GSON.fromJson("""
                {"onlineRegistrationId": "%s", "localRegistrationUuid": "%s", "status": "confirmed",
                 "firstName": "Anna", "lastName": "Adler", "partnerFirstName": "Ben", "partnerLastName": "Berg",
                 "executionRevision": 2, "persons": [], "club": null}
                """.formatted(ONLINE_ID, LOKALE_UUID), ServerZuordnungDto.class);
    }

    private static RegistrationDto anmeldung(String id, @Nullable String teamname) {
        return GSON.fromJson(teamname == null ? "{\"id\": \"%s\"}".formatted(id)
                : "{\"id\": \"%s\", \"teamName\": \"%s\"}".formatted(id, teamname), RegistrationDto.class);
    }
}
