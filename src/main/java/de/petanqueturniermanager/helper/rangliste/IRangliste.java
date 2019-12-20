/**
* Erstellung : 10.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.rangliste;

import java.util.List;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.Position;

public interface IRangliste extends ISheet {

	int getErsteSummeSpalte() throws GenerateException;

	int getLetzteSpalte() throws GenerateException;

	int getLetzteDatenZeile() throws GenerateException;

	int getErsteDatenZiele() throws GenerateException;

	int getManuellSortSpalte() throws GenerateException;

	List<Position> getRanglisteSpalten() throws GenerateException;

}
