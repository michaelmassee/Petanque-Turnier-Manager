package de.petanqueturniermanager.supermelee.ergebnis;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.Test;

import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SpielerSpieltagErgebnisTest {

    @Test(expected = NullPointerException.class)
    public void testKonstruktorSpielTagNull() throws Exception {
        new SpielerSpieltagErgebnis(null, 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testKonstruktorUngueltigeSpielerNr() throws Exception {
        new SpielerSpieltagErgebnis(SpielTagNr.from(1), 0);
    }

    @Test
    public void testFromFactory() throws Exception {
        SpielTagNr spielTag = SpielTagNr.from(2);
        SpielerSpieltagErgebnis erg = SpielerSpieltagErgebnis.from(spielTag, 3);
        assertThat(erg.getSpielTagNr()).isEqualTo(2);
        assertThat(erg.getSpielerNr()).isEqualTo(3);
    }

    @Test
    public void testGetSpielTagNr() throws Exception {
        SpielerSpieltagErgebnis erg = new SpielerSpieltagErgebnis(SpielTagNr.from(5), 1);
        assertThat(erg.getSpielTagNr()).isEqualTo(5);
    }

    @Test
    public void testGetSpielTag() throws Exception {
        SpielTagNr spielTag = SpielTagNr.from(3);
        SpielerSpieltagErgebnis erg = new SpielerSpieltagErgebnis(spielTag, 1);
        assertThat(erg.getSpielTag()).isSameAs(spielTag);
    }

    @Test
    public void testCompareToBesterKommtZuerst() throws Exception {
        // Spieler mit mehr SpielPlus kommt zuerst
        SpielerSpieltagErgebnis besser = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        besser.setSpielPlus(6);

        SpielerSpieltagErgebnis schlechter = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        schlechter.setSpielPlus(3);

        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testCompareToNachSpielDiv() throws Exception {
        // Gleiche SpielPlus -> SpielDiv entscheidet
        SpielerSpieltagErgebnis besser = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        besser.setSpielPlus(4).setSpielMinus(1); // SpielDiv = 3

        SpielerSpieltagErgebnis schlechter = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        schlechter.setSpielPlus(4).setSpielMinus(2); // SpielDiv = 2

        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testCompareToNachPunkteDiv() throws Exception {
        // Gleiche SpielPlus, SpielDiv -> PunkteDiv entscheidet
        SpielerSpieltagErgebnis besser = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        besser.setSpielPlus(4).setSpielMinus(2).setPunktePlus(20).setPunkteMinus(8); // PunkteDiv = 12

        SpielerSpieltagErgebnis schlechter = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        schlechter.setSpielPlus(4).setSpielMinus(2).setPunktePlus(15).setPunkteMinus(10); // PunkteDiv = 5

        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testCompareToNachSpielTagNr() throws Exception {
        // Gleiche Ergebnisse, aber unterschiedlicher SpielTag -> hoehere SpielTagNr kommt zuerst
        SpielerSpieltagErgebnis spaetererTag = SpielerSpieltagErgebnis.from(SpielTagNr.from(3), 1);
        spaetererTag.setSpielPlus(4).setSpielMinus(2).setPunktePlus(10).setPunkteMinus(5);

        SpielerSpieltagErgebnis fruehererTag = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        fruehererTag.setSpielPlus(4).setSpielMinus(2).setPunktePlus(10).setPunkteMinus(5);

        assertThat(spaetererTag.compareTo(fruehererTag)).isNegative();
    }

    @Test
    public void testCompareToNachSpielerNr() throws Exception {
        // Alles gleich -> niedrigere SpielerNr kommt zuerst
        SpielerSpieltagErgebnis spieler1 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        spieler1.setSpielPlus(4).setSpielMinus(2).setPunktePlus(10).setPunkteMinus(5);

        SpielerSpieltagErgebnis spieler7 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 7);
        spieler7.setSpielPlus(4).setSpielMinus(2).setPunktePlus(10).setPunkteMinus(5);

        assertThat(spieler1.compareTo(spieler7)).isNegative();
    }

    @Test
    public void testSortierungMitCollections() throws Exception {
        SpielerSpieltagErgebnis platz2 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        platz2.setSpielPlus(4);

        SpielerSpieltagErgebnis platz1 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        platz1.setSpielPlus(6);

        SpielerSpieltagErgebnis platz3 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 3);
        platz3.setSpielPlus(2);

        List<SpielerSpieltagErgebnis> liste = new ArrayList<>();
        liste.add(platz2);
        liste.add(platz1);
        liste.add(platz3);

        Collections.sort(liste);

        assertThat(liste).containsExactly(platz1, platz2, platz3);
    }

    @Test
    public void testReversedCompareToSchlechtesterZuerst() throws Exception {
        SpielerSpieltagErgebnis besser = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        besser.setSpielPlus(6);

        SpielerSpieltagErgebnis schlechter = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        schlechter.setSpielPlus(2);

        // schlechter.reversedCompareTo(besser) < 0 -> schlechter kommt zuerst
        assertThat(schlechter.reversedCompareTo(besser)).isNegative();
    }

    @Test
    public void testGetComparatorGleichesVerhaltenWieCompareTo() throws Exception {
        SpielerSpieltagErgebnis erg1 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        erg1.setSpielPlus(5);

        SpielerSpieltagErgebnis erg2 = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 2);
        erg2.setSpielPlus(3);

        Comparator<SpielerSpieltagErgebnis> comparator = Comparator.naturalOrder();
        // erg1 hat mehr SpielPlus -> kommt zuerst
        assertThat(comparator.compare(erg1, erg2)).isNegative();
    }

    @Test
    public void testCompareToGleich() throws Exception {
        SpielerSpieltagErgebnis erg = SpielerSpieltagErgebnis.from(SpielTagNr.from(1), 1);
        assertThat(erg.compareTo(erg)).isZero();
    }

}
