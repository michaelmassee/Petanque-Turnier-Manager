/**
* Erstellung : 07.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;

/**
 * generate 5 komplette spieltage
 *
 * @author michael
 *
 */
public class SpieltagRanglisteSheet_TestDaten extends SuperMeleeSheet {
	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheet_TestDaten.class);

	private final SpielrundeSheet_TestDaten spielrundeSheetTestDaten;
	private final MeldeListeSheet_NeuerSpieltag meldeListeSheetNeuerSpieltag;

	public SpieltagRanglisteSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		spielrundeSheetTestDaten = new SpielrundeSheet_TestDaten(workingSpreadsheet);
		meldeListeSheetNeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(workingSpreadsheet);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] { SupermeleeTeamPaarungenSheet.SHEETNAME });

		for (int spieltagCntr = 1; spieltagCntr <= 5; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spieltagNr = SpielTagNr.from(spieltagCntr);

			if (spieltagCntr > 1) {
				meldeListeSheetNeuerSpieltag.setSpielTag(spieltagNr);
				meldeListeSheetNeuerSpieltag.naechsteSpieltag();
			}

			spielrundeSheetTestDaten.setSpielTag(spieltagNr);
			spielrundeSheetTestDaten.generate();
			getKonfigurationSheet().setAktiveSpieltag(spieltagNr);

			// validieren
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(spieltagNr);
		}
	}

}
