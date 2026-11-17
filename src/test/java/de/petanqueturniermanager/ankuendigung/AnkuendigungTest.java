/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ankuendigung;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnkuendigungTest {

    @Test
    void vereinheitlichtZeilenumbruecheUndEntferntSteuerzeichen() {
        assertThat(Ankuendigung.bereinige("  Runde 3\r\nstartet\rum 14:30\u0007\t "))
                .isEqualTo("Runde 3\nstartet\num 14:30");
    }

    @Test
    void leererOderFehlenderTextGiltAlsGeloescht() {
        assertThat(Ankuendigung.bereinige(null)).isEmpty();
        assertThat(Ankuendigung.bereinige(" \n ")).isEmpty();
    }

    @Test
    void kuerztAufHoechstlaengeWiePtmOnline() {
        String lang = "x".repeat(Ankuendigung.MAX_LAENGE + 20);

        assertThat(Ankuendigung.bereinige(lang)).hasSize(Ankuendigung.MAX_LAENGE);
    }

    @Test
    void laesstUmlauteUndEmojisUnveraendert() {
        assertThat(Ankuendigung.bereinige("Siegerehrung 🏆 im Vereinsheim – Grüße"))
                .isEqualTo("Siegerehrung 🏆 im Vereinsheim – Grüße");
    }
}
