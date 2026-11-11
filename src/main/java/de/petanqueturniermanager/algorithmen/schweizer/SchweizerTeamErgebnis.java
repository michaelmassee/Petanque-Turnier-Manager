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
 * @param heruntergelostInRunden Runden, in denen das Team heruntergelost wurde, inkl. Freilos-Runden
 *                               (siehe {@link HerunterlosChronik})
 * @param anzFreilose            Freilose in gewerteten Runden mit Ergebnissen (für den Freilos-Buchholz, siehe
 *                               {@link FreilosBuchholz})
 * @param freilosInRunden        aufsteigende Rundennummern aller ausgelosten, gewerteten Runden mit Freilos – auch
 *                               solche ohne eingetragenes Ergebnis (für die Freilos-Spalte der Rangliste)
 */
public record SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
		List<Integer> gegnerNrn, List<Integer> heruntergelostInRunden, int anzFreilose,
		List<Integer> freilosInRunden) {

	public SchweizerTeamErgebnis {
		heruntergelostInRunden = List.copyOf(heruntergelostInRunden);
		freilosInRunden = List.copyOf(freilosInRunden);
	}

	/**
	 * Teamergebnis ohne Angabe der Freilos-Runden.
	 */
	public SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
			List<Integer> gegnerNrn, List<Integer> heruntergelostInRunden, int anzFreilose) {
		this(teamNr, siege, punktedifferenz, erzieltePunkte, gegnerNrn, heruntergelostInRunden, anzFreilose,
				List.of());
	}

	/**
	 * Teamergebnis ohne Freilos.
	 */
	public SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
			List<Integer> gegnerNrn, List<Integer> heruntergelostInRunden) {
		this(teamNr, siege, punktedifferenz, erzieltePunkte, gegnerNrn, heruntergelostInRunden, 0, List.of());
	}

	/**
	 * Teamergebnis ohne bekannte Herunterlosungen und ohne Freilos.
	 */
	public SchweizerTeamErgebnis(int teamNr, int siege, int punktedifferenz, int erzieltePunkte,
			List<Integer> gegnerNrn) {
		this(teamNr, siege, punktedifferenz, erzieltePunkte, gegnerNrn, List.of(), 0, List.of());
	}
}
