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
    void erkenntDenAbschluss() {
        assertThat(ENTWURF.istAbgeschlossen()).isFalse();
        assertThat(ENTWURF.mitGemeldetemStatus("finished")).hasValueSatisfying(stand -> {
            assertThat(stand.istAbgeschlossen()).isTrue();
            assertThat(stand.istRunning()).isFalse();
        });
    }

    @Test
    void unveraenderterOderFehlenderStatusLiefertNichts() {
        assertThat(ENTWURF.mitGemeldetemStatus("draft")).isEmpty();
        assertThat(ENTWURF.mitGemeldetemStatus(null)).isEmpty();
        assertThat(ENTWURF.mitGemeldetemStatus(" ")).isEmpty();
    }

    @Test
    void erkenntDenZurueckgesetztenStart() {
        String zurueckgesetzt = "2026-11-16T10:00:00Z";
        assertThat(stand("registration", zurueckgesetzt).istStartZurueckgesetzt()).isTrue();
        assertThat(stand("running", zurueckgesetzt).istStartZurueckgesetzt())
                .as("nach dem Zurücksetzen erneut gestartet").isFalse();
        assertThat(stand("registration", null).istStartZurueckgesetzt()).isFalse();
        assertThat(stand("registration", "").istStartZurueckgesetzt()).isFalse();
    }

    private static SyncStandDto stand(String status, String runningResetAt) {
        return new SyncStandDto(status, true, runningResetAt, 1, 0, null, false);
    }
}
