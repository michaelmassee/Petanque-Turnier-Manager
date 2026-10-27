package de.petanqueturniermanager.algorithmen.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

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

    @Test
    public void testGleichmaessig_falseDelegiertAnChunkAufteilung() {
        assertThat(DurchgangAufteilungRechner.berechne(17, 8, false)).containsExactly(8, 8, 1);
    }

    @Test
    public void testGleichmaessig_restWirdVerteiltStattImLetztenBlock() {
        // 13 Paarungen (z.B. 26 Teams), 4 Bahnen -> [4, 3, 3, 3] statt [4, 4, 4, 1]
        assertThat(DurchgangAufteilungRechner.berechne(13, 4, true)).containsExactly(4, 3, 3, 3);
    }

    @Test
    public void testGleichmaessig_restBlockKleinerAlsBahnenWirdVerteilt() {
        // 17 Paarungen, 8 Bahnen -> ceil(17/8)=3 Bloecke, gleichmaessig verteilt: [6, 6, 5]
        assertThat(DurchgangAufteilungRechner.berechne(17, 8, true)).containsExactly(6, 6, 5);
    }

    @Test
    public void testGleichmaessig_exakteTeilbarkeitUnveraendert() {
        assertThat(DurchgangAufteilungRechner.berechne(16, 8, true)).containsExactly(8, 8);
    }

    @Test
    public void testGleichmaessig_einBlockWennBahnenGroesserAlsPaarungen() {
        assertThat(DurchgangAufteilungRechner.berechne(5, 8, true)).containsExactly(5);
    }

    @Test
    public void testGleichmaessig_illegalArgUnveraendert() {
        assertThatThrownBy(() -> DurchgangAufteilungRechner.berechne(0, 8, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> DurchgangAufteilungRechner.berechne(16, 0, true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testZeilen_freilosErzwingtKeinenZusaetzlichenDurchgang() {
        // 8 Spiele + Freilos am Ende, 8 Bahnen: ein Durchgang mit allen 9 Zeilen
        assertThat(DurchgangAufteilungRechner.berechneZeilen(9, Set.of(8), 8, false)).containsExactly(9);
    }

    @Test
    public void testZeilen_freilosWirdLetztemDurchgangZugeschlagen() {
        // 16 Spiele + Freilos, 8 Bahnen: Freilos-Zeile haengt am zweiten Durchgang
        assertThat(DurchgangAufteilungRechner.berechneZeilen(17, Set.of(16), 8, false)).containsExactly(8, 9);
    }

    @Test
    public void testZeilen_gleichmaessigOhneFreilosInDerZaehlung() {
        // 13 Spiele + Freilos, 4 Bahnen gleichmaessig: Spiele [4, 3, 3, 3], Freilos im letzten Block
        assertThat(DurchgangAufteilungRechner.berechneZeilen(14, Set.of(13), 4, true)).containsExactly(4, 3, 3, 4);
    }

    @Test
    public void testZeilen_ohneFreilosWieBerechne() {
        assertThat(DurchgangAufteilungRechner.berechneZeilen(17, Set.of(), 8, false)).containsExactly(8, 8, 1);
    }

    @Test
    public void testZeilen_nurFreilosEinBlock() {
        assertThat(DurchgangAufteilungRechner.berechneZeilen(1, Set.of(0), 8, false)).containsExactly(1);
    }
}
