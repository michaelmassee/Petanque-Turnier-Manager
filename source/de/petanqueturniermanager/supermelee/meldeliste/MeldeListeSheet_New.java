/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.NewSheet;

public class MeldeListeSheet_New extends AbstractMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_New.class);

	public MeldeListeSheet_New(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (NewSheet.from(getxContext(), SHEETNAME).pos(SHEET_POS).tabColor(SHEET_COLOR).create()) {
			getKonfigurationSheet().setAktuelleSpieltag(1);
			getKonfigurationSheet().setAktuelleSpielRunde(1);
			upDateSheet();
		}
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
