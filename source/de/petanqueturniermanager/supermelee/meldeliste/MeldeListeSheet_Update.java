/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

public class MeldeListeSheet_Update extends AbstractSupermeleeMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_Update.class);

	public MeldeListeSheet_Update(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		upDateSheet();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
}
