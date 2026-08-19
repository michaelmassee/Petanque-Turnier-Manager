package de.petanqueturniermanager.schweizer.aggregation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamSpieltagErgebnis;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

public class SchweizerSpieltagErgebnisMapperTest {

    @Test
    public void testMappingSiegeUndPunkte() throws Exception {
        // 3 Runden gespielt, 2 Siege, erzielte Punkte 33, Punktedifferenz +9 -> Punkte- = 24
        SchweizerTeamErgebnis ergebnis = new SchweizerTeamErgebnis(5, 2, 9, 33, List.of(1, 2));

        TeamSpieltagErgebnis erg = SchweizerSpieltagErgebnisMapper.von(SpielTagNr.from(1), ergebnis, 3);

        assertThat(erg.getTeamNr()).isEqualTo(5);
        assertThat(erg.getSpielTagNr()).isEqualTo(1);
        assertThat(erg.getSpielPlus()).isEqualTo(2);
        assertThat(erg.getSpielMinus()).isEqualTo(1);
        assertThat(erg.getPunktePlus()).isEqualTo(33);
        assertThat(erg.getPunkteMinus()).isEqualTo(24);
        assertThat(erg.isTeilgenommen()).isTrue();
    }

    @Test
    public void testAlleRundenGewonnen() throws Exception {
        SchweizerTeamErgebnis ergebnis = new SchweizerTeamErgebnis(1, 4, 40, 52, List.of());

        TeamSpieltagErgebnis erg = SchweizerSpieltagErgebnisMapper.von(SpielTagNr.from(2), ergebnis, 4);

        assertThat(erg.getSpielMinus()).isZero();
        assertThat(erg.getPunktePlus()).isEqualTo(52);
        assertThat(erg.getPunkteMinus()).isEqualTo(12);
    }

    @Test
    public void testSpielMinusNieNegativ() throws Exception {
        // Randfall: mehr Siege gezaehlt als Runden bekannt (sollte in der Praxis nicht vorkommen,
        // die Berechnung darf aber nie negativ werden)
        SchweizerTeamErgebnis ergebnis = new SchweizerTeamErgebnis(1, 3, 10, 30, List.of());

        TeamSpieltagErgebnis erg = SchweizerSpieltagErgebnisMapper.von(SpielTagNr.from(1), ergebnis, 2);

        assertThat(erg.getSpielMinus()).isZero();
    }

}
