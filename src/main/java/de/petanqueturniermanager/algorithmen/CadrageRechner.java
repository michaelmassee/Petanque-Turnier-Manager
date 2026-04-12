/**
 * Erstellung 23.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Berechnet die Cadrage-Parameter fuer ein K.o.-Turnier.<br>
 * <br>
 * Ein K.o.-Turnierbaum erfordert eine Zweierpotenz als Teilnehmerzahl (2, 4, 8, 16, 32, …).
 * Ist die Gesamtzahl der Teams keine Zweierpotenz, wird eine Vorrunde (Cadrage) gespielt,
 * um das Feld auf die naechste kleinere Zweierpotenz zu reduzieren.<br>
 * <br>
 * Beispiel: 10 Teams → Zielfeld 8 → 4 Teams spielen Cadrage (2 Spiele),
 * 6 Teams erhalten ein Freilos und starten direkt in der Hauptrunde.
 *
 * @author Michael Massee
 */
public class CadrageRechner {

	private final int gesamtTeams;

	public CadrageRechner(int gesamtTeams) {
		checkArgument(gesamtTeams >= 2, "gesamtTeams muss mindestens 2 sein, war: %s", gesamtTeams);
		this.gesamtTeams = gesamtTeams;
	}

	/**
	 * die anzahl der Teams die Cadrage spielen muessen.<br>
	 * Formel: (gesamtTeams - zielAnzahlTeams) * 2<br>
	 * Ergibt 0 wenn gesamtTeams bereits eine Zweierpotenz ist (keine Cadrage noetig).
	 *
	 * @return Anzahl Cadrage-Teams (gerade Zahl oder 0)
	 */
	public int anzTeams() {
		return (gesamtTeams - zielAnzahlTeams()) * 2;
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
	 * Groesste Zweierpotenz &lt;= gesamtTeams – das ist die Zielgroesse des Feldes nach der Cadrage.<br>
	 * Verwendet Integer.highestOneBit(), das ohne Schleife und ohne hartcodierte Obergrenze arbeitet.
	 *
	 * @return Ziel-Teamanzahl (Zweierpotenz)
	 */
	public int zielAnzahlTeams() {
		return Integer.highestOneBit(gesamtTeams);
	}
}
