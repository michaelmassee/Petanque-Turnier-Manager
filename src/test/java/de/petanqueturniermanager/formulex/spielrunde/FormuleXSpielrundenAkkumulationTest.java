/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.formulex.spielrunde;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.petanqueturniermanager.algorithmen.formulex.FormuleXErgebnis;
import de.petanqueturniermanager.algorithmen.formulex.FormuleXTeamErgebnis;
import de.petanqueturniermanager.formulex.spielrunde.FormuleXAbstractSpielrundeSheet.SpielrundenAkkumulation;

/**
 * Unit-Test für {@link SpielrundenAkkumulation#sortiertNachRangliste()} – stellt sicher,
 * dass die Paarung der nächsten Runde dieselben Kriterien nutzt wie die Rangliste
 * (Siege → Wertung → Punktedifferenz → eigene Punkte → Direktvergleich → Teamnummer).
 */
class FormuleXSpielrundenAkkumulationTest {

    @Test
    void sieganzahl_bestimmtPaarungsreihenfolgeVorWertung() {
        // Team 1: 3 Siege, weniger Wertung – muss trotzdem vor Team 2 (2 Siege, mehr Wertung) stehen
        var team1 = new FormuleXErgebnis(1, 40, 20, List.of(2, 3, 4), false);
        var team2 = new FormuleXErgebnis(2, 45, 25, List.of(1, 3, 4), false);

        var akkumulierung = new SpielrundenAkkumulation(
                List.of(team2, team1),
                Map.of(1, 300, 2, 500),
                Map.of(1, 3, 2, 2),
                Map.of(
                        1, List.of(new FormuleXTeamErgebnis.SpielErgebnisGegen(2, 13, 5)),
                        2, List.of(new FormuleXTeamErgebnis.SpielErgebnisGegen(1, 5, 13))));

        List<FormuleXErgebnis> sortiert = akkumulierung.sortiertNachRangliste();

        assertThat(sortiert).extracting(FormuleXErgebnis::teamNr).containsExactly(1, 2);
    }

    @Test
    void ohneSpiele_fallbackAufTeamnummer() {
        var team2 = new FormuleXErgebnis(2, 0, 0, List.of(), false);
        var team1 = new FormuleXErgebnis(1, 0, 0, List.of(), false);

        var akkumulierung = new SpielrundenAkkumulation(
                List.of(team2, team1), Map.of(1, 0, 2, 0), Map.of(1, 0, 2, 0), Map.of(1, List.of(), 2, List.of()));

        List<FormuleXErgebnis> sortiert = akkumulierung.sortiertNachRangliste();

        assertThat(sortiert).extracting(FormuleXErgebnis::teamNr).containsExactly(1, 2);
    }
}
