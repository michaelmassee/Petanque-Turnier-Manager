/**
 * Erstellung : 30.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class MeldeListeSheet_New extends AbstractSupermeleeMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_New.class);

	public MeldeListeSheet_New(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(this, SHEETNAME).pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).hideGrid().setActiv()
				.setDocVersionWhenNew().create().isDidCreate()) {
			SpielTagNr spielTag1 = new SpielTagNr(1);
			setSpielTag(spielTag1);
			getKonfigurationSheet().setAktiveSpieltag(spielTag1);
			getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(1));
			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
}
