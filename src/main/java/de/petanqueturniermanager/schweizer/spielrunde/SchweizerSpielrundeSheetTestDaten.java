package de.petanqueturniermanager.schweizer.spielrunde;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;

/**
 * Erstellung 27.03.2024 / Michael Massee
 */

public class SchweizerSpielrundeSheetTestDaten extends SchweizerAbstractSpielrundeSheet {

	private final SchweizerMeldeListeSheetTestDaten schweizerMeldeListeSheetTestDaten;
	private final SchweizerSpielrundeSheetNaechste schweizerSpielrundeSheetNaechste;

	protected SchweizerSpielrundeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		schweizerMeldeListeSheetTestDaten = new SchweizerMeldeListeSheetTestDaten(workingSpreadsheet);
		schweizerSpielrundeSheetNaechste = new SchweizerSpielrundeSheetNaechste(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		// clean up first
		getSheetHelper().removeAllSheetsExclude();
		generate();
		// sicher gehen das aktive spielrunde sheet ist activ
		getSheetHelper().setActiveSheet(getXSpreadSheet());
	}

	/**
	 *
	 * @throws GenerateException
	 */
	public void generate() throws GenerateException {
		schweizerMeldeListeSheetTestDaten.doRun();
		schweizerSpielrundeSheetNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);

		// 1. Spielrunde erstellen
		schweizerSpielrundeSheetNaechste.doRun();

	}


}
