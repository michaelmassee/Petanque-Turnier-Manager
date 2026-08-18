package de.petanqueturniermanager.algorithmen.turnierserie.ergebnis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.supermelee.SpielTagNr;

public class TeamSpieltagErgebnisTest {

    @Test
    public void testKonstruktorSpielTagNull() throws Exception {
        assertThrows(NullPointerException.class, () -> new TeamSpieltagErgebnis(null, 1));
    }

    @Test
    public void testKonstruktorUngueltigeTeamNr() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> new TeamSpieltagErgebnis(SpielTagNr.from(1), 0));
    }

    @Test
    public void testNichtTeilgenommenFactory() throws Exception {
        TeamSpieltagErgebnis erg = TeamSpieltagErgebnis.nichtTeilgenommen(SpielTagNr.from(2), 5);
        assertThat(erg.getSpielTagNr()).isEqualTo(2);
        assertThat(erg.getTeamNr()).isEqualTo(5);
        assertThat(erg.isTeilgenommen()).isFalse();
    }

    @Test
    public void testIsTeilgenommenBeiPositivemSpielPlus() throws Exception {
        TeamSpieltagErgebnis erg = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1).setSpielPlus(0);
        assertThat(erg.isTeilgenommen()).isTrue();
    }

    @Test
    public void testFreilosFlag() throws Exception {
        TeamSpieltagErgebnis erg = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1);
        assertThat(erg.isFreilos()).isFalse();
        assertThat(erg.setFreilos(true)).isSameAs(erg);
        assertThat(erg.isFreilos()).isTrue();
    }

    @Test
    public void testCompareToBesterKommtZuerst() throws Exception {
        TeamSpieltagErgebnis besser = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1).setSpielPlus(6);
        TeamSpieltagErgebnis schlechter = new TeamSpieltagErgebnis(SpielTagNr.from(1), 2).setSpielPlus(3);

        assertThat(besser.compareTo(schlechter)).isNegative();
    }

    @Test
    public void testReversedCompareToSchlechtesterZuerst() throws Exception {
        TeamSpieltagErgebnis besser = new TeamSpieltagErgebnis(SpielTagNr.from(1), 1).setSpielPlus(6);
        TeamSpieltagErgebnis schlechter = new TeamSpieltagErgebnis(SpielTagNr.from(1), 2).setSpielPlus(2);

        assertThat(schlechter.reversedCompareTo(besser)).isNegative();
    }

}
