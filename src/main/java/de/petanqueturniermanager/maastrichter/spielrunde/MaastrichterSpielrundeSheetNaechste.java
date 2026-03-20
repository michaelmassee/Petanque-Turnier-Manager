/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.spielrunde;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SchweizerKonfigurationSheet;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerSpielrundeSheetNaechste;
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

	/** Öffentlicher Einstiegspunkt für Testdaten-Generatoren. */
	public void erstelleNaechsteVorrunde() throws de.petanqueturniermanager.exception.GenerateException {
		doRun();
	}

}
