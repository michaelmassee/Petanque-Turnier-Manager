/**
* Erstellung : 26.03.2018 / Michael Massee
**/
package de.petanqueturniermanager.supermelee.spielrunde;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.Meldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

public class SpielrundeSheet_Naechste extends AbstractSpielrundeSheet {
	private static final Logger logger = LogManager.getLogger(SpielrundeSheet_Naechste.class);

	public SpielrundeSheet_Naechste(XComponentContext xContext) {
		super(xContext);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	protected void doRun() throws GenerateException {
		setSpielTag(getKonfigurationSheet().getAktiveSpieltag());
		naechsteSpielrundeEinfuegen();
		new SpielrundeSheet_Validator(getxContext()).validateSpieltag(getSpielTag()); // validieren
	}

	public void naechsteSpielrundeEinfuegen() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		setSpielRundeNr(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		Meldungen meldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(meldungen)) {
			return;
		}

		// aktuelle vorhanden ?
		int neueSpielrunde = aktuelleSpielrunde.getNr();
		if (getSheetHelper().findByName(getSheetName(getSpielTag(), getSpielRundeNr())) != null) {
			neueSpielrunde++;
		}

		gespieltenRundenEinlesen(meldungen, getKonfigurationSheet().getSpielRundeNeuAuslosenAb(), neueSpielrunde - 1);
		neueSpielrunde(meldungen, SpielRundeNr.from(neueSpielrunde));
	}

}
