package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.spielerdb.MeldelisteZiel.MeldelisteSchreibException;

class MeldelisteZielTest {

    @Test
    void meldelisteSchreibException_nurMessage() {
        MeldelisteSchreibException e = new MeldelisteSchreibException("Liste voll");
        assertThat(e.getMessage()).isEqualTo("Liste voll");
        assertThat(e.getCause()).isNull();
    }

    @Test
    void meldelisteSchreibException_mitCause() {
        Throwable cause = new RuntimeException("Ursache");
        MeldelisteSchreibException e = new MeldelisteSchreibException("Sheet-Fehler", cause);
        assertThat(e.getMessage()).isEqualTo("Sheet-Fehler");
        assertThat(e.getCause()).isSameAs(cause);
    }
}
