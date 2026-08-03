package de.petanqueturniermanager.algorithmen.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class DurchgangAufteilungRechnerTest {

    @Test
    public void testBahnenGroesserAlsPaarungen_einBlock() {
        assertThat(DurchgangAufteilungRechner.berechne(5, 8)).containsExactly(5);
    }

    @Test
    public void testBahnenGleichPaarungen_einBlock() {
        assertThat(DurchgangAufteilungRechner.berechne(8, 8)).containsExactly(8);
    }

    @Test
    public void testExakteTeilbarkeit_zweiBloecke() {
        assertThat(DurchgangAufteilungRechner.berechne(16, 8)).containsExactly(8, 8);
    }

    @Test
    public void testRestBlockKleinerAlsBahnen() {
        // 17 Paarungen, 8 Bahnen -> [8, 8, 1], kein Falten (anders als GruppenAufteilungRechner)
        assertThat(DurchgangAufteilungRechner.berechne(17, 8)).containsExactly(8, 8, 1);
    }

    @Test
    public void testEinePaarung() {
        assertThat(DurchgangAufteilungRechner.berechne(1, 8)).containsExactly(1);
    }

    @Test
    public void testIllegalArg_anzahlPaarungenNull() {
        assertThatThrownBy(() -> DurchgangAufteilungRechner.berechne(0, 8))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzahlPaarungenNegativ() {
        assertThatThrownBy(() -> DurchgangAufteilungRechner.berechne(-1, 8))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzahlBahnenNull() {
        assertThatThrownBy(() -> DurchgangAufteilungRechner.berechne(16, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzahlBahnenNegativ() {
        assertThatThrownBy(() -> DurchgangAufteilungRechner.berechne(16, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
