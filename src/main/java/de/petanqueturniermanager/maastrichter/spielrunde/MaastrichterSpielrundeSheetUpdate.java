/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Aktualisiert die aktuelle Vorrunde im Maastrichter Turnier (neu auslosen).
 */
public class MaastrichterSpielrundeSheetUpdate extends SchweizerSpielrundeSheetUpdate {

	public MaastrichterSpielrundeSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, MaastrichterSpielrundeSheetNaechste.SHEET_BASIS_NAME);
	}

	@Override
	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected String getSpielrundeSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselMaastrichterVorrunde(rundeNr);
	}

}
