/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Sortiert die Teamergebnisse innerhalb einer Poule nach den Auswertungskriterien.
 *
 * <p>Kriterien in absteigender Priorität:
 * <ol>
 *   <li>Siege (absteigend)</li>
 *   <li>Niederlagen (aufsteigend)</li>
 *   <li>Punktedifferenz (absteigend)</li>
 *   <li>Direktvergleich – nur bei genau 2 gleichrangigen Teams (nach Kriterien 1–3)</li>
 *   <li>Teamnummer (aufsteigend) – garantiert stabile, totale Ordnung als Fallback</li>
 * </ol>
 *
 * <p>Bei einem zyklischen Dreier-Gleichstand (A schlägt B, B schlägt C, C schlägt A)
 * ist der Direktvergleich nicht anwendbar. Die Sortierung nach Teamnummer (Kriterium 5)
 * liefert in diesem Fall eine deterministische Ordnung.
 *
 * <p>Der Algorithmus arbeitet in zwei Phasen:
 * <ol>
 *   <li><b>Phase 1:</b> Sortierung nach Kriterien 1–3 + Teamnummer (totale Ordnung).</li>
 *   <li><b>Phase 2:</b> Verbesserung per Direktvergleich – nur für Gruppen mit genau
 *       2 gleichrangigen Teams. Bei 3 oder mehr gleichrangigen Teams (Zyklus) bleibt
 *       die Teamnummer-Ordnung aus Phase 1 erhalten.</li>
 * </ol>
 */
public class PouleRanglisteRechner {

    /**
     * Sortiert die übergebenen Teamergebnisse nach den Poule-Auswertungskriterien.
     *
     * <p>Gilt sowohl für 3er- als auch für 4er-Poules.
     *
     * @param ergebnisse Liste der Teamergebnisse (darf leer sein)
     * @return neue, unveränderliche Liste in aufsteigender Platzierungsreihenfolge (Platz 1 zuerst)
     */
    public List<PouleTeamErgebnis> sortiere(List<PouleTeamErgebnis> ergebnisse) {
        if (ergebnisse.isEmpty()) {
            return List.of();
        }
        int[][] paarungen = bauePaarungen(ergebnisse);
        int[][] siege = baueSiege(ergebnisse);
        int[][] spielPunkte = baueSpielPunkte(ergebnisse);

        // Phase 1: stabile Basissortierung mit totalem Tiebreaker (teamNr)
        Comparator<PouleTeamErgebnis> basisKomparator =
                Comparator.comparingInt(PouleTeamErgebnis::siege).reversed()
                        .thenComparingInt(PouleTeamErgebnis::niederlagen)
                        .thenComparing(
                                Comparator.comparingInt(PouleTeamErgebnis::punkteDifferenz).reversed())
                        .thenComparingInt(PouleTeamErgebnis::teamNr);

        var sortiert = new ArrayList<>(ergebnisse.stream().sorted(basisKomparator).toList());

        // Phase 2: Direktvergleich für Zweier-Gruppen verbessern
        verbessereDurchDirektvergleich(sortiert, paarungen, siege, spielPunkte);

        return List.copyOf(sortiert);
    }

    /**
     * Verbessert die Reihenfolge gleichrangiger Zweier-Gruppen per Direktvergleich.
     * Bei 3 oder mehr gleichrangigen Teams (möglicher Zyklus) bleibt die Teamnummer-Ordnung.
     */
    private void verbessereDurchDirektvergleich(List<PouleTeamErgebnis> sortiert,
            int[][] paarungen, int[][] siege, int[][] spielPunkte) {
        int n = sortiert.size();
        int start = 0;
        while (start < n) {
            int end = start + 1;
            while (end < n && sindGleichrangig(sortiert.get(start), sortiert.get(end))) {
                end++;
            }
            if (end - start == 2) {
                var a = sortiert.get(start);
                var b = sortiert.get(end - 1);
                int komparatorWert = new Direktvergleich(a.teamNr(), b.teamNr(), paarungen, siege, spielPunkte)
                        .calc().toKomparatorWert();
                if (komparatorWert > 0) {
                    sortiert.set(start, b);
                    sortiert.set(end - 1, a);
                }
            }
            start = end;
        }
    }

    private boolean sindGleichrangig(PouleTeamErgebnis a, PouleTeamErgebnis b) {
        return a.siege() == b.siege()
                && a.niederlagen() == b.niederlagen()
                && a.punkteDifferenz() == b.punkteDifferenz();
    }

    /**
     * Baut die Paarungen-Matrix für {@link Direktvergleich} auf.
     * Jede {@link PouleTeamErgebnis.SpielErgebnisGegen} ergibt eine Zeile
     * {@code [teamNr, gegnerNr]}.
     */
    private int[][] bauePaarungen(List<PouleTeamErgebnis> ergebnisse) {
        int anzZeilen = zaehleSpiele(ergebnisse);
        int[][] paarungen = new int[anzZeilen][2];
        int idx = 0;
        for (var ergebnis : ergebnisse) {
            for (var spiel : ergebnis.spielErgebnisse()) {
                paarungen[idx][0] = ergebnis.teamNr();
                paarungen[idx][1] = spiel.gegnerNr();
                idx++;
            }
        }
        return paarungen;
    }

    /**
     * Baut die Siege-Matrix für {@link Direktvergleich} auf.
     * Eintrag {@code siege[i][0] = 1} wenn das Team in Zeile {@code i} gewonnen hat.
     */
    private int[][] baueSiege(List<PouleTeamErgebnis> ergebnisse) {
        int anzZeilen = zaehleSpiele(ergebnisse);
        int[][] siege = new int[anzZeilen][2];
        int idx = 0;
        for (var ergebnis : ergebnisse) {
            for (var spiel : ergebnis.spielErgebnisse()) {
                siege[idx][0] = spiel.eigeneSpunkte() > spiel.gegnerSpunkte() ? 1 : 0;
                siege[idx][1] = spiel.gegnerSpunkte() > spiel.eigeneSpunkte() ? 1 : 0;
                idx++;
            }
        }
        return siege;
    }

    /**
     * Baut die Spielpunkte-Matrix für {@link Direktvergleich} auf.
     * Eintrag {@code spielPunkte[i][0]} enthält die eigenen Punkte des Teams in Zeile {@code i}.
     */
    private int[][] baueSpielPunkte(List<PouleTeamErgebnis> ergebnisse) {
        int anzZeilen = zaehleSpiele(ergebnisse);
        int[][] spielPunkte = new int[anzZeilen][2];
        int idx = 0;
        for (var ergebnis : ergebnisse) {
            for (var spiel : ergebnis.spielErgebnisse()) {
                spielPunkte[idx][0] = spiel.eigeneSpunkte();
                spielPunkte[idx][1] = spiel.gegnerSpunkte();
                idx++;
            }
        }
        return spielPunkte;
    }

    private int zaehleSpiele(List<PouleTeamErgebnis> ergebnisse) {
        return ergebnisse.stream().mapToInt(e -> e.spielErgebnisse().size()).sum();
    }
}
