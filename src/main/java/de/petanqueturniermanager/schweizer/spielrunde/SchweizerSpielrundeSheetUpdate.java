package de.petanqueturniermanager.schweizer.spielrunde;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.ptmonline.PtmOnlineSpielrundeSync;
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
		TeamMeldungen aktivUndAusgesetztVorSync = getMeldeListe().getAktiveUndAusgesetztMeldungen();
		Set<Integer> aktiveNrVorSync = PtmOnlineSpielrundeSync.nummern(getMeldeListe().getAktiveMeldungen());
		Set<Integer> ausgestiegeneNr = new HashSet<>(PtmOnlineSpielrundeSync.nummern(aktivUndAusgesetztVorSync));
		ausgestiegeneNr.removeAll(aktiveNrVorSync);
		PtmOnlineSpielrundeSync.abgleichen(getWorkingSpreadsheet(), getTurnierSystem(), false,
				PtmOnlineSpielrundeSync.nummern(getMeldeListe().getAlleMeldungen()), aktiveNrVorSync, ausgestiegeneNr);

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
