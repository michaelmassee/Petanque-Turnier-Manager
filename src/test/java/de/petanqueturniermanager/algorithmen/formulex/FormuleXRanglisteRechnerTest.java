/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.formulex;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FormuleXRanglisteRechnerTest {

    private FormuleXRanglisteRechner rechner;

    @BeforeEach
    void setUp() {
        rechner = new FormuleXRanglisteRechner();
    }

    private static FormuleXTeamErgebnis.SpielErgebnisGegen gegen(int gegnerNr, int eigene, int gegner) {
        return new FormuleXTeamErgebnis.SpielErgebnisGegen(gegnerNr, eigene, gegner);
    }

    private static FormuleXTeamErgebnis ergebnis(int teamNr, int siege, int wertung,
            int punktedifferenz, int eigenePunkte, FormuleXTeamErgebnis.SpielErgebnisGegen... spiele) {
        return new FormuleXTeamErgebnis(teamNr, siege, wertung, punktedifferenz, eigenePunkte, List.of(spiele));
    }

    @Test
    void leereListe_liefertLeereListe() {
        assertThat(rechner.sortiere(List.of())).isEmpty();
    }

    @Test
    void sieganzahl_istHauptkriterium() {
        // Team 1 hat weniger Wertung, aber mehr Siege → muss trotzdem vor Team 2 stehen
        var team1 = ergebnis(1, 3, 300, 0, 0);
        var team2 = ergebnis(2, 2, 500, 0, 0);

        var sortiert = rechner.sortiere(List.of(team2, team1));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void wertung_entscheidetBeiGleicherSieganzahl() {
        var team1 = ergebnis(1, 3, 400, 0, 0);
        var team2 = ergebnis(2, 3, 350, 0, 0);

        var sortiert = rechner.sortiere(List.of(team2, team1));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void punktedifferenz_entscheidetBeiGleicherSiegeUndWertung() {
        var team1 = ergebnis(1, 2, 300, 10, 0);
        var team2 = ergebnis(2, 2, 300, 5, 0);

        var sortiert = rechner.sortiere(List.of(team2, team1));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void eigenePunkte_entscheidenBeiGleicherPunktedifferenz() {
        var team1 = ergebnis(1, 2, 300, 5, 60);
        var team2 = ergebnis(2, 2, 300, 5, 55);

        var sortiert = rechner.sortiere(List.of(team2, team1));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void direktvergleich_entscheidetBeiVollstaendigemGleichstandZweierTeams() {
        // Team 1 hat Team 2 in einer der Runden geschlagen (13:7), sonst identische Werte
        var team1 = ergebnis(1, 2, 300, 5, 50, gegen(2, 13, 7));
        var team2 = ergebnis(2, 2, 300, 5, 50, gegen(1, 7, 13));

        var sortiert = rechner.sortiere(List.of(team2, team1));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void ohneDirektvergleich_fallbackAufTeamnummer() {
        // Team 2 und Team 1 komplett gleich, haben nie gegeneinander gespielt
        var team2 = ergebnis(2, 2, 300, 5, 50);
        var team1 = ergebnis(1, 2, 300, 5, 50);

        var sortiert = rechner.sortiere(List.of(team2, team1));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void zyklischerDreierGleichstand_fallbackAufTeamnummer() {
        // T1 schlägt T2, T2 schlägt T3, T3 schlägt T1 - alle Basiswerte identisch
        var t1 = ergebnis(1, 1, 200, 0, 20, gegen(2, 13, 7), gegen(3, 7, 13));
        var t2 = ergebnis(2, 1, 200, 0, 20, gegen(3, 13, 7), gegen(1, 7, 13));
        var t3 = ergebnis(3, 1, 200, 0, 20, gegen(1, 13, 7), gegen(2, 7, 13));

        var sortiert = rechner.sortiere(List.of(t3, t1, t2));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2, 3);
    }

    @Test
    void freilosTeamOhneSpielErgebnisse_wirdKorrektEinsortiert() {
        var mitFreilos = ergebnis(1, 3, 378, 0, 0); // 3 Siege, 3x126 durch Freilose
        var ohneFreilos = ergebnis(2, 2, 238, 0, 0);

        var sortiert = rechner.sortiere(List.of(ohneFreilos, mitFreilos));

        assertThat(sortiert).extracting(FormuleXTeamErgebnis::teamNr).containsExactly(1, 2);
    }
}
