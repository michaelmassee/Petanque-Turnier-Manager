package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class SpielerAddInCacheTest {

    @Test
    void holeOderBerechne_berechnetNurEinmalInnerhalbTtl() {
        SpielerAddInCache cache = new SpielerAddInCache();
        AtomicInteger aufrufe = new AtomicInteger();

        String erstesErgebnis = cache.holeOderBerechne("key", () -> {
            aufrufe.incrementAndGet();
            return "Wert" + aufrufe.get();
        });
        String zweitesErgebnis = cache.holeOderBerechne("key", () -> {
            aufrufe.incrementAndGet();
            return "Wert" + aufrufe.get();
        });

        assertThat(erstesErgebnis).isEqualTo("Wert1");
        assertThat(zweitesErgebnis).isEqualTo("Wert1");
        assertThat(aufrufe.get()).isEqualTo(1);
    }

    @Test
    void holeOderBerechne_unterschiedlicheKeysUnabhaengig() {
        SpielerAddInCache cache = new SpielerAddInCache();
        assertThat(cache.holeOderBerechne("a", () -> "A")).isEqualTo("A");
        assertThat(cache.holeOderBerechne("b", () -> "B")).isEqualTo("B");
    }

    @Test
    void leeren_erzwingtNeuberechnung() {
        SpielerAddInCache cache = new SpielerAddInCache();
        AtomicInteger aufrufe = new AtomicInteger();

        cache.holeOderBerechne("key", () -> "Wert" + aufrufe.incrementAndGet());
        cache.leeren();
        cache.holeOderBerechne("key", () -> "Wert" + aufrufe.incrementAndGet());

        assertThat(aufrufe.get()).isEqualTo(2);
    }
}
