/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;

public interface IRangliste extends ISheet {

	int getErsteSummeSpalte() throws GenerateException;

	int getLetzteSpalte() throws GenerateException;

	int getLetzteDatenZeile() throws GenerateException;

	int getErsteDatenZiele() throws GenerateException;

	int getManuellSortSpalte() throws GenerateException;

}
