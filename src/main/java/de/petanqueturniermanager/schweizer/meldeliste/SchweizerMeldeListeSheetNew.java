/**
 * Erstellung : 01.03.2024 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerSheet;

public class SchweizerMeldeListeSheetNew extends AbstractSchweizerMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetNew.class);

	public SchweizerMeldeListeSheetNew(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), SchweizerSheet.TURNIERSYSTEM)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude();

	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
