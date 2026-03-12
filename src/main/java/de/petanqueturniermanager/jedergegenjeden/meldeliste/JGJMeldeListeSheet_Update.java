package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJMeldeListeSheet_Update extends AbstractJGJMeldeListeSheet {

	public JGJMeldeListeSheet_Update(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
	}

}
