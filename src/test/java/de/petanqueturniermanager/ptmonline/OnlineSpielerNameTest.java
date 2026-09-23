/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ptmonline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class OnlineSpielerNameTest {

    @Test
    void ignoriertAkzenteSatzzeichenUndGrossschreibungWieOnline() {
        assertThat(OnlineSpielerName.schluessel("Jean-Paul", "Müller"))
                .isEqualTo(OnlineSpielerName.schluessel("jean paul", "muller"))
                .isEqualTo("jeanpaulmuller");
    }

    @Test
    void ignoriertLeerzeichenAmRand() {
        assertThat(OnlineSpielerName.schluessel(" Anna ", "Schmidt ")).isEqualTo("annaschmidt");
    }

    @Test
    void unterscheidetVerschiedeneNamen() {
        assertThat(OnlineSpielerName.schluessel("Hans", "Müller"))
                .isNotEqualTo(OnlineSpielerName.schluessel("Hans", "Müller jun."));
    }

    @Test
    void nichtLateinischeNamenFallenNichtAufLeerenSchluesselZusammen() {
        assertThat(OnlineSpielerName.schluessel("Иван", "Петров"))
                .isNotEmpty()
                .isNotEqualTo(OnlineSpielerName.schluessel("Пётр", "Иванов"));
    }

    @Test
    void nullWirdWieLeererNameBehandelt() {
        assertThat(OnlineSpielerName.schluessel(null, "Muster")).isEqualTo("muster");
    }
}
