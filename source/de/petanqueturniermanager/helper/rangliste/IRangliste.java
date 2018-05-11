/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.IEndSummeSpalten;

public interface IRangliste extends ISheet, IEndSummeSpalten {

	int getLetzteSpalte() throws GenerateException;

	public int getLetzteDatenZeile() throws GenerateException;

	int getErsteDatenZiele() throws GenerateException;

	int getManuellSortSpalte() throws GenerateException;

}
