package de.petanqueturniermanager.webserver;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.webserver.RandKonfiguration.RandDaten;

class RandKonfigurationTest {

    @Test
    void keinRahmenBeiDickeNull() {
        var rand = new RandKonfiguration(0, RandKonfiguration.ART_SOLID, 0xFF0000, 0, RandKonfiguration.ANIMATION_KEINE);
        assertThat(rand.toDaten()).isNull();
    }

    @Test
    void keinRahmenBeiArtKein() {
        var rand = new RandKonfiguration(5, RandKonfiguration.ART_KEIN, 0xFF0000, 0, RandKonfiguration.ANIMATION_KEINE);
        assertThat(rand.toDaten()).isNull();
    }

    @Test
    void defaultKeinerHatKeinenRahmen() {
        assertThat(RandKonfiguration.KEINER.toDaten()).isNull();
    }

    @Test
    void rgbaUmrechnungOhneTransparenz() {
        var rand = new RandKonfiguration(3, RandKonfiguration.ART_SOLID, 0xFF0000, 0, RandKonfiguration.ANIMATION_KEINE);
        RandDaten daten = rand.toDaten();
        assertThat(daten).isNotNull();
        assertThat(daten.dicke()).isEqualTo(3);
        assertThat(daten.art()).isEqualTo("solid");
        assertThat(daten.farbe()).isEqualTo("rgba(255,0,0,1.00)");
        assertThat(daten.animation()).isEqualTo("keine");
    }

    @Test
    void rgbaUmrechnungMitHalberTransparenz() {
        var rand = new RandKonfiguration(2, RandKonfiguration.ART_DASHED, 0x00FF00, 50, RandKonfiguration.ANIMATION_AMEISEN);
        RandDaten daten = rand.toDaten();
        assertThat(daten.farbe()).isEqualTo("rgba(0,255,0,0.50)");
    }

    @Test
    void rgbaUmrechnungBeiVollerTransparenzIstDeckkraftNull() {
        var rand = new RandKonfiguration(2, RandKonfiguration.ART_DOTTED, 0x0000FF, 100, RandKonfiguration.ANIMATION_KEINE);
        RandDaten daten = rand.toDaten();
        assertThat(daten.farbe()).isEqualTo("rgba(0,0,255,0.00)");
    }

    @Test
    void unbekannteArtWirdAufKeinNormiert() {
        var rand = new RandKonfiguration(5, "unsinn", 0x000000, 0, RandKonfiguration.ANIMATION_KEINE);
        assertThat(rand.art()).isEqualTo(RandKonfiguration.ART_KEIN);
    }

    @Test
    void unbekannteAnimationWirdAufKeineNormiert() {
        var rand = new RandKonfiguration(5, RandKonfiguration.ART_DOUBLE, 0x000000, 0, "unsinn");
        assertThat(rand.animation()).isEqualTo(RandKonfiguration.ANIMATION_KEINE);
    }

    @Test
    void transparenzWirdAufBereichBegrenzt() {
        assertThat(new RandKonfiguration(1, RandKonfiguration.ART_SOLID, 0, -10, RandKonfiguration.ANIMATION_KEINE).transparenz())
                .isZero();
        assertThat(new RandKonfiguration(1, RandKonfiguration.ART_SOLID, 0, 150, RandKonfiguration.ANIMATION_KEINE).transparenz())
                .isEqualTo(100);
    }

    @Test
    void dickeWirdNichtNegativ() {
        assertThat(new RandKonfiguration(-5, RandKonfiguration.ART_SOLID, 0, 0, RandKonfiguration.ANIMATION_KEINE).dicke())
                .isZero();
    }

    @Test
    void dickeWirdAufMaximumBegrenzt() {
        assertThat(new RandKonfiguration(999, RandKonfiguration.ART_SOLID, 0, 0, RandKonfiguration.ANIMATION_KEINE).dicke())
                .isEqualTo(RandKonfiguration.MAX_DICKE);
    }
}
