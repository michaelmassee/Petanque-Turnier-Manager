/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.basesheet.meldeliste.MeldungenSpalte;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class MeldeListeSheet_NeuerSpieltag extends AbstractSupermeleeMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_NeuerSpieltag.class);

	public MeldeListeSheet_NeuerSpieltag(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		naechsteSpieltag();
	}

	public void naechsteSpieltag() throws GenerateException {
		int anzSpieltage = countAnzSpieltageInMeldeliste();
		getKonfigurationSheet().setAktiveSpieltag(SpielTagNr.from(anzSpieltage + 1));
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		getKonfigurationSheet().setAktiveSpielRunde(SpielRundeNr.from(1));

		RangePosition cleanUpRange = RangePosition.from(aktuelleSpieltagSpalte(), ERSTE_HEADER_ZEILE, aktuelleSpieltagSpalte(), MeldungenSpalte.MAX_ANZ_MELDUNGEN);
		RangeHelper.from(getXSpreadSheet(), cleanUpRange).clearRange();
		upDateSheet();

	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
