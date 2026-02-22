package de.petanqueturniermanager.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class SpielErgebnisTest {

    @Test
    public void testSiegAWennAGroesserB() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(13, 7);
        assertThat(ergebnis.siegA()).isTrue();
    }

    @Test
    public void testSiegAFalschWennBGroesser() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(5, 13);
        assertThat(ergebnis.siegA()).isFalse();
    }

    @Test
    public void testSiegAFalschBeiUnentschieden() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(10, 10);
        assertThat(ergebnis.siegA()).isFalse();
    }

    @Test
    public void testSiegBWennBGroesserA() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(3, 13);
        assertThat(ergebnis.siegB()).isTrue();
    }

    @Test
    public void testSiegBFalschWennAGroesser() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(13, 3);
        assertThat(ergebnis.siegB()).isFalse();
    }

    @Test
    public void testSiegBFalschBeiUnentschieden() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(8, 8);
        assertThat(ergebnis.siegB()).isFalse();
    }

    @Test
    public void testUnentschiedenKeinerGewinnt() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(6, 6);
        assertThat(ergebnis.siegA()).isFalse();
        assertThat(ergebnis.siegB()).isFalse();
    }

    @Test
    public void testGetSpielPunkteA() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(11, 5);
        assertThat(ergebnis.getSpielPunkteA()).isEqualTo(11);
    }

    @Test
    public void testGetSpielPunkteB() throws Exception {
        SpielErgebnis ergebnis = new SpielErgebnis(11, 5);
        assertThat(ergebnis.getSpielPunkteB()).isEqualTo(5);
    }

    @Test
    public void testNullPunkteErlaubt() throws Exception {
        // Beide 0 ist erlaubt (z.B. noch kein Ergebnis eingetragen)
        SpielErgebnis ergebnis = new SpielErgebnis(0, 0);
        assertThat(ergebnis.siegA()).isFalse();
        assertThat(ergebnis.siegB()).isFalse();
    }

    @Test
    public void testNegativePunkteANichtErlaubt() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new SpielErgebnis(-1, 5));
    }

    @Test
    public void testNegativePunkteBNichtErlaubt() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new SpielErgebnis(5, -1));
    }

}
