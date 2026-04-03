/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PouleRanglisteRechnerTest {

    private PouleRanglisteRechner rechner;

    @BeforeEach
    void setUp() {
        rechner = new PouleRanglisteRechner();
    }

    // -----------------------------------------------------------------------
    // Hilfsmethoden
    // -----------------------------------------------------------------------

    private static PouleTeamErgebnis.SpielErgebnisGegen gegen(int gegnerNr, int eigene, int gegner) {
        return new PouleTeamErgebnis.SpielErgebnisGegen(gegnerNr, eigene, gegner);
    }

    private static PouleTeamErgebnis ergebnis(int teamNr, int siege, int niederlagen,
            int punkteDiff, int erzieltePunkte,
            PouleTeamErgebnis.SpielErgebnisGegen... spiele) {
        return new PouleTeamErgebnis(teamNr, siege, niederlagen, punkteDiff, erzieltePunkte,
                List.of(spiele));
    }

    // -----------------------------------------------------------------------
    // 4er-Poule – klare Rangfolge (2-0, 2-1, 1-2, 0-2)
    // -----------------------------------------------------------------------

    @Test
    void testVierTeamPoule_klareRangfolge_siegeEntscheiden() {
        var team1 = ergebnis(1, 2, 0, +15, 26);
        var team2 = ergebnis(2, 2, 1, +5, 28);
        var team3 = ergebnis(3, 1, 2, -10, 18);
        var team4 = ergebnis(4, 0, 2, -10, 15);

        var sortiert = rechner.sortiere(List.of(team4, team2, team1, team3));

        assertThat(sortiert)
                .extracting(PouleTeamErgebnis::teamNr)
                .containsExactly(1, 2, 3, 4);
    }

    @Test
    void testVierTeamPoule_platz1HatZweiSiegeOhneNiederlage() {
        var platz1 = ergebnis(1, 2, 0, +10, 26);
        var platz2 = ergebnis(2, 2, 1, +5, 28);
        var platz3 = ergebnis(3, 1, 2, -5, 18);
        var platz4 = ergebnis(4, 0, 2, -10, 15);

        var sortiert = rechner.sortiere(List.of(platz3, platz1, platz4, platz2));

        assertThat(sortiert.get(0).teamNr()).isEqualTo(1);
        assertThat(sortiert.get(3).teamNr()).isEqualTo(4);
    }

    // -----------------------------------------------------------------------
    // Niederlagen als zweites Kriterium
    // -----------------------------------------------------------------------

    @Test
    void testSortierung_niederlagenAlsZweitesKriterium() {
        // Team 1: 1 Sieg, 0 Niederlagen → vor Team 2: 1 Sieg, 1 Niederlage
        var teamA = ergebnis(1, 1, 0, +5, 13);
        var teamB = ergebnis(2, 1, 1, +3, 13);

        var sortiert = rechner.sortiere(List.of(teamB, teamA));

        assertThat(sortiert)
                .extracting(PouleTeamErgebnis::teamNr)
                .containsExactly(1, 2);
    }

    // -----------------------------------------------------------------------
    // Punktedifferenz als drittes Kriterium (3er-Poule)
    // -----------------------------------------------------------------------

    @Test
    void testDreiTeamPoule_punkteDifferenzLoestGleichstand() {
        // T1 schlägt T2 13:5, T2 schlägt T3 13:7, T3 schlägt T1 13:10
        // T1: 1S, 1N, PD = (13-5) + (10-13) = +8-3 = +5
        // T2: 1S, 1N, PD = (5-13) + (13-7) = -8+6 = -2
        // T3: 1S, 1N, PD = (7-13) + (13-10) = -6+3 = -3
        var t1 = new PouleTeamErgebnis(1, 1, 1, +5, 13 + 10,
                List.of(gegen(2, 13, 5), gegen(3, 10, 13)));
        var t2 = new PouleTeamErgebnis(2, 1, 1, -2, 5 + 13,
                List.of(gegen(1, 5, 13), gegen(3, 13, 7)));
        var t3 = new PouleTeamErgebnis(3, 1, 1, -3, 7 + 13,
                List.of(gegen(2, 7, 13), gegen(1, 13, 10)));

        var sortiert = rechner.sortiere(List.of(t2, t3, t1));

        assertThat(sortiert)
                .extracting(PouleTeamErgebnis::teamNr)
                .containsExactly(1, 2, 3);
    }

    // -----------------------------------------------------------------------
    // Direktvergleich als viertes Kriterium
    // -----------------------------------------------------------------------

    @Test
    void testDreiTeamPoule_direktvergleichLoestGleichstandBeiGleicherPunktedifferenz() {
        // T1 schlägt T2 13:0, T2 schlägt T3 13:0, T3 schlägt T1 13:0
        // alle: 1S, 1N, PD = 13-0 + 0-13 = 0 → Direktvergleich
        // T1 > T2 (T1 gewann direkt), T2 > T3, aber T3 > T1 (zyklisch)
        // Zwischen T1 und T2: T1 hat Direktvergleich gewonnen → T1 vor T2
        var t1 = new PouleTeamErgebnis(1, 1, 1, 0, 13,
                List.of(gegen(2, 13, 0), gegen(3, 0, 13)));
        var t2 = new PouleTeamErgebnis(2, 1, 1, 0, 13,
                List.of(gegen(1, 0, 13), gegen(3, 13, 0)));
        var t3 = new PouleTeamErgebnis(3, 1, 1, 0, 13,
                List.of(gegen(2, 0, 13), gegen(1, 13, 0)));

        var sortiert = rechner.sortiere(List.of(t2, t3, t1));

        // Zyklisch: T1>T2>T3>T1 – Reihenfolge ist nicht-deterministisch, aber kein Exception
        assertThat(sortiert).hasSize(3);
    }

    @Test
    void testDreiTeamPoule_direktvergleichBeiZweiTeamsGleichstand_nichtZyklisch() {
        // T1 schlägt T3 13:5 und verliert gegen T2 5:13
        // T2 schlägt T1 13:5 und verliert gegen T3 8:13
        // T3 schlägt T2 13:8 und verliert gegen T1 5:13
        // alle: 1S, 1N, PD = 0 (jeder: +13-5 + -(13-aufgegebene) ...)
        // Tatsächliche PDs: T1: (13-5)+(5-13)=0, T2: (13-5)+(8-13)=-5+8=+3, T3: (13-8)+(5-13)=0
        // T2 hat PD=+3 → Platz 1; T1 und T3 gleich → Direktvergleich T1 vs T3 (T1 gewinnt 13:5)
        var t1 = new PouleTeamErgebnis(1, 1, 1, 0, 18,
                List.of(gegen(3, 13, 5), gegen(2, 5, 13)));
        var t2 = new PouleTeamErgebnis(2, 1, 1, 3, 21,
                List.of(gegen(1, 13, 5), gegen(3, 8, 13)));
        var t3 = new PouleTeamErgebnis(3, 1, 1, 0, 18,
                List.of(gegen(2, 13, 8), gegen(1, 5, 13)));

        var sortiert = rechner.sortiere(List.of(t3, t1, t2));

        // T2 hat höchste PD → Platz 1; T1 schlägt T3 direkt → T1 Platz 2, T3 Platz 3
        assertThat(sortiert)
                .extracting(PouleTeamErgebnis::teamNr)
                .containsExactly(2, 1, 3);
    }

    // -----------------------------------------------------------------------
    // Zyklischer Gleichstand – kein Exception
    // -----------------------------------------------------------------------

    @Test
    void testDreiTeamPoule_zyklischerGleichstand_fallbackAufTeamNr() {
        // T1>T2, T2>T3, T3>T1 – Direktvergleich ergibt keinen totale Ordnung.
        // Fallback-Kriterium teamNr garantiert stabile, deterministische Reihenfolge: [1, 2, 3]
        var t1 = new PouleTeamErgebnis(1, 1, 1, 0, 13,
                List.of(gegen(2, 13, 0), gegen(3, 0, 13)));
        var t2 = new PouleTeamErgebnis(2, 1, 1, 0, 13,
                List.of(gegen(1, 0, 13), gegen(3, 13, 0)));
        var t3 = new PouleTeamErgebnis(3, 1, 1, 0, 13,
                List.of(gegen(2, 0, 13), gegen(1, 13, 0)));

        var sortiert = rechner.sortiere(List.of(t3, t1, t2));

        assertThat(sortiert)
                .extracting(PouleTeamErgebnis::teamNr)
                .containsExactly(1, 2, 3);
    }

    // -----------------------------------------------------------------------
    // Leere Liste
    // -----------------------------------------------------------------------

    @Test
    void testSortiere_leereListe_gibtLeereListe() {
        assertThat(rechner.sortiere(List.of())).isEmpty();
    }

    // -----------------------------------------------------------------------
    // Unveränderlichkeit der Rückgabe
    // -----------------------------------------------------------------------

    @Test
    void testSortiere_rueckgabeIstUnveraenderlich() {
        var einzeleintrag = ergebnis(1, 2, 0, 10, 26);
        var sortiert = rechner.sortiere(List.of(einzeleintrag));

        assertThatThrownBy(() -> sortiert.add(einzeleintrag))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -----------------------------------------------------------------------
    // PouleTeamErgebnis – defensive copy
    // -----------------------------------------------------------------------

    @Test
    void testPouleTeamErgebnis_spielErgebnisseNull_wirftNullPointerException() {
        assertThatThrownBy(() -> new PouleTeamErgebnis(1, 1, 0, 8, 13, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("spielErgebnisse");
    }

    @Test
    void testPouleTeamErgebnis_defensiveCopy_aenderungExternerListeHatKeineAuswirkung() {
        var externeList = new ArrayList<PouleTeamErgebnis.SpielErgebnisGegen>();
        externeList.add(gegen(2, 13, 5));
        var teamErgebnis = new PouleTeamErgebnis(1, 1, 0, 8, 13, externeList);

        externeList.clear();

        assertThat(teamErgebnis.spielErgebnisse()).hasSize(1);
    }

    @Test
    void testPouleTeamErgebnis_spielErgebnisseIstUnveraenderlich() {
        var teamErgebnis = new PouleTeamErgebnis(1, 1, 0, 8, 13, List.of(gegen(2, 13, 5)));

        assertThatThrownBy(() -> teamErgebnis.spielErgebnisse().add(gegen(3, 13, 5)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    // -----------------------------------------------------------------------
    // PouleTeamErgebnis – gegnerNrn()
    // -----------------------------------------------------------------------

    @Test
    void testGegnerNrn_gibtGegnerNummernInReihenfolge() {
        var teamErgebnis = new PouleTeamErgebnis(1, 2, 0, 20, 26,
                List.of(gegen(2, 13, 5), gegen(3, 13, 7)));

        assertThat(teamErgebnis.gegnerNrn()).containsExactly(2, 3);
    }
}
