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
	 * die anzahl der Teams die Cadrage spielen muessen.<br>
	 * Formel: (gesanzTeams - zielAnzahlTeams) * 2<br>
	 * Ergibt 0 wenn gesanzTeams bereits eine Zweierpotenz ist (keine Cadrage noetig).
	 *
	 * @return Anzahl Cadrage-Teams (gerade Zahl oder 0)
	 */
	public int anzTeams() {
		return (gesanzTeams - zielAnzahlTeams()) * 2;
	}

	/**
	 * Anzahl Teams mit Freilos (kein Cadrage-Spiel, direkt in die Hauptrunde).<br>
	 * Formel: zielAnzahlTeams - anzTeams/2
	 *
	 * @return Anzahl Freilos-Teams
	 */
	public int anzFreilose() {
		return zielAnzahlTeams() - anzTeams() / 2;
	}

	/**
	 * Groesste Zweierpotenz &lt;= gesanzTeams – das ist die Zielgroesse des Feldes nach der Cadrage.<br>
	 * Verwendet Integer.highestOneBit(), das ohne Schleife und ohne hartcodierte Obergrenze arbeitet.
	 *
	 * @return Ziel-Teamanzahl (Zweierpotenz)
	 */
	public int zielAnzahlTeams() {
		return Integer.highestOneBit(gesanzTeams);
	}
}
