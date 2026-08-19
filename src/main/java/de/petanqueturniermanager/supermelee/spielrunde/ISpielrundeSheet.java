package de.petanqueturniermanager.supermelee.spielrunde;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.basesheet.meldeliste.SpielRundeNr;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

interface ISpielrundeSheet extends ISheet {

	SpielTagNr getSpielTag();

	SpielRundeNr getSpielRundeNr();

	void setSpielRundeNr(SpielRundeNr spielRundeNr);

	boolean isForceOk();

	Integer getMaxAnzGespielteSpieltage() throws GenerateException;
}
