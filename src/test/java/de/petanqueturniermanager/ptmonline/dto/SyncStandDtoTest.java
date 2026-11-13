/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.dto;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SyncStandDtoTest {

    private static final SyncStandDto ENTWURF = new SyncStandDto("draft", false, null, 0, 7, "2026-11-15", true);

    @Test
    void gemeldeterNeuerStatusErsetztNurDenStatus() {
        assertThat(ENTWURF.mitGemeldetemStatus("registration")).hasValueSatisfying(neu -> {
            assertThat(neu.status()).isEqualTo("registration");
            assertThat(neu).usingRecursiveComparison().ignoringFields("status").isEqualTo(ENTWURF);
        });
    }

    @Test
    void unveraenderterOderFehlenderStatusLiefertNichts() {
        assertThat(ENTWURF.mitGemeldetemStatus("draft")).isEmpty();
        assertThat(ENTWURF.mitGemeldetemStatus(null)).isEmpty();
        assertThat(ENTWURF.mitGemeldetemStatus(" ")).isEmpty();
    }
}
