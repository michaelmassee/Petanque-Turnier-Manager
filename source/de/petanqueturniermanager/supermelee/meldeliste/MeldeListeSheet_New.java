/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.DefaultSheetPos;
import de.petanqueturniermanager.helper.sheet.NewSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class MeldeListeSheet_New extends AbstractMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_New.class);

	public MeldeListeSheet_New(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		SpielTagNr spielTag1 = new SpielTagNr(1);
		if (NewSheet.from(getxContext(), SHEETNAME).pos(DefaultSheetPos.MELDELISTE).tabColor(SHEET_COLOR).create()) {
			setSpielTag(spielTag1);
			getKonfigurationSheet().setAktiveSpieltag(spielTag1);
			getKonfigurationSheet().setAktiveSpielRunde(1);
			upDateSheet();
		}
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
