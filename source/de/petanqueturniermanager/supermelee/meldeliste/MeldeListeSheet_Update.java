/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;

public class MeldeListeSheet_Update extends AbstractMeldeListeSheet {
	private static final Logger logger = LogManager.getLogger(MeldeListeSheet_Update.class);

	public MeldeListeSheet_Update(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		upDateSheet();
	}

	@Override
	public Logger getLogger() {
		return logger;
	}
}
