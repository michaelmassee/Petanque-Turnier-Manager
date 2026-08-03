/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.formulex;

import java.util.List;
import java.util.Objects;

/**
 * Hält die über alle Spielrunden aggregierten Auswertungsdaten eines Teams für die
 * Formule-X-Rangliste.
 *
 * <p>Wird als Eingabe für {@link FormuleXRanglisteRechner#sortiere(List)} verwendet.
 *
 * @param teamNr          Nummer des Teams
 * @param siege           Anzahl gewonnener Spiele inkl. Freilose (Hauptkriterium)
 * @param wertung         Formule-X-Wertungssumme über alle Runden (Feinwertung)
 * @param punktedifferenz Erzielte minus kassierte Spielpunkte über alle Runden
 * @param eigenePunkte    Erzielte Spielpunkte über alle Runden
 * @param spielErgebnisse Alle Einzelergebnisse gegen Gegner (ohne Freilose), unveränderlich
 */
public record FormuleXTeamErgebnis(
        int teamNr,
        int siege,
        int wertung,
        int punktedifferenz,
        int eigenePunkte,
        List<SpielErgebnisGegen> spielErgebnisse) {

    /**
     * Einzelergebnis eines Spiels gegen einen konkreten Gegner (für den Direktvergleich).
     *
     * @param gegnerNr      Teamnummer des Gegners
     * @param eigeneSpunkte Erzielte Spielpunkte dieses Teams in diesem Spiel
     * @param gegnerSpunkte Erzielte Spielpunkte des Gegners in diesem Spiel
     */
    public record SpielErgebnisGegen(int gegnerNr, int eigeneSpunkte, int gegnerSpunkte) {}

    /** Kompakter Konstruktor: schützende Kopie der Spielergebnisliste. */
    public FormuleXTeamErgebnis {
        Objects.requireNonNull(spielErgebnisse, "spielErgebnisse");
        spielErgebnisse = List.copyOf(spielErgebnisse);
    }
}
