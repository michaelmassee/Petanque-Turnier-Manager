/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;

public class MeldeListeSheet_New extends AbstractMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_New.class);

	public MeldeListeSheet_New(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		int anzSpieltage = countAnzSpieltage();
		getKonfigurationSheet().setAktuelleSpieltag(anzSpieltage + 1);
		getKonfigurationSheet().setAktuelleSpielRunde(1);

		RangePosition cleanUpRange = RangePosition.from(aktuelleSpieltagSpalte(), HEADER_ZEILE,
				aktuelleSpieltagSpalte(), 999);
		getSheetHelper().clearRange(getSheet(), cleanUpRange);

		upDateSheet();
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
