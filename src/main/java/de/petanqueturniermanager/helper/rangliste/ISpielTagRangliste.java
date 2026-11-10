/*
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import de.petanqueturniermanager.exception.GenerateException;

public interface ISpielTagRangliste extends IRangliste {
	int getAnzahlRunden() throws GenerateException;

	/**
	 * @return true, wenn die Spielrunde {@code rundeNr} des Spieltags als „nicht gewertet" markiert ist
	 */
	boolean istRundeNichtGewertet(int rundeNr);
}
