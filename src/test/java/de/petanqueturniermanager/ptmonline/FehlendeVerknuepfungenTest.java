/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.Gson;

import de.petanqueturniermanager.ptmonline.dto.RegistrationDto;

/** Welche Zuordnungen PTM-Online noch verknüpft werden müssen (T-21). */
class FehlendeVerknuepfungenTest {

    private static final Gson GSON = new Gson();

    private static RegistrationDto online(String id, String lokaleUuid) {
        String uuid = lokaleUuid == null ? "null" : "\"" + lokaleUuid + "\"";
        return GSON.fromJson("{\"id\":\"" + id + "\",\"localRegistrationUuid\":" + uuid + "}", RegistrationDto.class);
    }

    @Test
    void importierteAnmeldungOhneLokaleUuidWirdVerknuepft() {
        Map<String, String> fehlend = RegistrationImportTask.fehlendeVerknuepfungen(Map.of("u1", "r1", "u2", "r2"),
                List.of(online("r1", null), online("r2", "u2")));

        assertThat(fehlend).isEqualTo(Map.of("u1", "r1"));
    }

    @Test
    void abweichendeLokaleUuidWirdUeberschrieben() {
        assertThat(RegistrationImportTask.fehlendeVerknuepfungen(Map.of("u1", "r1"), List.of(online("r1", "alt"))))
                .isEqualTo(Map.of("u1", "r1"));
    }

    @Test
    void uuidAnEinerAnderenAnmeldungUndOnlineGeloeschteEntfallen() {
        Map<String, String> fehlend = RegistrationImportTask.fehlendeVerknuepfungen(Map.of("u1", "r1", "u9", "r9"),
                List.of(online("r1", null), online("r2", "u1")));

        assertThat(fehlend).as("u1 trägt schon r2, r9 gibt es online nicht").isEmpty();
    }
}
