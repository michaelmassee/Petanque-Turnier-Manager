/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import java.util.List;
import java.util.Objects;

/**
 * Hält die Auswertungsdaten eines Teams innerhalb einer Poule.
 *
 * <p>Wird als Eingabe für {@link PouleRanglisteRechner#sortiere(List)} verwendet.
 *
 * @param teamNr          Nummer des Teams
 * @param siege           Anzahl gewonnener Spiele (Hauptkriterium)
 * @param niederlagen     Anzahl verlorener Spiele
 * @param punkteDifferenz Erzielte minus kassierte Spielpunkte (über alle Spiele)
 * @param erzieltePunkte  Erzielte Spielpunkte (Punkte+)
 * @param spielErgebnisse Alle Einzelergebnisse gegen Gegner, unveränderlich gespeichert
 */
public record PouleTeamErgebnis(
        int teamNr,
        int siege,
        int niederlagen,
        int punkteDifferenz,
        int erzieltePunkte,
        List<SpielErgebnisGegen> spielErgebnisse) {

    /**
     * Einzelergebnis eines Spiels gegen einen konkreten Gegner.
     *
     * @param gegnerNr      Teamnummer des Gegners
     * @param eigeneSpunkte Erzielte Spielpunkte dieses Teams in diesem Spiel
     * @param gegnerSpunkte Erzielte Spielpunkte des Gegners in diesem Spiel
     */
    public record SpielErgebnisGegen(int gegnerNr, int eigeneSpunkte, int gegnerSpunkte) {}

    /** Kompakter Konstruktor: schützende Kopie der Spielergebnisliste. */
    public PouleTeamErgebnis {
        Objects.requireNonNull(spielErgebnisse, "spielErgebnisse");
        spielErgebnisse = List.copyOf(spielErgebnisse);
    }

    /**
     * Gibt alle Gegner-Teamnummern zurück, abgeleitet aus den Spielergebnissen.
     *
     * @return unveränderliche Liste der Gegner-Nummern in der Reihenfolge der Spiele
     */
    public List<Integer> gegnerNrn() {
        return spielErgebnisse.stream()
                .map(SpielErgebnisGegen::gegnerNr)
                .toList();
    }
}
