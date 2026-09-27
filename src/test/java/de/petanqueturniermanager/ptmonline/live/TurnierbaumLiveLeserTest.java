/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline.live;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.helper.position.Position;

class TurnierbaumLiveLeserTest {

    @Test
    void liestScorePositionenInGespeicherterReihenfolge() {
        assertThat(TurnierbaumLiveLeser.scorePositionen("PTM_EDIT:3,2|3,4| 7 , 3 |x,1|9"))
                .containsExactly(Position.from(3, 2), Position.from(3, 4), Position.from(7, 3));
    }

    @Test
    void ohneScoreDatenKeinePositionen() {
        assertThat(TurnierbaumLiveLeser.scorePositionen(null)).isEmpty();
        assertThat(TurnierbaumLiveLeser.scorePositionen("etwas anderes")).isEmpty();
    }
}
