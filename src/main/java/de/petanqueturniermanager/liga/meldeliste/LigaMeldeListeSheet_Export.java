/**
* Erstellung : 30.06.2022 / Michael Massee
**/

package de.petanqueturniermanager.liga.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.io.PdfExport;
import de.petanqueturniermanager.liga.rangliste.LigaRanglisteSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheet;

/**
 * Exportiert die Tabellen nach pdf und erstelt eine html datei
 * 
 * @author michael
 *
 */

public class LigaMeldeListeSheet_Export extends AbstractLigaMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheet_Export.class);

	public LigaMeldeListeSheet_Export(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, "Liga Export");
	}

	@Override
	protected void doRun() throws GenerateException {
		upDateSheet();
		ProcessBox().info("Exportiere nach PDF");
		ProcessBox().info(PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaSpielPlanSheet.SHEET_NAMEN)
				.prefix1(LigaSpielPlanSheet.SHEET_NAMEN).doExport().toString());
		ProcessBox().info(PdfExport.from(getWorkingSpreadsheet()).sheetName(LigaRanglisteSheet.SHEETNAME)
				.prefix1(LigaRanglisteSheet.SHEETNAME).doExport().toString());

	}

	@Override
	public Logger getLogger() {
		return logger;
	}

}
