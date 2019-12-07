/**
 * Erstellung 29.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.spielplan;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.liga.meldeliste.LigaMeldeListeSheet_TestDaten;

/**
 * @author Michael Massee
 *
 */
public class LigaSpielPlanSheet_TestDaten extends LigaSpielPlanSheet {

	/**
	 * @param workingSpreadsheet
	 */
	public LigaSpielPlanSheet_TestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] { IKonfigurationKonstanten.SHEETNAME });
		// Meldeliste
		new LigaMeldeListeSheet_TestDaten(getWorkingSpreadsheet()).testNamenEinfuegen();
	}

	/**
	 * testdaten generieren
	 *
	 * @throws GenerateException
	 */
	public void generate() throws GenerateException {
		super.generate(getMeldeListe().getAlleMeldungen());

	}

}
