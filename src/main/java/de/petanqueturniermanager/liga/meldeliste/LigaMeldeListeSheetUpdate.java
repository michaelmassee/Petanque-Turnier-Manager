/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

public class LigaMeldeListeSheetUpdate extends AbstractLigaMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheetUpdate.class);

	public LigaMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
}
