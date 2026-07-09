package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class SpielerMitVereinTest {

    @Test
    void spielernameVollstaendig_verbindetVorUndNachname() {
        SpielerMitVerein s = new SpielerMitVerein(1, "Max", "Mustermann",
                null, null, null, null, null);
        assertThat(s.spielernameVollstaendig()).isEqualTo("Max Mustermann");
    }

    @Test
    void nullListen_werdenZuLeererListe() {
        SpielerMitVerein s = new SpielerMitVerein(1, "Max", "Mustermann",
                null, null, null, null, null);
        assertThat(s.labelNrs()).isEmpty();
        assertThat(s.labelNamen()).isEmpty();
    }

    @Test
    void listen_werdenAlsUnveraenderlicheKopieUebernommen() {
        SpielerMitVerein s = new SpielerMitVerein(1, "Max", "Mustermann",
                5, "BC Linden", List.of(1, 2), List.of("Anfänger", "Fortgeschritten"), "L-1");
        assertThat(s.labelNrs()).containsExactly(1, 2);
        assertThat(s.labelNamen()).containsExactly("Anfänger", "Fortgeschritten");
    }
}
