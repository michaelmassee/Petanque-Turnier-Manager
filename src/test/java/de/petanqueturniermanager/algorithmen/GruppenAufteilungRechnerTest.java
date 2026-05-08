package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class GruppenAufteilungRechnerTest {

    // ---------------------------------------------------------------
    // Exakte Aufteilung (kein Rest)
    // ---------------------------------------------------------------

    @Test
    public void testExaktAufgeteilt_zweiGruppen() {
        assertThat(GruppenAufteilungRechner.berechne(32, 16)).containsExactly(16, 16);
    }

    @Test
    public void testExaktAufgeteilt_eineGruppe() {
        assertThat(GruppenAufteilungRechner.berechne(16, 16)).containsExactly(16);
    }

    @Test
    public void testExaktAufgeteilt_dreiGruppen() {
        assertThat(GruppenAufteilungRechner.berechne(48, 16)).containsExactly(16, 16, 16);
    }

    // ---------------------------------------------------------------
    // Rest >= 2 → eigene Folgegruppe
    // ---------------------------------------------------------------

    @Test
    public void test15Teams_gruppe8() {
        // 15 Teams, max 8 → [8, 7]
        assertThat(GruppenAufteilungRechner.berechne(15, 8)).containsExactly(8, 7);
    }

    @Test
    public void test35Teams_gruppe16() {
        // 35 Teams, max 16 → [16, 16, 3]
        assertThat(GruppenAufteilungRechner.berechne(35, 16)).containsExactly(16, 16, 3);
    }

    @Test
    public void test57Teams_gruppe16() {
        // 57 Teams, max 16 → [16, 16, 16, 9]
        assertThat(GruppenAufteilungRechner.berechne(57, 16)).containsExactly(16, 16, 16, 9);
    }

    @Test
    public void test18Teams_gruppe16() {
        // 18 Teams, max 16 → [16, 2]
        assertThat(GruppenAufteilungRechner.berechne(18, 16)).containsExactly(16, 2);
    }

    @Test
    public void testNurRest_keineVolleGruppe() {
        // 3 Teams, max 16 → [3]
        assertThat(GruppenAufteilungRechner.berechne(3, 16)).containsExactly(3);
    }

    // ---------------------------------------------------------------
    // 1-Team-Rest wird in vorherige Gruppe gefaltet
    // ---------------------------------------------------------------

    @Test
    public void test9Teams_gruppe8_einTeamFold() {
        // 9 Teams, max 8 → [8, 1] → Fold → [9]
        assertThat(GruppenAufteilungRechner.berechne(9, 8)).containsExactly(9);
    }

    @Test
    public void test17Teams_gruppe16_einTeamFold() {
        // 17 Teams, max 16 → [16, 1] → Fold → [17]
        assertThat(GruppenAufteilungRechner.berechne(17, 16)).containsExactly(17);
    }

    @Test
    public void test33Teams_gruppe16_einTeamFold() {
        // 33 Teams, max 16 → [16, 16, 1] → Fold → [16, 17]
        assertThat(GruppenAufteilungRechner.berechne(33, 16)).containsExactly(16, 17);
    }

    // ---------------------------------------------------------------
    // Sonderfall: 1 Team gesamt (keine Fold-Möglichkeit)
    // ---------------------------------------------------------------

    @Test
    public void testNur1Team() {
        // 1 Team, max 16 → [1] (kein Fold möglich, da nur eine Gruppe)
        assertThat(GruppenAufteilungRechner.berechne(1, 16)).containsExactly(1);
    }

    // ---------------------------------------------------------------
    // IllegalArgumentException
    // ---------------------------------------------------------------

    @Test
    public void testIllegalArg_anzTeamsNull() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(0, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzTeamsNegativ() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(-1, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_maxGruppenGroesseZuKlein() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(16, 1))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
