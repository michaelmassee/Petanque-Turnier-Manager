package de.petanqueturniermanager.triptete.spielplan;

import java.util.List;

import de.petanqueturniermanager.algorithmen.triptete.TripTetePaarungen;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.model.TeamPaarung;
import de.petanqueturniermanager.triptete.meldeliste.TripTeteMeldeListeSheetTestDaten;
import de.petanqueturniermanager.triptete.rangliste.TripTeteRanglisteSheet;

/**
 * Test-Daten-Lauf: erzeugt Meldeliste mit 6 Teams, Spielplan, befüllt alle
 * drei Partien jeder Begegnung mit Pseudo-Ergebnissen und baut die Rangliste.
 */
public class TripTeteSpielPlanSheetTestDaten extends TripTeteSpielPlanSheet {

	public TripTeteSpielPlanSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	public void generate() throws GenerateException {
		doRun();
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.TRIPTETE)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude(new String[] {});

		new TripTeteMeldeListeSheetTestDaten(getWorkingSpreadsheet()).erstelleUndFuelleTestDaten();

		TeamMeldungen meldungen = getMeldeListe().getAlleMeldungen();
		super.generate(meldungen);
		ergebnisseEinlesen(TripTetePaarungen.jederGegenJeden(meldungen));

		new TripTeteRanglisteSheet(getWorkingSpreadsheet()).upDateSheet();
	}

	/**
	 * Schreibt pro Begegnung 3 Pseudo-Partie-Ergebnisse (Triplette, Doublette,
	 * Tête) in den Spielplan. Jedes Spiel endet mit 13:Pseudo.
	 */
	private void ergebnisseEinlesen(List<List<TeamPaarung>> spielPlan) throws GenerateException {
		RangeData rangeData = new RangeData();
		for (List<TeamPaarung> runde : spielPlan) {
			for (int i = 0; i < runde.size(); i++) {
				RowData row = rangeData.addNewRow();
				for (int partie = 0; partie < 3; partie++) {
					int gewinner = RandomSource.nextInt(0, 2);
					int verliererPunkte = RandomSource.nextInt(0, 13);
					if (gewinner == 0) {
						row.newInt(13);
						row.newInt(verliererPunkte);
					} else {
						row.newInt(verliererPunkte);
						row.newInt(13);
					}
				}
			}
		}
		Position startPos = Position.from(TRI_A_SPALTE, ERSTE_DATEN_ZEILE);
		RangeHelper.from(this, rangeData.getRangePosition(startPos)).setDataInRange(rangeData);
	}
}
