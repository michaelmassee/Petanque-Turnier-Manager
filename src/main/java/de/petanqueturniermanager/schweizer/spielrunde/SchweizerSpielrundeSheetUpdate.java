package de.petanqueturniermanager.schweizer.spielrunde;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerAuswertung;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellung 2026 / Michael Massee
 */

public class SchweizerSpielrundeSheetUpdate extends SchweizerAbstractSpielrundeSheet {

	public SchweizerSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	protected SchweizerSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet,
			TurnierSystem ts, String sheetBaseName) {
		super(workingSpreadsheet, ts, sheetBaseName);
	}

	@Override
	public void doRun() throws GenerateException {
		getxCalculatable().enableAutomaticCalculation(false); // speed up

		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		processBoxinfo("processbox.aktuelle.spielrunde", aktuelleSpielrunde.getNr());
		setSpielRundeNrInSheet(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			return;
		}

		SchweizerAuswertung auswertung = gespieltenRundenEinlesen(aktiveMeldungen, 1,
				aktuelleSpielrunde.getNr() - 1);

		// Teams nach Rangliste sortieren (ab Runde 2)
		TeamMeldungen meldungenFuerAuslosung = (aktuelleSpielrunde.getNr() > 1)
				? sortierteTeamMeldungen(aktiveMeldungen, auswertung)
				: aktiveMeldungen;

		neueSpielrunde(meldungenFuerAuslosung, aktuelleSpielrunde, auswertung.ergebnisse());
	}


}
