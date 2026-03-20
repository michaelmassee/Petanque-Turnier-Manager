package de.petanqueturniermanager.schweizer.spielrunde;

import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetTestDaten;
import de.petanqueturniermanager.schweizer.rangliste.SchweizerRanglisteSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges Schweizer Beispielturnier:
 * Meldeliste + Spielrunden mit Zufallsergebnissen + Rangliste.
 * <p>
 * Standard: 16 Teams, Triplette, Teamnummer-Anzeige.<br>
 * Parametrisiert: beliebige Teamanzahl und SpielplanTeamAnzeige konfigurierbar.
 */
public class SchweizerTurnierTestDaten extends SchweizerAbstractSpielrundeSheet {

	private static final int ANZ_RUNDEN = 3;

	private final SpielplanTeamAnzeige spielplanTeamAnzeige;

	private final SchweizerMeldeListeSheetTestDaten meldelisteTestDaten;
	public final SchweizerSpielrundeSheetNaechste naechsteSpielrunde;
	private final SchweizerRanglisteSheet ranglisteSheet;

	/** Standard-Konstruktor: 16 Teams, Teamnummer-Anzeige */
	public SchweizerTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, SchweizerMeldeListeSheetTestDaten.ANZ_TEAMS_DEFAULT, SpielplanTeamAnzeige.NR);
	}

	/**
	 * Parametrisierter Konstruktor.
	 *
	 * @param anzTeams            Anzahl zu generierender Teams
	 * @param spielplanTeamAnzeige NR = Teamnummer, NAME = Teamname in der Spielrunde
	 */
	public SchweizerTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet,
			int anzTeams, SpielplanTeamAnzeige spielplanTeamAnzeige) {
		super(workingSpreadsheet);
		this.spielplanTeamAnzeige = spielplanTeamAnzeige;
		meldelisteTestDaten = new SchweizerMeldeListeSheetTestDaten(workingSpreadsheet, anzTeams);
		naechsteSpielrunde = new SchweizerSpielrundeSheetNaechste(workingSpreadsheet);
		ranglisteSheet = new SchweizerRanglisteSheet(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator
				.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.SCHWEIZER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude();
		generate();
		getSheetHelper().setActiveSheet(ranglisteSheet.getXSpreadSheet());
	}

	public void generate() throws GenerateException {
		generate(ANZ_RUNDEN, true);
	}

	public void generate(int anzRunden, boolean mitRangliste) throws GenerateException {
		// 1. Meldeliste erstellen
		meldelisteTestDaten.doRun();
		naechsteSpielrunde.getKonfigurationSheet().setSpielrundeSpielbahn(SpielrundeSpielbahn.R);
		naechsteSpielrunde.getKonfigurationSheet().setSpielplanTeamAnzeige(spielplanTeamAnzeige);

		// 2. Spielrunden erstellen und mit Zufallsergebnissen füllen
		for (int runde = 1; runde <= anzRunden; runde++) {
			SheetRunner.testDoCancelTask();
			processBoxinfo("Erstelle Spielrunde " + runde + " von " + anzRunden + " ...");
			naechsteSpielrunde.doRun();

			XSpreadsheet sheet = getSheetHelper().findByName(runde + ". " + SHEET_NAMEN);
			if (sheet != null) {
				ergebnisseEinfuegen(sheet);
			}
		}

		// 3. Rangliste erstellen (optional)
		if (mitRangliste) {
			SheetRunner.testDoCancelTask();
			ranglisteSheet.doRun();
		}
	}

	private void ergebnisseEinfuegen(XSpreadsheet sheet) throws GenerateException {
		RangePosition readRange = RangePosition.from(
				TEAM_A_SPALTE, ERSTE_DATEN_ZEILE, ERG_TEAM_B_SPALTE, ERSTE_DATEN_ZEILE + 100);
		RangeData data = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), readRange)
				.getDataFromRange();

		for (int i = 0; i < data.size(); i++) {
			RowData row = data.get(i);
			if (row.size() < 2) break;

			int nrA = row.get(0).getIntVal(0);
			if (nrA <= 0) break; // Ende der Daten
			int nrB = row.get(1).getIntVal(0);
			if (nrB <= 0) continue; // Freilos – kein Ergebnis nötig

			int zeile = ERSTE_DATEN_ZEILE + i;
			int winner = ThreadLocalRandom.current().nextInt(2); // 0 oder 1
			int loserPts = ThreadLocalRandom.current().nextInt(0, 13); // 0–12
			int ergA = (winner == 0) ? 13 : loserPts;
			int ergB = (winner == 0) ? loserPts : 13;

			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(ERG_TEAM_A_SPALTE, zeile)).setValue(ergA));
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(ERG_TEAM_B_SPALTE, zeile)).setValue(ergB));
		}
	}

}
