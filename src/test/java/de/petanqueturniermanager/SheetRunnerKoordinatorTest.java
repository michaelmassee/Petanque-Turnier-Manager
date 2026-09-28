/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

/**
 * Konsistentes Lesen außerhalb eines SheetRunner-Laufs ({@link SheetRunnerKoordinator#ruhenderLaufStand()} /
 * {@link SheetRunnerKoordinator#unveraendertSeit(long)}): ein Hintergrund-Leser muss jeden Lauf erkennen, der
 * während seines Lesens aktiv war – auch einen, der vor dem Ende des Lesens schon wieder beendet ist.
 */
class SheetRunnerKoordinatorTest {

    private final SheetRunnerKoordinator koordinator = new SheetRunnerKoordinator();

    @Test
    void ohneLaufBleibtStandUnveraendert() {
        long stand = koordinator.ruhenderLaufStand().orElseThrow();

        assertThat(koordinator.unveraendertSeit(stand)).isTrue();
    }

    @Test
    void waehrendEinesLaufsGibtEsKeinenRuhendenStand() {
        koordinator.getAndSetLaeuft(true);

        assertThat(koordinator.ruhenderLaufStand()).isEqualTo(OptionalLong.empty());
    }

    @Test
    void laufBeginntWaehrendDesLesens() {
        long stand = koordinator.ruhenderLaufStand().orElseThrow();

        koordinator.getAndSetLaeuft(true);

        assertThat(koordinator.unveraendertSeit(stand)).isFalse();
    }

    @Test
    void laufBeginntUndEndetWaehrendDesLesens() {
        long stand = koordinator.ruhenderLaufStand().orElseThrow();

        koordinator.getAndSetLaeuft(true);
        koordinator.setLaeuft(false);

        assertThat(koordinator.unveraendertSeit(stand)).isFalse();
    }

    @Test
    void erneutesBeendenZaehltNichtAlsNeuerLauf() {
        koordinator.getAndSetLaeuft(true);
        koordinator.setLaeuft(false);
        long stand = koordinator.ruhenderLaufStand().orElseThrow();

        koordinator.setLaeuft(false);

        assertThat(koordinator.unveraendertSeit(stand)).isTrue();
    }
}
