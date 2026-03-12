/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

public class LigaMeldeListeSheetUpdate extends AbstractLigaMeldeListeSheet {

	public LigaMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

}
