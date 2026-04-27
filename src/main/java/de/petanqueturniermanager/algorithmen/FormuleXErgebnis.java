package de.petanqueturniermanager.algorithmen;

import java.util.List;

/**
 * Hält die Auswertungsdaten eines Teams für das Formule X Turniersystem.
 * <p>
 * Wird als Eingabe für die Sortierung nach Wertungspunkten verwendet.
 * Der eigentliche Wertungsscore wird funktional berechnet ({@code berechneWertung()})
 * und ist bewusst NICHT Teil dieses Records.
 *
 * @param teamNr          Nummer des Teams
 * @param eigenePunkte    Erzielte Punkte im letzten Spiel
 * @param kassiertePunkte Kassierte Punkte im letzten Spiel
 * @param gegnerNrn       Team-Nummern aller bisherigen Gegner (für Rematch-Prüfung)
 * @param hatteFreilos    true wenn das Team in der aktuellen Runde ein Freilos (BYE) hatte
 */
public record FormuleXErgebnis(int teamNr, int eigenePunkte, int kassiertePunkte,
		List<Integer> gegnerNrn, boolean hatteFreilos) {

	/**
	 * Prüft ob dieses Team gewonnen hat.
	 * <p>
	 * Ein Team gewinnt wenn:
	 * <ul>
	 *   <li>es ein Freilos hatte (BYE zählt als Sieg), oder</li>
	 *   <li>es mehr eigene Punkte als kassierte Punkte erzielt hat</li>
	 * </ul>
	 *
	 * @return true wenn Sieg, false wenn Niederlage
	 */
	public boolean istSieger() {
		return hatteFreilos || eigenePunkte > kassiertePunkte;
	}

	/**
	 * Berechnet die Punktedifferenz aus eigenen und kassierten Punkten.
	 *
	 * @return eigenePunkte - kassiertePunkte (positiv bei Sieg, negativ bei Niederlage)
	 */
	public int punktedifferenz() {
		return eigenePunkte - kassiertePunkte;
	}
}