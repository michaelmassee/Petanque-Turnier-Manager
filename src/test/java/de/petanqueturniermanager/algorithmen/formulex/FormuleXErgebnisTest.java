package de.petanqueturniermanager.algorithmen.formulex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class FormuleXErgebnisTest {

    @Test
    public void istSieger_trueBeiFreilos() {
        var bye = new FormuleXErgebnis(1, 0, 0, List.of(), true);
        assertThat(bye.istSieger()).isTrue();
    }

    @Test
    public void istSieger_trueWennEigenePunkteGroesser() {
        var sieg = new FormuleXErgebnis(1, 13, 7, List.of(2), false);
        assertThat(sieg.istSieger()).isTrue();
    }

    @Test
    public void istSieger_falseBeiNiederlage() {
        var nieder = new FormuleXErgebnis(1, 7, 13, List.of(2), false);
        assertThat(nieder.istSieger()).isFalse();
    }

    @Test
    public void istSieger_falseBeiPunktegleichstandOhneFreilos() {
        // Edge-Case: gleicher Stand zählt nicht als Sieg
        var unentschieden = new FormuleXErgebnis(1, 10, 10, List.of(2), false);
        assertThat(unentschieden.istSieger()).isFalse();
    }

    @Test
    public void punktedifferenz_kannNegativSein() {
        assertThat(new FormuleXErgebnis(1, 13, 7, List.of(), false).punktedifferenz()).isEqualTo(6);
        assertThat(new FormuleXErgebnis(1, 7, 13, List.of(), false).punktedifferenz()).isEqualTo(-6);
        assertThat(new FormuleXErgebnis(1, 0, 0, List.of(), true).punktedifferenz()).isZero();
    }
}
