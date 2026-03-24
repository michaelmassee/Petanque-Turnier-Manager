/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt die nächste Vorrunde im Maastrichter Turnier (Schweizer Algorithmus,
 * aber Blätter heißen "N. Vorrunde" statt "N. Spielrunde").
 */
public class MaastrichterSpielrundeSheetNaechste extends SchweizerSpielrundeSheetNaechste {

	public static final String SHEET_BASIS_NAME = "Vorrunde";

	public MaastrichterSpielrundeSheetNaechste(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, SHEET_BASIS_NAME);
	}

	@Override
	protected SchweizerKonfigurationSheet initKonfigurationSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterKonfigurationSheet(workingSpreadsheet);
	}

	@Override
	protected String getSpielrundeSchluessel(int rundeNr) {
		return SheetMetadataHelper.schluesselMaastrichterVorrunde(rundeNr);
	}

	@Override
	protected String getSheetName(SpielRundeNr nr) {
		return SheetNamen.maastrichterVorrunde(nr.getNr());
	}

	@Override
	protected SchweizerMeldeListeSheetUpdate initMeldeListeSheet(WorkingSpreadsheet workingSpreadsheet) {
		return new MaastrichterMeldeListeSheetUpdate(workingSpreadsheet);
	}

	/** Öffentlicher Einstiegspunkt für Testdaten-Generatoren. */
	public void erstelleNaechsteVorrunde() throws de.petanqueturniermanager.exception.GenerateException {
		doRun();
	}

}
