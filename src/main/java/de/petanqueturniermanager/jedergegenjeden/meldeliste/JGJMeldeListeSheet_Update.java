package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;

/**
 * Erstellung 01.08.2022 / Michael Massee
 */

public class JGJMeldeListeSheet_Update extends AbstractJGJMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(JGJMeldeListeSheet_Update.class);

	public JGJMeldeListeSheet_Update(WorkingSpreadsheet workingSpreadsheet) {
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
