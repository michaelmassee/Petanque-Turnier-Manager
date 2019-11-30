/**
 * Erstellung 30.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.model;

/**
 * @author Michael Massee
 *
 */
public class Meldung extends NrComparable<Meldung> implements TurnierDaten {

	// entweder ein Spieler oder Team

	// MeldungNr

	/**
	 * @param nr
	 */
	public Meldung(int nr) {
		super(nr);
	}

}
