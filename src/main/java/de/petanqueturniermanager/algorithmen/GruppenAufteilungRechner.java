/*
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
 * <p>Einfache Chunk-Aufteilung: Teams werden nach Rang in Blöcke der Größe
 * {@code maxGruppenGroesse} eingeteilt. Die letzte Gruppe enthält den Rest
 * (auch wenn kleiner). Ein 1-Team-Rest wird in die vorherige Gruppe gefaltet,
 * weil eine Gruppe mit weniger als 2 Teams kein KO bilden kann.
 */
public class GruppenAufteilungRechner {

    private GruppenAufteilungRechner() {
        // Hilfsklasse – kein Instanz-Konstruktor
    }

    /**
     * Berechnet die Gruppengrößen für die Aufteilung auf A-, B-, C-Turniere.
     *
     * @param anzTeams          Gesamtanzahl Teams (muss &gt; 0 sein)
     * @param maxGruppenGroesse Maximale Gruppengröße (z. B. 16)
     * @return unveränderliche Liste der Gruppengrößen (Index 0 = Gruppe A usw.)
     * @throws IllegalArgumentException wenn {@code anzTeams} &lt;= 0 oder
     *                                  {@code maxGruppenGroesse} &lt; 2
     */
    public static List<Integer> berechne(int anzTeams, int maxGruppenGroesse) {
        checkArgument(anzTeams > 0, "anzTeams muss groesser als 0 sein");
        checkArgument(maxGruppenGroesse >= 2, "maxGruppenGroesse muss >= 2 sein, war: %s", maxGruppenGroesse);

        List<Integer> ergebnis = new ArrayList<>();
        for (int start = 0; start < anzTeams; start += maxGruppenGroesse) {
            ergebnis.add(Math.min(maxGruppenGroesse, anzTeams - start));
        }
        // 1-Team-Rest in vorherige Gruppe falten (Gruppen mit < 2 Teams können kein KO bilden)
        if (ergebnis.size() >= 2 && ergebnis.get(ergebnis.size() - 1) < 2) {
            int rest = ergebnis.remove(ergebnis.size() - 1);
            ergebnis.set(ergebnis.size() - 1, ergebnis.get(ergebnis.size() - 1) + rest);
        }
        return ergebnis;
    }
}
