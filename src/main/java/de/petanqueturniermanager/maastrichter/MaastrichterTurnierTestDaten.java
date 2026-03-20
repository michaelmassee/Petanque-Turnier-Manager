/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter;

import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.finalrunde.MaastrichterFinalrundeSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.maastrichter.meldeliste.MaastrichterMeldeListeSheetTestDaten;
import de.petanqueturniermanager.maastrichter.rangliste.MaastrichterVorrundenRanglisteSheet;
import de.petanqueturniermanager.maastrichter.spielrunde.MaastrichterSpielrundeSheetNaechste;
import de.petanqueturniermanager.schweizer.spielrunde.SchweizerAbstractSpielrundeSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges Maastrichter Beispielturnier ohne Dialoge:
 * <ol>
 *   <li>Meldeliste (12 Teams, Doublette)</li>
 *   <li>3 Vorrunden (Schweizer System) mit Zufallsergebnissen</li>
 *   <li>Vorrunden-Rangliste</li>
 *   <li>Finalrunden (A/B/C/D-Bracket)</li>
 * </ol>
 */
public class MaastrichterTurnierTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private final MaastrichterMeldeListeSheetTestDaten meldelisteTestDaten;
	private final MaastrichterSpielrundeSheetNaechste naechsteVorrunde;
	private final MaastrichterVorrundenRanglisteSheet ranglisteSheet;
	private final MaastrichterFinalrundeSheet finalrundeSheet;

	public MaastrichterTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Turnier-Testdaten");
		meldelisteTestDaten = new MaastrichterMeldeListeSheetTestDaten(workingSpreadsheet);
		naechsteVorrunde = new MaastrichterSpielrundeSheetNaechste(workingSpreadsheet);
		ranglisteSheet = new MaastrichterVorrundenRanglisteSheet(workingSpreadsheet);
		finalrundeSheet = new MaastrichterFinalrundeSheet(workingSpreadsheet);
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SHEETNAME);
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.MAASTRICHTER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// 1. Meldeliste erstellen (löscht alle vorhandenen Sheets)
		meldelisteTestDaten.erstelleTestdaten();

		// Spielbahn auf Bahnnummern setzen; Anzahl Vorrunden aus Konfiguration lesen
		MaastrichterKonfigurationSheet konfigSheet = new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
		konfigSheet.setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		int anzVorrunden = konfigSheet.getAnzVorrunden();

		// 2. Vorrunden erstellen und mit Zufallsergebnissen füllen
		for (int runde = 1; runde <= anzVorrunden; runde++) {
			SheetRunner.testDoCancelTask();
			processBoxinfo("Erstelle Vorrunde " + runde + " von " + anzVorrunden + " ...");
			naechsteVorrunde.erstelleNaechsteVorrunde();

			String sheetName = runde + ". " + MaastrichterSpielrundeSheetNaechste.SHEET_BASIS_NAME;
			XSpreadsheet sheet = getSheetHelper().findByName(sheetName);
			if (sheet != null) {
				ergebnisseEinfuegen(sheet);
			}
		}

		// 3. Vorrunden-Rangliste erstellen
		SheetRunner.testDoCancelTask();
		processBoxinfo("Erstelle Vorrunden-Rangliste...");
		ranglisteSheet.doRun();

		// 4. Finalrunden erstellen
		SheetRunner.testDoCancelTask();
		processBoxinfo("Erstelle Finalrunden...");
		finalrundeSheet.doRun();
	}

	/**
	 * Füllt alle Paarungen des Vorrunden-Sheets mit Zufallsergebnissen (13:x).
	 */
	private void ergebnisseEinfuegen(XSpreadsheet sheet) throws GenerateException {
		RangePosition readRange = RangePosition.from(
				SchweizerAbstractSpielrundeSheet.TEAM_A_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE,
				SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE,
				SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + 100);

		RangeData data = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		for (int i = 0; i < data.size(); i++) {
			RowData row = data.get(i);
			if (row.size() < 2) break;

			int nrA = row.get(0).getIntVal(0);
			if (nrA <= 0) break;
			int nrB = row.get(1).getIntVal(0);
			if (nrB <= 0) continue; // Freilos – kein Ergebnis nötig

			int zeile = SchweizerAbstractSpielrundeSheet.ERSTE_DATEN_ZEILE + i;
			int winner = ThreadLocalRandom.current().nextInt(2);
			int loserPts = ThreadLocalRandom.current().nextInt(0, 13);
			int ergA = (winner == 0) ? 13 : loserPts;
			int ergB = (winner == 0) ? loserPts : 13;

			getSheetHelper().setNumberValueInCell(NumberCellValue
					.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_A_SPALTE, zeile))
					.setValue(ergA));
			getSheetHelper().setNumberValueInCell(NumberCellValue
					.from(sheet, Position.from(SchweizerAbstractSpielrundeSheet.ERG_TEAM_B_SPALTE, zeile))
					.setValue(ergB));
		}
	}

}
