package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.PouleTeamErgebnis.SpielErgebnisGegen;

public class PouleTeamErgebnisTest {

    @Test
    public void konstruktor_kopiertSpielergebnisseAlsUnveraenderlicheListe() {
        var mutierbar = new ArrayList<SpielErgebnisGegen>();
        mutierbar.add(new SpielErgebnisGegen(2, 13, 7));

        var ergebnis = new PouleTeamErgebnis(1, 1, 0, 6, 13, mutierbar);

        // Nachträgliches Anfügen am Original darf das Record nicht verändern
        mutierbar.add(new SpielErgebnisGegen(3, 9, 13));
        assertThat(ergebnis.spielErgebnisse()).hasSize(1);

        // Die zurückgegebene Liste ist immutable
        assertThatThrownBy(() -> ergebnis.spielErgebnisse().add(new SpielErgebnisGegen(4, 13, 0)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    public void konstruktor_wirftBeiNullSpielergebnissen() {
        assertThatNullPointerException()
                .isThrownBy(() -> new PouleTeamErgebnis(1, 0, 0, 0, 0, null))
                .withMessageContaining("spielErgebnisse");
    }

    @Test
    public void gegnerNrn_extrahiertAlleGegnerInReihenfolge() {
        var ergebnis = new PouleTeamErgebnis(1, 2, 1, 5, 30, List.of(
                new SpielErgebnisGegen(2, 13, 7),
                new SpielErgebnisGegen(3, 4, 13),
                new SpielErgebnisGegen(4, 13, 10)));

        assertThat(ergebnis.gegnerNrn()).containsExactly(2, 3, 4);
    }

    @Test
    public void gegnerNrn_leerWennKeineSpiele() {
        var ergebnis = new PouleTeamErgebnis(1, 0, 0, 0, 0, List.of());
        assertThat(ergebnis.gegnerNrn()).isEmpty();
    }
}
