package de.petanqueturniermanager.schweizer.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 26.03.2024 / Michael Massee
 */

public class SchweizerSpielrundeSheetNaechste extends SchweizerAbstractSpielrundeSheet {
	private static final Logger LOGGER = LogManager.getLogger(SchweizerSpielrundeSheetNaechste.class);

	public SchweizerSpielrundeSheetNaechste(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		processBoxinfo("Neuer Spielrundeplan " + getSpielRundeNr().getNr());

		if (naechsteSpielrundeEinfuegen()) {

		}

	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

	public boolean naechsteSpielrundeEinfuegen() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNrInSheet(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return false;
		}

		// aktuelle vorhanden ?
		int neueSpielrunde = aktuelleSpielrunde.getNr();
		if (getSheetHelper().findByName(getSheetName(aktuelleSpielrunde)) != null) {
			neueSpielrunde++;
		}

		gespieltenRundenEinlesen(aktiveMeldungen, 1, neueSpielrunde - 1);
		return neueSpielrunde(aktiveMeldungen, SpielRundeNr.from(neueSpielrunde));
	}

}
