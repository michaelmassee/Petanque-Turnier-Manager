/**
 * Erstellung 23.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * @author Michael Massee
 *
 */
public class CadrageRechner {

	private final int gesanzTeams;

	public CadrageRechner(int gesanzTeams) {
		checkArgument(gesanzTeams > 2);
		this.gesanzTeams = gesanzTeams;
	}

	/**
	 * die anzahl der Teams die Cadrage Spielen mussen
	 *
	 * @return
	 */

	public int anzTeams() {
		// 2,4,8,16,32
		int zielAnzahlTeams = zielAnzahlTeams();
		int anzTeams = (gesanzTeams - zielAnzahlTeams) * 2;
		return (anzTeams > 0) ? anzTeams : 0;
	}

	/**
	 * die ziel anzahl nach der Cadrage
	 *
	 * @return
	 */
	public int zielAnzahlTeams() {
		// 2,4,8,16,32,64,128
		int zielAnzahl = 0;
		for (int exponent = 7; exponent > 0; exponent--) {
			zielAnzahl = (int) Math.pow(2, exponent);
			if (zielAnzahl <= gesanzTeams) {
				break;
			}
		}
		return zielAnzahl;
	}
}
