package de.petanqueturniermanager.schweizer.spielrunde;

import java.util.List;

import de.petanqueturniermanager.algorithmen.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzManager;
import de.petanqueturniermanager.helper.sheet.blattschutz.BlattschutzRegistry;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;
import de.petanqueturniermanager.toolbar.TurnierModus;

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
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(getTurnierSystem())
					.ifPresent(k -> BlattschutzManager.get().entsperren(k, getWorkingSpreadsheet()));
		}

		SpielRundeNr aktuelleSpielrunde = getKonfigurationSheet().getAktiveSpielRunde();
		processBoxinfo("processbox.aktuelle.spielrunde", aktuelleSpielrunde.getNr());
		setSpielRundeNrInSheet(aktuelleSpielrunde);
		getMeldeListe().upDateSheet();
		TeamMeldungen aktiveMeldungen = getMeldeListe().getAktiveMeldungen();

		if (!canStart(aktiveMeldungen)) {
			if (TurnierModus.get().istAktiv()) {
				BlattschutzRegistry.fuer(getTurnierSystem())
						.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
			}
			return;
		}

		List<SchweizerTeamErgebnis> ergebnisse = gespieltenRundenEinlesen(aktiveMeldungen, 1,
				aktuelleSpielrunde.getNr() - 1);

		// Teams nach Rangliste sortieren (ab Runde 2)
		TeamMeldungen meldungenFuerAuslosung = (aktuelleSpielrunde.getNr() > 1)
				? sortierteTeamMeldungen(aktiveMeldungen, ergebnisse)
				: aktiveMeldungen;

		neueSpielrunde(meldungenFuerAuslosung, aktuelleSpielrunde, ergebnisse);
		if (TurnierModus.get().istAktiv()) {
			BlattschutzRegistry.fuer(getTurnierSystem())
					.ifPresent(k -> BlattschutzManager.get().schuetzen(k, getWorkingSpreadsheet()));
		}
	}


}
