/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.rangliste;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die Vorrunden-Rangliste für das Maastrichter Turniersystem.
 * Liest "N. Vorrunde"-Blätter statt "N. Spielrunde"-Blätter.
 */
public class MaastrichterVorrundenRanglisteSheet extends SchweizerRanglisteSheet {

	public static final String SHEETNAME = "Vorrunden-Rangliste";

	public MaastrichterVorrundenRanglisteSheet(WorkingSpreadsheet workingSpreadsheet) {
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
		return SHEETNAME;
	}

}
