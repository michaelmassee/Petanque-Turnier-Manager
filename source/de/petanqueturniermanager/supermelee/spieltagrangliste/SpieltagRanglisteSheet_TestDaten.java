/**
* Erstellung : 07.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.konfiguration.KonfigurationSheet;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.SupermeleeTeamPaarungenSheet;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;

/**
 * generate 5 komplette spieltage
 *
 * @author michael
 *
 */
public class SpieltagRanglisteSheet_TestDaten extends SheetRunner {
	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheet_TestDaten.class);

	private final SpielrundeSheet_TestDaten spielrundeSheetTestDaten;
	private final KonfigurationSheet konfigurationSheet;

	public SpieltagRanglisteSheet_TestDaten(XComponentContext xContext) {
		super(xContext);
		this.spielrundeSheetTestDaten = new SpielrundeSheet_TestDaten(xContext);
		this.konfigurationSheet = new KonfigurationSheet(xContext);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		// clean up first
		this.getSheetHelper().removeAllSheetsExclude(
				new String[] { KonfigurationSheet.SHEETNAME, SupermeleeTeamPaarungenSheet.SHEETNAME });

		for (int spieltagCntr = 1; spieltagCntr <= 5; spieltagCntr++) {
			SheetRunner.testDoCancelTask();
			SpielTagNr spieltagNr = SpielTagNr.from(spieltagCntr);
			this.spielrundeSheetTestDaten.setSpielTag(spieltagNr);
			this.spielrundeSheetTestDaten.generate();
			this.konfigurationSheet.setAktiveSpieltag(spieltagNr);
		}
	}
}
