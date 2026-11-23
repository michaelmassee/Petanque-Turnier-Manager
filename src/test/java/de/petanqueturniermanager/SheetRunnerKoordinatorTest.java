/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.OptionalLong;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    @Test
    void anwenderLaufWartetAufStillenHintergrundlaufUndHatVorrang() throws Exception {
        assertThat(koordinator.belege(true)).isTrue();
        assertThat(koordinator.istStillBelegt()).isTrue();

        CompletableFuture<Boolean> wartend = CompletableFuture
                .supplyAsync(() -> {
                    try {
                        return koordinator.warteUndBelege(5_000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                });
        TimeUnit.MILLISECONDS.sleep(150);
        koordinator.setLaeuft(false);

        assertThat(wartend.get(5, TimeUnit.SECONDS)).isTrue();
        assertThat(koordinator.istStillBelegt()).as("jetzt läuft der Anwender-Lauf").isFalse();
        assertThat(koordinator.belege(true)).isFalse();
    }

    @Test
    void anwenderLaufWartetNichtAufAnderenAnwenderLauf() throws Exception {
        assertThat(koordinator.belege(false)).isTrue();

        assertThat(koordinator.istStillBelegt()).isFalse();
        assertThat(koordinator.warteUndBelege(5_000)).isFalse();
    }

    @Test
    void belegeFuerAnwenderMeldetHalterDesKoordinators() {
        assertThat(koordinator.belege(true)).isTrue();
        assertThat(koordinator.belegeFuerAnwender()).isEqualTo(SheetRunnerKoordinator.Belegung.HINTERGRUNDLAUF_AKTIV);

        koordinator.setLaeuft(false);
        assertThat(koordinator.belegeFuerAnwender()).isEqualTo(SheetRunnerKoordinator.Belegung.BELEGT);
        assertThat(koordinator.belegeFuerAnwender()).isEqualTo(SheetRunnerKoordinator.Belegung.BESETZT);
    }

    @Test
    void anwenderLaufBelegtAuchBeiSofortEndendemHintergrundlauf() throws Exception {
        assertThat(koordinator.belege(true)).isTrue();
        koordinator.setLaeuft(false);

        assertThat(koordinator.warteUndBelege(5_000)).as("freier Koordinator darf nicht als besetzt gelten").isTrue();
    }
}
