/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import de.petanqueturniermanager.exception.GenerateException;

public interface ISpielTagRangliste extends IRangliste {
	int getAnzahlRunden() throws GenerateException;
}
