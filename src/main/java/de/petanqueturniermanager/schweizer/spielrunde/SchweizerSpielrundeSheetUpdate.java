package de.petanqueturniermanager.schweizer.spielrunde;

import java.util.List;

import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * Erstellung 2026 / Michael Massee
 */

public class SchweizerSpielrundeSheetUpdate extends SchweizerAbstractSpielrundeSheet {

	public SchweizerSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	protected SchweizerSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet,
			de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem ts, String sheetBaseName) {
		super(workingSpreadsheet, ts, sheetBaseName);
	}

	@Override
	public void doRun() throws GenerateException {
		getxCalculatable().enableAutomaticCalculation(false); // speed up

		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		processBoxinfo(I18n.get("processbox.aktuelle.spielrunde", aktuelleSpielrunde.getNr()));
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


}
