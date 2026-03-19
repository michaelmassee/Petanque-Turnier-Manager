/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.List;

/**
 * Berechnet die Gruppengrößen für die Aufteilung aller Teams auf A-, B-, C-Turniere
 * nach der Vorrunde.
 *
 * <p>Zwei Szenarien für den „Rest" (letztes, nicht vollständig besetztes Teilnehmerfeld):
 * <ul>
 *   <li><b>Szenario 1</b> – Rest ≥ {@code minRestGroesse}: eigenes Folgeturnier (Cadrage dort)</li>
 *   <li><b>Szenario 2</b> – Rest &lt; {@code minRestGroesse} (und &gt; 0): Rest in letztes
 *       volles Turnier falten, Cadrage dort spielen</li>
 * </ul>
 */
public class GruppenAufteilungRechner {

    private GruppenAufteilungRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet die Gruppengrößen für die Aufteilung auf A-, B-, C-Turniere.
     *
     * @param anzTeams          Gesamtanzahl Teams (muss &gt; 0 sein)
     * @param maxGruppenGroesse Maximale Gruppengröße (Zweierpotenz, z. B. 16)
     * @param minRestGroesse    Mindestzahl für ein eigenes Folgeturnier (Zweierpotenz)
     * @return unveränderliche Liste der Gruppengrößen (Index 0 = Gruppe A usw.)
     * @throws IllegalArgumentException wenn {@code maxGruppenGroesse} oder
     *                                  {@code minRestGroesse} keine Zweierpotenz sind
     *                                  oder {@code anzTeams} &lt;= 0
     */
    public static List<Integer> berechne(int anzTeams, int maxGruppenGroesse, int minRestGroesse) {
        checkArgument(anzTeams > 0, "anzTeams muss groesser als 0 sein");
        checkArgument(Integer.bitCount(maxGruppenGroesse) == 1,
                "maxGruppenGroesse muss Zweierpotenz sein, war: %s", maxGruppenGroesse);
        checkArgument(Integer.bitCount(minRestGroesse) == 1,
                "minRestGroesse muss Zweierpotenz sein, war: %s", minRestGroesse);

        int volleGruppen = anzTeams / maxGruppenGroesse;
        int rest = anzTeams % maxGruppenGroesse;

        List<Integer> ergebnis = new ArrayList<>();
        for (int i = 0; i < volleGruppen; i++) {
            ergebnis.add(maxGruppenGroesse);
        }

        if (rest == 0) {
            // perfekt aufgeteilt – keine Cadrage nötig
        } else if (rest >= minRestGroesse) {
            // Szenario 1: Rest bekommt eigenes Turnier (Cadrage dort)
            ergebnis.add(rest);
        } else {
            // Szenario 2: Rest in letztes volles Turnier falten (Cadrage dort)
            if (ergebnis.isEmpty()) {
                // Sonderfall: rest < minRestGroesse, aber keine vollen Gruppen
                ergebnis.add(rest);
            } else {
                ergebnis.set(ergebnis.size() - 1, maxGruppenGroesse + rest);
            }
        }
        return ergebnis;
    }
}
