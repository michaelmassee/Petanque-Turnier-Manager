/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

public class SpielrundeSheet_Naechste extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_Naechste.class);

	public SpielrundeSheet_Naechste(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		if (naechsteSpielrundeEinfuegen()) {
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag()); // validieren
			// sicher gehen das aktive spielrunde sheet ist activ
			getSheetHelper().setActiveSheet(getXSpreadSheet());
		}
	}

	public boolean naechsteSpielrundeEinfuegen() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNr(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		SpielerMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return false;
		}

		// aktuelle vorhanden ?
		int neueSpielrunde = aktuelleSpielrunde.getNr();
		if (getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr())) != null) {
			neueSpielrunde++;
		}

		gespieltenRundenEinlesen(aktiveMeldungen, getKonfigurationSheet().getSpielRundeNeuAuslosenAb(), neueSpielrunde - 1);
		return neueSpielrunde(aktiveMeldungen, SpielRundeNr.from(neueSpielrunde));
	}

}
