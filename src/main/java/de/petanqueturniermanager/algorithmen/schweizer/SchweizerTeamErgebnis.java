package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.List;

/**
 * Hält die Auswertungsdaten eines Teams für das Schweizer Turniersystem.
 * <p>
 * Wird als Eingabe für die Sortierung nach Auswertungskriterien und für die Paarung der nächsten Runde verwendet.
 *
 * @param teamNr                 Nummer des Teams
 * @param siege                  Anzahl gewonnener Spiele (Hauptkriterium)
 * @param punktedifferenz        Erzielte minus kassierte Punkte (über alle Spiele)
 * @param erzieltePunkte         Erzielte Punkte (Punkte+, für Ranking ohne Buchholz)
 * @param gegnerNrn              Team-Nummern aller bisherigen Gegner (für BHZ/FBHZ-Berechnung)
 * @param heruntergelostInRunden Runden, in denen das Team heruntergelost wurde (siehe {@link HerunterlosChronik})
 */
public record SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
		List<Integer> gegnerNrn, List<Integer> heruntergelostInRunden) {

	public SchweizerTeamErgebnis {
		heruntergelostInRunden = List.copyOf(heruntergelostInRunden);
	}

	/**
	 * Teamergebnis ohne bekannte Herunterlosungen.
	 */
	public SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
			List<Integer> gegnerNrn) {
		this(teamNr, siege, punktedifferenz, erzieltePunkte, gegnerNrn, List.of());
	}
}
