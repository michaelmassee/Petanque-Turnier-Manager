/*
 * Erstellung 27.03.2024 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.spielrunde;

import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;

/**
 * Erzeugt eine Schweizer Meldeliste mit Testdaten und die erste Spielrunde. Ersetzt alle Blätter – vorher wird
 * nachgefragt und eine bestehende PTM-Online-Verbindung getrennt.
 */
public class SchweizerSpielrundeSheetTestDaten extends SchweizerAbstractSpielrundeSheet {

	private final SchweizerMeldeListeSheetTestDaten schweizerMeldeListeSheetTestDaten;
	private final SchweizerSpielrundeSheetNaechste schweizerSpielrundeSheetNaechste;

	protected SchweizerSpielrundeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
		schweizerMeldeListeSheetTestDaten = new SchweizerMeldeListeSheetTestDaten(workingSpreadsheet);
		schweizerSpielrundeSheetNaechste = new SchweizerSpielrundeSheetNaechste(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SCHWEIZER)
				.prefix(getLogPrefix()).validateFuerTestdaten()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude();
		generate();
		// erst nach dem Erzeugen ist die Spielrunde vorhanden und kann aktiviert werden
		getSheetHelper().setActiveSheet(getXSpreadSheet());
	}

	/**
	 * Erzeugt Meldeliste und erste Spielrunde ohne Rückfrage.
	 *
	 * @throws GenerateException wenn Meldeliste oder Spielrunde nicht erzeugt werden können
	 */
	public void generate() throws GenerateException {
		schweizerMeldeListeSheetTestDaten.doRun();
		schweizerSpielrundeSheetNaechste.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		schweizerSpielrundeSheetNaechste.doRun();
	}

}
