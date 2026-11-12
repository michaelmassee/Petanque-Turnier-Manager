/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

/**
 * Bestimmt, welchen Buchholz-Beitrag ein Freilos im Schweizer System liefert (siehe 03_Schweizer.md, 5.1).
 */
public enum SchweizerFreilosBuchholzModus {
	/** BPV-Regel: Ein Freilos ist kein Gegner und liefert 0 BHZ und 0 FBHZ. */
	NULL,
	/**
	 * PTM-Regel: Das Freilos zählt als fiktiver Durchschnittsgegner, der gegen das eigene Team verliert und in den
	 * übrigen Runden eine ausgeglichene Bilanz hat (abgerundet, keine halben Punkte).
	 */
	NEUTRAL
}
