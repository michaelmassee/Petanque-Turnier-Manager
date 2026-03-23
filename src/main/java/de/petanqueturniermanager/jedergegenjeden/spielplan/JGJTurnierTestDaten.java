package de.petanqueturniermanager.jedergegenjeden.spielplan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.jedergegenjeden.meldeliste.JGJMeldeListeSheet_New;
import de.petanqueturniermanager.jedergegenjeden.rangliste.JGJRanglisteSheet;
import de.petanqueturniermanager.model.TeamMeldungen;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Generiert ein vollständiges JGJ-Beispielturnier:
 * Meldeliste + Spielplan mit Zufallsergebnissen + Rangliste.
 */
public class JGJTurnierTestDaten extends JGJSpielPlanSheet {

	public static final String TEST_GRUPPE = "Test Gruppe";

	public JGJTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet);
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.JGJ)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude(new String[] {});
		getKonfigurationSheet().setGruppenname(TEST_GRUPPE);

		// 1. Meldeliste erstellen und mit Testnamen befüllen
		JGJMeldeListeSheet_New meldeliste = new JGJMeldeListeSheet_New(getWorkingSpreadsheet());
		meldeliste.createMeldelisteWithParams(TEST_GRUPPE);
		testNamenEinfuegen(meldeliste);

		// 2. Spielplan generieren
		TeamMeldungen meldungen = meldeliste.getAlleMeldungen();
		generate(meldungen);

		// 3. Zufallsergebnisse einfügen
		processBoxinfo("processbox.jgj.testdaten.ergebnisse");
		ergebnisseEinfuegen();

		// 4. Rangliste erstellen
		processBoxinfo("processbox.erstelle.rangliste");
		new JGJRanglisteSheet(getWorkingSpreadsheet()).upDateSheet();

		// 5. Rangliste als aktives Sheet setzen
		var ranglisteSheet = new JGJRanglisteSheet(getWorkingSpreadsheet());
		TurnierSheet.from(ranglisteSheet.getXSpreadSheet(), getWorkingSpreadsheet()).setActiv();
	}

	private void testNamenEinfuegen(JGJMeldeListeSheet_New meldeliste) throws GenerateException {
		List<String> testNamen = new ArrayList<>(listeMitTestNamen());
		Collections.shuffle(testNamen);

		RangeData data = new RangeData();
		for (String name : testNamen) {
			RowData zeile = data.addNewRow();
			zeile.newEmpty(); // Nr-Spalte (wird automatisch vergeben)
			zeile.newString(name);
		}

		Position pos = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		RangeHelper.from(meldeliste, data.getRangePosition(pos)).setDataInRange(data);
		meldeliste.upDateSheet();
	}

	private void ergebnisseEinfuegen() throws GenerateException {
		XSpreadsheet sheet = getXSpreadSheet();
		RangePosition leseBereich = RangePosition.from(
				TEAM_A_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE,
				TEAM_B_NR_SPALTE, ERSTE_SPIELTAG_DATEN_ZEILE + 500);
		RangeData data = RangeHelper
				.from(sheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), leseBereich)
				.getDataFromRange();

		for (int i = 0; i < data.size(); i++) {
			RowData zeile = data.get(i);
			if (zeile.size() < 2) break;

			int nrA = zeile.get(0).getIntVal(0);
			if (nrA <= 0) break; // Ende der Daten

			int nrB = zeile.get(1).getIntVal(0);
			if (nrB <= 0) continue; // Freispiel – kein Ergebnis nötig

			int spielZeile = ERSTE_SPIELTAG_DATEN_ZEILE + i;
			int gewinner = ThreadLocalRandom.current().nextInt(2); // 0 oder 1
			int verliererPunkte = ThreadLocalRandom.current().nextInt(0, 13); // 0–12
			int ergA = (gewinner == 0) ? 13 : verliererPunkte;
			int ergB = (gewinner == 0) ? verliererPunkte : 13;

			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(SPIELPNKT_A_SPALTE, spielZeile)).setValue(ergA));
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(sheet, Position.from(SPIELPNKT_B_SPALTE, spielZeile)).setValue(ergB));
		}
	}

	List<String> listeMitTestNamen() {
		return List.of(
				"BC-Linden 1",
				"Boule Biebertal",
				"Boule-Freunde Fernwald",
				"PC Petterweil",
				"PSG Ehringshausen 1",
				"DFG Wettenberg 1",
				"Boulefreunde Marburg",
				"Boulodromedare Fulda 2",
				"VNH Hain-Gründau 1",
				"BC Darmstadt West");
	}

}
