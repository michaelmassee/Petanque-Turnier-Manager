/**
 * Erstellung : 24.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.liga.meldeliste;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.liga.konfiguration.LigaSheet;
import de.petanqueturniermanager.liga.spielplan.LigaSpielPlanSheetTestDaten;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

public class LigaMeldeListeSheetTestDaten extends LigaSheet implements ISheet {

	private static final Logger logger = LogManager.getLogger(LigaMeldeListeSheetTestDaten.class);

	private final LigaMeldeListeSheetNew meldeListe;
	private final boolean geradeAnzahlMannschaften;

	public LigaMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, boolean geradeAnzahlMannschaften) {
		super(workingSpreadsheet);
		meldeListe = new LigaMeldeListeSheetNew(workingSpreadsheet);
		this.geradeAnzahlMannschaften = geradeAnzahlMannschaften;
	}

	@Override
	public Logger getLogger() {
		return LigaMeldeListeSheetTestDaten.logger;
	}

	@Override
	protected void doRun() throws GenerateException {

		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.LIGA)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		// clean up first
		getSheetHelper().removeAllSheetsExclude(new String[] {});
		getKonfigurationSheet().setGruppenname(LigaSpielPlanSheetTestDaten.TEST_GRUPPE);
		testNamenEinfuegen();
	}

	/**
	 * @throws GenerateException
	 */
	public void testNamenEinfuegen() throws GenerateException {
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		TurnierSheet.from(meldelisteSheet, getWorkingSpreadsheet()).setActiv();

		List<String> testNamen = listeMitTestNamen();
		Collections.shuffle(testNamen);

		RangeData data = new RangeData();

		int anzTeams = geradeAnzahlMannschaften ? 5 : 6;
		int cntr = 0;
		for (String name : testNamen) {
			RowData newTeam = data.addNewRow();
			newTeam.newEmpty();
			newTeam.newString(name);
			if (cntr++ > anzTeams) {
				break;
			}
		}
		Position posSpielerNr = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		RangeHelper.from(this, data.getRangePosition(posSpielerNr)).setDataInRange(data);
		meldeListe.upDateSheet();
	}

	// Testdaten Generator
	// http://migano.de/testdaten.php

	List<String> listeMitTestNamen() {
		List<String> testNamen = new ArrayList<>();
		testNamen.add("BC-Linden 1");
		testNamen.add("Boule Biebertal");
		testNamen.add("Boule-Freunde Fernwald");
		testNamen.add("PC Petterweil");
		testNamen.add("PSG Ehringshausen 1");
		testNamen.add("DFG Wettenberg 1");
		testNamen.add("Boulefreunde Marburg");
		testNamen.add("Boulodromedare Fulda 2");
		testNamen.add("VNH Hain-Gründau 1");
		return testNamen;
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return meldeListe.getXSpreadSheet();
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return meldeListe.getTurnierSheet();
	}

}
