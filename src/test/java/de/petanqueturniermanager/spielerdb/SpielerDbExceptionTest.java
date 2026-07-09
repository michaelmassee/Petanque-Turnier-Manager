package de.petanqueturniermanager.spielerdb;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpielerDbExceptionTest {

    @Test
    void nurMessage_istKeinLockFehler() {
        SpielerDbException e = new SpielerDbException("Fehler");
        assertThat(e.getMessage()).isEqualTo("Fehler");
        assertThat(e.istLockFehler()).isFalse();
    }

    @Test
    void messageUndCause_istKeinLockFehler() {
        Throwable cause = new RuntimeException("Ursache");
        SpielerDbException e = new SpielerDbException("Fehler", cause);
        assertThat(e.getCause()).isSameAs(cause);
        assertThat(e.istLockFehler()).isFalse();
    }

    @Test
    void mitLockFehlerFlag_wirdUebernommen() {
        Throwable cause = new RuntimeException("Ursache");
        SpielerDbException e = new SpielerDbException("Fehler", cause, true);
        assertThat(e.istLockFehler()).isTrue();
    }
}
