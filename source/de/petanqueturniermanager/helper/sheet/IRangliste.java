/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;

public interface IRangliste extends ISheet, IEndSummeSpalten {

	int getLetzteSpalte() throws GenerateException;

	public int letzteDatenZeile() throws GenerateException;

	int getErsteDatenZiele() throws GenerateException;

}
