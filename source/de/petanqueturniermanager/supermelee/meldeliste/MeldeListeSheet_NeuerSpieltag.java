/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class MeldeListeSheet_NeuerSpieltag extends AbstractSupermeleeMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_NeuerSpieltag.class);

	public MeldeListeSheet_NeuerSpieltag(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		naechsteSpieltag();
	}

	public void naechsteSpieltag() throws GenerateException {
		int anzSpieltage = countAnzSpieltageInMeldeliste();
		getKonfigurationSheet().setAktiveSpieltag(new SpielTagNr(anzSpieltage + 1));
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		getKonfigurationSheet().setAktiveSpielRunde(1);

		RangePosition cleanUpRange = RangePosition.from(aktuelleSpieltagSpalte(), ERSTE_HEADER_ZEILE, aktuelleSpieltagSpalte(), 999);
		getSheetHelper().clearRange(getSheet(), cleanUpRange);

		upDateSheet();

	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
