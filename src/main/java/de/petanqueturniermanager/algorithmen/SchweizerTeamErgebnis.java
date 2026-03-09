package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Hält die Auswertungsdaten eines Teams für das Schweizer Turniersystem.
 * <p>
 * Wird als Eingabe für die Sortierung nach Auswertungskriterien verwendet.
 *
 * @param teamNr          Nummer des Teams
 * @param siege           Anzahl gewonnener Spiele (Hauptkriterium)
 * @param punktedifferenz  Erzielte minus kassierte Punkte (über alle Spiele)
 * @param erzieltePunkte  Erzielte Punkte (Punkte+, für Ranking ohne Buchholz)
 * @param gegnerNrn       Team-Nummern aller bisherigen Gegner (für BHZ/FBHZ-Berechnung)
 */
public record SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
		List<Integer> gegnerNrn) {
}
