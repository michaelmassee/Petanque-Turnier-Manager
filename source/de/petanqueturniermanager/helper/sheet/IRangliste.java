/**
* Erstellung : 14.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;

public interface IRangliste extends ISheet {

	int getAnzahlRunden() throws GenerateException;

	public int getErsteSummeSpalte() throws GenerateException;

}
