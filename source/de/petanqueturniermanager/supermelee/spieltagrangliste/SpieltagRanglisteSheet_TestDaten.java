/**
* Erstellung : 07.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spieltagrangliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.spielrunde.SpielrundeSheet_TestDaten;

/**
 * generate 5 komplette spieltage
 *
 * @author michael
 *
 */
public class SpieltagRanglisteSheet_TestDaten extends SheetRunner {
	private static final Logger logger = LogManager.getLogger(SpieltagRanglisteSheet_TestDaten.class);

	final SpielrundeSheet_TestDaten spielrundeSheetTestDaten;

	public SpieltagRanglisteSheet_TestDaten(XComponentContext xContext) {
		super(xContext);
		this.spielrundeSheetTestDaten = new SpielrundeSheet_TestDaten(xContext);

	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		for (int i = 1; i < 3; i++) {
			this.spielrundeSheetTestDaten.setSpielTag(SpielTagNr.from(i));
			this.spielrundeSheetTestDaten.generate();
		}
	}
}
