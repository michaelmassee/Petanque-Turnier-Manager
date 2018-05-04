/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;

public class MeldeListeSheet_NeuerSpieltag extends AbstractMeldeListeSheet {

	public MeldeListeSheet_NeuerSpieltag(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	protected void doRun() throws GenerateException {

	}

	private void insertNewSpieltag() {

	}

}
