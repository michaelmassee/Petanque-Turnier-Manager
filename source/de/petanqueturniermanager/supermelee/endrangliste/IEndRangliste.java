/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.endrangliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.IRangliste;

public interface IEndRangliste extends IRangliste {

	int getAnzahlSpieltage() throws GenerateException;

	public int getErsteSummeSpalte() throws GenerateException;

}
