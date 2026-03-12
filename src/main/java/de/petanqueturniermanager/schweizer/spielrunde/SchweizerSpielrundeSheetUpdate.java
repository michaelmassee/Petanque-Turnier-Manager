package de.petanqueturniermanager.schweizer.spielrunde;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 2026 / Michael Massee
 */

public class SchweizerSpielrundeSheetUpdate extends SchweizerAbstractSpielrundeSheet {
	private static final Logger LOGGER = LogManager.getLogger(SchweizerSpielrundeSheetUpdate.class);

	public SchweizerSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	public void doRun() throws GenerateException {
		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		processBoxinfo("Aktuelle Spielrunde " + aktuelleSpielrunde.getNr());
		setSpielRundeNrInSheet(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return;
		}

		List<SchweizerTeamErgebnis> ergebnisse = gespieltenRundenEinlesen(aktiveMeldungen, 1,
				aktuelleSpielrunde.getNr() - 1);

		// Teams nach Rangliste sortieren (ab Runde 2)
		TeamMeldungen meldungenFuerAuslosung = (aktuelleSpielrunde.getNr() > 1)
				? sortierteTeamMeldungen(aktiveMeldungen, ergebnisse)
				: aktiveMeldungen;

		neueSpielrunde(meldungenFuerAuslosung, aktuelleSpielrunde, ergebnisse);
	}

	@Override
	public Logger getLogger() {
		return LOGGER;
	}

}
