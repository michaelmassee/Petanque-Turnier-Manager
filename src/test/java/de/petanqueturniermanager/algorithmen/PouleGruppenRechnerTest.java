/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class PouleGruppenRechnerTest {

    // -----------------------------------------------------------------------
    // berechneGruppenGroessen – Rest 0 (alle Vierergruppen)
    // -----------------------------------------------------------------------

    @Test
    void testRest0_8Teams_zweiVierergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(8)).containsExactly(4, 4);
    }

    @Test
    void testRest0_12Teams_dreiVierergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(12)).containsExactly(4, 4, 4);
    }

    @Test
    void testRest0_16Teams_vierVierergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(16)).containsExactly(4, 4, 4, 4);
    }

    // -----------------------------------------------------------------------
    // berechneGruppenGroessen – Rest 1 (drei Dreiergruppen)
    // -----------------------------------------------------------------------

    @Test
    void testRest1_9Teams_dreiDreiergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(9)).containsExactly(3, 3, 3);
    }

    @Test
    void testRest1_13Teams_eineViererUndDreiDreiergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(13)).containsExactly(4, 3, 3, 3);
    }

    @Test
    void testRest1_17Teams_zweiViererUndDreiDreiergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(17)).containsExactly(4, 4, 3, 3, 3);
    }

    // -----------------------------------------------------------------------
    // berechneGruppenGroessen – Rest 2 (zwei Dreiergruppen)
    // -----------------------------------------------------------------------

    @Test
    void testRest2_6Teams_zweiDreiergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(6)).containsExactly(3, 3);
    }

    @Test
    void testRest2_10Teams_eineViererUndZweiDreiergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(10)).containsExactly(4, 3, 3);
    }

    @Test
    void testRest2_14Teams_zweiViererUndZweiDreiergruppen() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(14)).containsExactly(4, 4, 3, 3);
    }

    // -----------------------------------------------------------------------
    // berechneGruppenGroessen – Rest 3 (eine Dreiergruppe)
    // -----------------------------------------------------------------------

    @Test
    void testRest3_7Teams_eineViererUndEineDreiergruppe() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(7)).containsExactly(4, 3);
    }

    @Test
    void testRest3_11Teams_zweiViererUndEineDreiergruppe() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(11)).containsExactly(4, 4, 3);
    }

    @Test
    void testRest3_15Teams_dreiViererUndEineDreiergruppe() {
        assertThat(PouleGruppenRechner.berechneGruppenGroessen(15)).containsExactly(4, 4, 4, 3);
    }

    // -----------------------------------------------------------------------
    // Invariante: Summe der Gruppengrößen == anzTeams
    // -----------------------------------------------------------------------

    @Test
    void testSummeGruppenGroessen_gleichAnzTeams() {
        var gueltigeWerte = List.of(3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 20, 21, 28, 32, 50);
        for (int n : gueltigeWerte) {
            int summe = PouleGruppenRechner.berechneGruppenGroessen(n)
                    .stream().mapToInt(Integer::intValue).sum();
            assertThat(summe)
                    .as("Summe der Gruppengrößen fuer anzTeams=%d", n)
                    .isEqualTo(n);
        }
    }

    // -----------------------------------------------------------------------
    // anzVierTeamGruppen und anzDreiTeamGruppen
    // -----------------------------------------------------------------------

    @Test
    void testAnzVierTeamGruppen_rest0() {
        assertThat(PouleGruppenRechner.anzVierTeamGruppen(8)).isEqualTo(2);
        assertThat(PouleGruppenRechner.anzVierTeamGruppen(12)).isEqualTo(3);
    }

    @Test
    void testAnzDreiTeamGruppen_rest0_gibtNull() {
        assertThat(PouleGruppenRechner.anzDreiTeamGruppen(8)).isZero();
        assertThat(PouleGruppenRechner.anzDreiTeamGruppen(16)).isZero();
    }

    @Test
    void testAnzVierTeamGruppen_rest1() {
        assertThat(PouleGruppenRechner.anzVierTeamGruppen(9)).isZero();
        assertThat(PouleGruppenRechner.anzVierTeamGruppen(13)).isEqualTo(1);
    }

    @Test
    void testAnzDreiTeamGruppen_rest1_gibtDrei() {
        assertThat(PouleGruppenRechner.anzDreiTeamGruppen(9)).isEqualTo(3);
        assertThat(PouleGruppenRechner.anzDreiTeamGruppen(13)).isEqualTo(3);
    }

    @Test
    void testAnzGruppen_summeStimmt() {
        for (int n : List.of(6, 7, 8, 9, 10, 11, 12, 13)) {
            int erwartet = PouleGruppenRechner.anzVierTeamGruppen(n)
                    + PouleGruppenRechner.anzDreiTeamGruppen(n);
            assertThat(PouleGruppenRechner.anzGruppen(n))
                    .as("anzGruppen(%d)", n)
                    .isEqualTo(erwartet);
        }
    }

    // -----------------------------------------------------------------------
    // Unveränderlichkeit der Rückgabeliste
    // -----------------------------------------------------------------------

    @Test
    void testBerechneGruppenGroessen_rueckgabeIstUnveraenderlich() {
        var gruppen = PouleGruppenRechner.berechneGruppenGroessen(8);
        assertThatThrownBy(() -> gruppen.add(4))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -----------------------------------------------------------------------
    // Validierungsfehler
    // -----------------------------------------------------------------------

    @Test
    void testValidierung_anzTeamsNull_wirftException() {
        assertThatThrownBy(() -> PouleGruppenRechner.berechneGruppenGroessen(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidierung_anzTeamsNegativ_wirftException() {
        assertThatThrownBy(() -> PouleGruppenRechner.berechneGruppenGroessen(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testValidierung_rest1MitNurFuenfTeams_wirftException() {
        // 5 % 4 == 1, aber 5 < 9 → ungültig
        assertThatThrownBy(() -> PouleGruppenRechner.berechneGruppenGroessen(5))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("9");
    }

    @Test
    void testValidierung_zweiTeams_wirftException() {
        // 2 < 3 → ungültig (Basisprüfung)
        assertThatThrownBy(() -> PouleGruppenRechner.berechneGruppenGroessen(2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("3");
    }

    @Test
    void testValidierung_einsIstUngueltig() {
        assertThatThrownBy(() -> PouleGruppenRechner.berechneGruppenGroessen(1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
