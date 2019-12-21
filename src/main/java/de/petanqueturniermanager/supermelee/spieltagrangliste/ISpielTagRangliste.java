/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.rangliste.IRangliste;

public interface ISpielTagRangliste extends IRangliste {
	int getAnzahlRunden() throws GenerateException;
}
