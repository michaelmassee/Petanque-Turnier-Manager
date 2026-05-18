package de.petanqueturniermanager.helper.rangliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Unit-Tests für die UNO-freie Logik der Rangliste-Signatur:
 * kanonische Wert-Normalisierung und Quellen-Validierung.
 */
public class RanglisteEingabeSignaturTest {

    @Test
    public void normalisiere_null_liefertNull() {
        assertThat(RanglisteEingabeSignatur.normalisiereWert(null)).isNull();
    }

    @Test
    public void normalisiere_leererString_liefertNull() {
        assertThat(RanglisteEingabeSignatur.normalisiereWert("")).isNull();
        assertThat(RanglisteEingabeSignatur.normalisiereWert("   ")).isNull();
    }

    @Test
    public void normalisiere_stringMitWhitespace_wirdGetrimmt() {
        assertThat(RanglisteEingabeSignatur.normalisiereWert("  Foo  ")).isEqualTo("Foo");
    }

    @Test
    public void normalisiere_ganzzahligesDouble_wirdAlsLongFormatiert() {
        assertThat(RanglisteEingabeSignatur.normalisiereWert(13.0d)).isEqualTo("13");
        assertThat(RanglisteEingabeSignatur.normalisiereWert(0.0d)).isEqualTo("0");
        assertThat(RanglisteEingabeSignatur.normalisiereWert(-7.0d)).isEqualTo("-7");
    }

    @Test
    public void normalisiere_echteFliesskommazahl_wirdAlsDoubleFormatiert() {
        assertThat(RanglisteEingabeSignatur.normalisiereWert(3.14d)).isEqualTo("3.14");
    }

    @Test
    public void normalisiere_integerObjekt_bleibtIntegerString() {
        assertThat(RanglisteEingabeSignatur.normalisiereWert(Integer.valueOf(42))).isEqualTo("42");
        assertThat(RanglisteEingabeSignatur.normalisiereWert(Integer.valueOf(0))).isEqualTo("0");
    }

    @Test
    public void signaturQuelle_ungueltigeArgumente_werfen() {
        Set<Integer> spalten = Set.of(1, 2);
        assertThatThrownBy(() -> new SignaturQuelle("", "key", 0, 10, spalten, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignaturQuelle("id", "", 0, 10, spalten, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignaturQuelle("id", "key", -1, 10, spalten, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignaturQuelle("id", "key", 0, 0, spalten, true))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new SignaturQuelle("id", "key", 0, 10, Set.of(), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void signaturQuelle_relevanteSpaltenMinMax() {
        SignaturQuelle q = new SignaturQuelle("id", "key", 2, 100, Set.of(3, 1, 7), true);
        assertThat(q.ersteRelevanteSpalte()).isEqualTo(1);
        assertThat(q.letzteRelevanteSpalte()).isEqualTo(7);
    }
}
