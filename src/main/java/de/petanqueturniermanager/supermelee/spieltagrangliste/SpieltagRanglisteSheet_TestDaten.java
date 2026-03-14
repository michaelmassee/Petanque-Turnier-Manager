/**
 * Erstellung : 07.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.MeldeListeSheet_NeuerSpieltag;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_Validator;

/**
 * generate 5 komplette spieltage
 *
 * @author michael
 *
 */
public class SpieltagRanglisteSheet_TestDaten extends SheetRunner {
	private final SuperMeleeKonfigurationSheet konfigurationSheet;
	private final SpielrundeSheet_TestDaten spielrundeSheetTestDaten;
	private final MeldeListeSheet_NeuerSpieltag meldeListeSheetNeuerSpieltag;

	public SpieltagRanglisteSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.SUPERMELEE);
		konfigurationSheet = new SuperMeleeKonfigurationSheet(workingSpreadsheet);
		spielrundeSheetTestDaten = new SpielrundeSheet_TestDaten(workingSpreadsheet);
		meldeListeSheetNeuerSpieltag = new MeldeListeSheet_NeuerSpieltag(workingSpreadsheet);
	}

	@Override
	protected SuperMeleeKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	@Override
	protected void doRun() throws GenerateException {

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SUPERMELEE)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

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
			getKonfigurationSheet().setAktiveSpieltag(spieltagNr);

			spielrundeSheetTestDaten.generate();

			// validieren
			new SpielrundeSheet_Validator(getWorkingSpreadsheet()).validateSpieltag(spieltagNr);
		}
	}

}
