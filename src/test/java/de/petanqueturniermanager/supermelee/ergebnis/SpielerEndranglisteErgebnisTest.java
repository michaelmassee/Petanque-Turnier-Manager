package de.petanqueturniermanager.supermelee.ergebnis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

public class SpielerEndranglisteErgebnisTest {

    @Test
    public void testKonstruktorUngueltigeNr() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new SpielerEndranglisteErgebnis(0));
    }

    @Test
    public void testKonstruktorNegativeNr() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new SpielerEndranglisteErgebnis(-1));
    }

    @Test
    public void testGetSpielDiv() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setSpielPlus(7).setSpielMinus(3);
        assertThat(erg.getSpielDiv()).isEqualTo(4);
    }

    @Test
    public void testGetSpielDivNegativ() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setSpielPlus(2).setSpielMinus(5);
        assertThat(erg.getSpielDiv()).isEqualTo(-3);
    }

    @Test
    public void testGetPunkteDiv() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setPunktePlus(13).setPunkteMinus(6);
        assertThat(erg.getPunkteDiv()).isEqualTo(7);
    }

    @Test
    public void testFluentSetterChaining() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        SpielerEndranglisteErgebnis result = erg.setSpielPlus(3).setSpielMinus(1).setPunktePlus(10).setPunkteMinus(4);
        assertThat(result).isSameAs(erg);
    }

    @Test
    public void testIsValidGueltig() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setSpielPlus(3).setSpielMinus(1).setPunktePlus(10).setPunkteMinus(4);
        assertThat(erg.isValid()).isTrue();
    }

    @Test
    public void testIsValidAlleNullWerte() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        // alle Standardwerte sind 0, das ist gueltig
        assertThat(erg.isValid()).isTrue();
    }

    @Test
    public void testIsValidNegativesSpielMinus() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setSpielMinus(-1);
        assertThat(erg.isValid()).isFalse();
    }

    @Test
    public void testIsValidNegativesPunktePlus() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setPunktePlus(-1);
        assertThat(erg.isValid()).isFalse();
    }

    @Test
    public void testCompareToBesterKommtZuerst() throws Exception {
        // Spieler mit mehr SpielPlus kommt zuerst (beste zuerst)
        SpielerEndranglisteErgebnis besser = new SpielerEndranglisteErgebnis(1);
        besser.setSpielPlus(5);

        SpielerEndranglisteErgebnis schlechter = new SpielerEndranglisteErgebnis(2);
        schlechter.setSpielPlus(3);

        // besser.compareTo(schlechter) < 0 -> besser kommt zuerst
        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testCompareToNachSpielDiv() throws Exception {
        // Gleiche SpielPlus -> SpielDiv entscheidet
        SpielerEndranglisteErgebnis besser = new SpielerEndranglisteErgebnis(1);
        besser.setSpielPlus(5).setSpielMinus(1); // SpielDiv = 4

        SpielerEndranglisteErgebnis schlechter = new SpielerEndranglisteErgebnis(2);
        schlechter.setSpielPlus(5).setSpielMinus(3); // SpielDiv = 2

        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testCompareToNachPunkteDiv() throws Exception {
        // Gleiche SpielPlus, gleiche SpielDiv -> PunkteDiv entscheidet
        SpielerEndranglisteErgebnis besser = new SpielerEndranglisteErgebnis(1);
        besser.setSpielPlus(5).setSpielMinus(3).setPunktePlus(15).setPunkteMinus(5); // PunkteDiv = 10

        SpielerEndranglisteErgebnis schlechter = new SpielerEndranglisteErgebnis(2);
        schlechter.setSpielPlus(5).setSpielMinus(3).setPunktePlus(12).setPunkteMinus(8); // PunkteDiv = 4

        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testCompareToNachSpielerNr() throws Exception {
        // Alle gleich -> niedrigere SpielerNr kommt zuerst
        SpielerEndranglisteErgebnis spieler1 = new SpielerEndranglisteErgebnis(1);
        spieler1.setSpielPlus(5).setSpielMinus(3).setPunktePlus(10).setPunkteMinus(5);

        SpielerEndranglisteErgebnis spieler5 = new SpielerEndranglisteErgebnis(5);
        spieler5.setSpielPlus(5).setSpielMinus(3).setPunktePlus(10).setPunkteMinus(5);

        assertThat(spieler1.compareTo(spieler5)).isNegative();
    }

    @Test
    public void testSortierungMitCollections() throws Exception {
        SpielerEndranglisteErgebnis platz3 = new SpielerEndranglisteErgebnis(1);
        platz3.setSpielPlus(3);

        SpielerEndranglisteErgebnis platz1 = new SpielerEndranglisteErgebnis(2);
        platz1.setSpielPlus(7);

        SpielerEndranglisteErgebnis platz2 = new SpielerEndranglisteErgebnis(3);
        platz2.setSpielPlus(5);

        List<SpielerEndranglisteErgebnis> liste = new ArrayList<>();
        liste.add(platz3);
        liste.add(platz1);
        liste.add(platz2);

        Collections.sort(liste);

        assertThat(liste).containsExactly(platz1, platz2, platz3);
    }

    @Test
    public void testReversedCompareToSchlechtesterZuerst() throws Exception {
        // reversedCompareTo: schlechteste an erster Stelle
        SpielerEndranglisteErgebnis besser = new SpielerEndranglisteErgebnis(1);
        besser.setSpielPlus(5);

        SpielerEndranglisteErgebnis schlechter = new SpielerEndranglisteErgebnis(2);
        schlechter.setSpielPlus(2);

        // schlechter.reversedCompareTo(besser) < 0 -> schlechter kommt zuerst
        assertThat(schlechter.reversedCompareTo(besser)).isNegative();
    }

    @Test
    public void testCompareToGleich() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        assertThat(erg.compareTo(erg)).isZero();
    }

    @Test
    public void testGetSpielDivNull() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setSpielPlus(4).setSpielMinus(4);
        assertThat(erg.getSpielDiv()).isZero();
    }

    @Test
    public void testGetPunkteDivNegativ() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setPunktePlus(5).setPunkteMinus(12);
        assertThat(erg.getPunkteDiv()).isEqualTo(-7);
    }

    @Test
    public void testGetPunkteDivNull() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setPunktePlus(8).setPunkteMinus(8);
        assertThat(erg.getPunkteDiv()).isZero();
    }

    @Test
    public void testIsValidNegativesSpielPlus() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setSpielPlus(-1);
        assertThat(erg.isValid()).isFalse();
    }

    @Test
    public void testIsValidNegativesPunkteMinus() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        erg.setPunkteMinus(-1);
        assertThat(erg.isValid()).isFalse();
    }

    @Test
    public void testCompareTo_nurSpielerNrUnterscheidet_niedrigereZuerst() throws Exception {
        // alle Sort-Kriterien identisch, nur spielerNr unterschiedlich
        SpielerEndranglisteErgebnis kleinerNr = new SpielerEndranglisteErgebnis(3);
        kleinerNr.setSpielPlus(4).setSpielMinus(2).setPunktePlus(15).setPunkteMinus(10);

        SpielerEndranglisteErgebnis groessereNr = new SpielerEndranglisteErgebnis(8);
        groessereNr.setSpielPlus(4).setSpielMinus(2).setPunktePlus(15).setPunkteMinus(10);

        assertThat(kleinerNr.compareTo(groessereNr)).isNegative();
        assertThat(groessereNr.compareTo(kleinerNr)).isPositive();
    }

    @Test
    public void testEquals_bleibtIdentitaetsbasiert() throws Exception {
        // siehe Architektur-Exclude EQ_COMPARETO_USE_OBJECT_EQUALS in spotbugs/exclude.xml:
        // compareTo definiert Sortierung, equals bleibt identitaetsbasiert.
        SpielerEndranglisteErgebnis erg1 = new SpielerEndranglisteErgebnis(1);
        SpielerEndranglisteErgebnis erg2 = new SpielerEndranglisteErgebnis(1);

        assertThat(erg1).isNotEqualTo(erg2);
        assertThat(erg1).isEqualTo(erg1);
    }

    @Test
    public void testFluentChaining_alleSetterGebenSelfZurueck() throws Exception {
        SpielerEndranglisteErgebnis erg = new SpielerEndranglisteErgebnis(1);
        assertThat(erg.setSpielPlus(1)).isSameAs(erg);
        assertThat(erg.setSpielMinus(1)).isSameAs(erg);
        assertThat(erg.setPunktePlus(1)).isSameAs(erg);
        assertThat(erg.setPunkteMinus(1)).isSameAs(erg);
    }

}
