package de.petanqueturniermanager.toolbar;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

class TurnierModusAutomatikTest {

    @Test
    void aktiviertTurnierdokumentOhneTurniermodus() {
        assertThat(TurnierModus.sollAutomatischAktivieren(true, TurnierSystem.SCHWEIZER, false)).isTrue();
    }

    @Test
    void nichtWennOptionAusgeschaltet() {
        assertThat(TurnierModus.sollAutomatischAktivieren(false, TurnierSystem.SCHWEIZER, false)).isFalse();
    }

    @Test
    void nichtFuerDokumentOhneTurniersystem() {
        assertThat(TurnierModus.sollAutomatischAktivieren(true, TurnierSystem.KEIN, false)).isFalse();
    }

    @Test
    void nichtWennBereitsAktiv() {
        assertThat(TurnierModus.sollAutomatischAktivieren(true, TurnierSystem.SCHWEIZER, true)).isFalse();
    }
}
