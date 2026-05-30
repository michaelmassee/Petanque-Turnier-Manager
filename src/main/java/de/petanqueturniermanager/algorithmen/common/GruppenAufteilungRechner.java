/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

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
     * Delegiert an {@link #berechne(int, int, int)} mit {@code minLetzteGruppeGroesse = 2}
     * (bisheriges Verhalten: nur 1-Team-Rest wird gefaltet).
     *
     * @param anzTeams          Gesamtanzahl Teams (muss &gt; 0 sein)
     * @param maxGruppenGroesse Maximale Gruppengröße (z. B. 16)
     * @return unveränderliche Liste der Gruppengrößen (Index 0 = Gruppe A usw.)
     * @throws IllegalArgumentException wenn {@code anzTeams} &lt;= 0 oder
     *                                  {@code maxGruppenGroesse} &lt; 2
     */
    public static List<Integer> berechne(int anzTeams, int maxGruppenGroesse) {
        return berechne(anzTeams, maxGruppenGroesse, 2);
    }

    /**
     * Berechnet die Gruppengrößen für die Aufteilung auf A-, B-, C-Turniere.
     *
     * <p>Ist die letzte Gruppe kleiner als {@code minLetzteGruppeGroesse}, wird sie in die
     * vorherige Gruppe gefaltet. Die vorherige Gruppe kann dadurch größer als
     * {@code maxGruppenGroesse} werden — Cadrage übernimmt dann den Ausgleich auf eine
     * Zweierpotenz.
     *
     * <p>Beispiel: 35 Teams, max=16, minLetzte=4 → [16, 16, 3] → 3 &lt; 4 → [16, 19]
     *
     * @param anzTeams               Gesamtanzahl Teams (muss &gt; 0 sein)
     * @param maxGruppenGroesse      Maximale Gruppengröße (z. B. 16)
     * @param minLetzteGruppeGroesse Mindestgröße der letzten Gruppe; ist sie kleiner,
     *                               wird sie in die vorherige Gruppe gefaltet (muss &gt;= 2 sein)
     * @return unveränderliche Liste der Gruppengrößen (Index 0 = Gruppe A usw.)
     * @throws IllegalArgumentException wenn Vorbedingungen verletzt sind
     */
    public static List<Integer> berechne(int anzTeams, int maxGruppenGroesse, int minLetzteGruppeGroesse) {
        checkArgument(anzTeams > 0, "anzTeams muss groesser als 0 sein");
        checkArgument(maxGruppenGroesse >= 2, "maxGruppenGroesse muss >= 2 sein, war: %s", maxGruppenGroesse);
        checkArgument(minLetzteGruppeGroesse >= 2, "minLetzteGruppeGroesse muss >= 2 sein, war: %s", minLetzteGruppeGroesse);

        List<Integer> ergebnis = new ArrayList<>();
        for (int start = 0; start < anzTeams; start += maxGruppenGroesse) {
            ergebnis.add(Math.min(maxGruppenGroesse, anzTeams - start));
        }
        if (ergebnis.size() >= 2 && ergebnis.getLast() < minLetzteGruppeGroesse) {
            int rest = ergebnis.removeLast();
            ergebnis.set(ergebnis.size() - 1, ergebnis.getLast() + rest);
        }
        return ergebnis;
    }
}
