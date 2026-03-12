/**
 * Erstellung : 26.03.2018 / Michael Massee
 **/
package de.petanqueturniermanager.supermelee.spielrunde;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.SpielerMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.spieltagrangliste.SpieltagRanglisteSheet;

public class SpielrundeSheet_Naechste extends AbstractSpielrundeSheet {

	public SpielrundeSheet_Naechste(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		if (naechsteSpielrundeEinfuegen()) {
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(getSpielTag()); // validieren

			// Spieltag rangliste vorhanden ?
			SpieltagRanglisteSheet spieltagRanglisteSheet = new SpieltagRanglisteSheet(getWorkingSpreadsheet());
			String ranglisteSheetName = spieltagRanglisteSheet.getSheetName(getSpielTag());
			XSpreadsheet xSpieltagRanglisteSheet = getSheetHelper().findByName(ranglisteSheetName);
			if (xSpieltagRanglisteSheet != null) {
				spieltagRanglisteSheet.generate(getSpielTag());
			}

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

		gespieltenRundenEinlesen(aktiveMeldungen, getKonfigurationSheet().getSpielRundeNeuAuslosenAb(),
				neueSpielrunde - 1);
		return neueSpielrunde(aktiveMeldungen, SpielRundeNr.from(neueSpielrunde));
	}

}
