/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.rangliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheetUpdate;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Aktualisiert die Maastrichter Vorrunden-Rangliste ohne das Sheet neu zu erstellen.
 *
 * @see SchweizerRanglisteSheetUpdate
 */
public class MaastrichterVorrundenRanglisteSheetUpdate extends SchweizerRanglisteSheetUpdate {

	public MaastrichterVorrundenRanglisteSheetUpdate(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER);
	}

	@Override
	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected String getSpielrundenBasisName() {
		return MaastrichterSpielrundeSheetNaechste.SHEET_BASIS_NAME;
	}

	@Override
	protected String getRanglistenSheetName() {
		return MaastrichterVorrundenRanglisteSheet.SHEETNAME;
	}

	@Override
	protected String getMetadatenSchluessel() {
		return SheetMetadataHelper.SCHLUESSEL_MAASTRICHTER_VORRUNDE_PREFIX;
	}

	@Override
	protected SchweizerRanglisteSheet erstelleNeuAufbauSheet() {
		return new MaastrichterVorrundenRanglisteSheet(getWorkingSpreadsheet());
	}
}
