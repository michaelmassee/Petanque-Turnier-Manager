package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class GruppenAufteilungRechnerTest {

    // ---------------------------------------------------------------
    // Exakte Aufteilung (kein Rest)
    // ---------------------------------------------------------------

    @Test
    public void testExaktAufgeteilt_keineRestgruppe() {
        // 32 Teams, max 16 → [16, 16]
        assertThat(GruppenAufteilungRechner.berechne(32, 16, 16)).containsExactly(16, 16);
    }

    @Test
    public void testExaktAufgeteilt_eineGruppe() {
        // 16 Teams, max 16 → [16]
        assertThat(GruppenAufteilungRechner.berechne(16, 16, 16)).containsExactly(16);
    }

    // ---------------------------------------------------------------
    // Szenario 1: Rest >= minRestGroesse → eigenes Folgeturnier
    // ---------------------------------------------------------------

    @Test
    public void testSzenario1_restGroesserMinRest() {
        // 42 Teams, max 16, minRest 16 → Rest=10 < 16 → Szenario 2!
        // Korrektur: 42 / 16 = 2 Rest 10 → 10 < 16 → Szenario 2 → [16, 26]
        // Um Szenario 1 zu testen: minRest = 8 → 10 >= 8 → [16, 16, 10]
        assertThat(GruppenAufteilungRechner.berechne(42, 16, 8)).containsExactly(16, 16, 10);
    }

    @Test
    public void testSzenario1_restGleichMinRest() {
        // 48 Teams, max 16, minRest 16 → Rest=0 → perfekt → [16, 16, 16]
        assertThat(GruppenAufteilungRechner.berechne(48, 16, 16)).containsExactly(16, 16, 16);
    }

    @Test
    public void testSzenario1_restGleichMinRest_direkt() {
        // 40 Teams, max 16, minRest 8 → Rest=8 >= 8 → [16, 16, 8]
        assertThat(GruppenAufteilungRechner.berechne(40, 16, 8)).containsExactly(16, 16, 8);
    }

    @Test
    public void testSzenario1_minRest4_rest5() {
        // 21 Teams, max 16, minRest 4 → Rest=5 >= 4 → [16, 5]
        assertThat(GruppenAufteilungRechner.berechne(21, 16, 4)).containsExactly(16, 5);
    }

    // ---------------------------------------------------------------
    // Szenario 2: Rest < minRestGroesse → in letzte Gruppe falten
    // ---------------------------------------------------------------

    @Test
    public void testSzenario2_restKleinerMinRest() {
        // 34 Teams, max 16, minRest 16 → Rest=2 < 16 → [16, 18]
        assertThat(GruppenAufteilungRechner.berechne(34, 16, 16)).containsExactly(16, 18);
    }

    @Test
    public void testSzenario2_18Teams() {
        // 18 Teams, max 16, minRest 16 → Rest=2 < 16 → Szenario 2 → letztes volles Feld: 16+2=18 → [18]
        assertThat(GruppenAufteilungRechner.berechne(18, 16, 16)).containsExactly(18);
    }

    @Test
    public void testSzenario2_minRest4_rest3() {
        // 19 Teams, max 16, minRest 4 → Rest=3 < 4 → [16+3=19]
        assertThat(GruppenAufteilungRechner.berechne(19, 16, 4)).containsExactly(19);
    }

    @Test
    public void testSzenario2_42Teams_minRest16() {
        // 42 Teams, max 16, minRest 16 → Rest=10 < 16 → Szenario 2 → [16, 26]
        assertThat(GruppenAufteilungRechner.berechne(42, 16, 16)).containsExactly(16, 26);
    }

    // ---------------------------------------------------------------
    // Sonderfall: kein volles Team-Feld, Rest < minRestGroesse
    // ---------------------------------------------------------------

    @Test
    public void testSonderfall_nurRestKleinerMinRest() {
        // 3 Teams, max 16, minRest 16 → keine vollen Gruppen, Rest=3 → [3]
        assertThat(GruppenAufteilungRechner.berechne(3, 16, 16)).containsExactly(3);
    }

    // ---------------------------------------------------------------
    // IllegalArgumentException
    // ---------------------------------------------------------------

    @Test
    public void testIllegalArg_maxGruppenGroesseKeineZweierpotenz() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(42, 15, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_minRestGroesseKeineZweierpotenz() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(42, 16, 15))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzTeamsNull() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(0, 16, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testIllegalArg_anzTeamsNegativ() {
        assertThatThrownBy(() -> GruppenAufteilungRechner.berechne(-1, 16, 16))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
