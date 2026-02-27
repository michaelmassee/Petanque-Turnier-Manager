package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Hält die Auswertungsdaten eines Teams für das Schweizer Turniersystem.
 * <p>
 * Wird als Eingabe für die Sortierung nach Auswertungskriterien verwendet.
 *
 * @param teamNr          Nummer des Teams
 * @param siege           Anzahl gewonnener Spiele (Hauptkriterium)
 * @param kugeldifferenz  Erzielte minus kassierte Punkte (über alle Spiele)
 * @param gegnerNrn       Team-Nummern aller bisherigen Gegner (für BHZ/FBHZ-Berechnung)
 */
public record SchweizerTeamErgebnis(int teamNr, int siege, int kugeldifferenz, List<Integer> gegnerNrn) {
}
