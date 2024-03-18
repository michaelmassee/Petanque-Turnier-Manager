/**
 * Erstellung : 01.03.2024 / Michael Massee
 **/

package de.petanqueturniermanager.schweizer.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;

public class SchweizerMeldeListeSheetUpdate extends AbstractSchweizerMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(SchweizerMeldeListeSheetUpdate.class);

	public SchweizerMeldeListeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).hideGrid().tabColor(SHEET_COLOR)
				.setDocVersionWhenNew().create().isDidCreate()) {
			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
