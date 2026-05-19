package de.petanqueturniermanager.triptete.meldeliste;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.triptete.konfiguration.TripTeteKonfigurationSheet;

/**
 * TestDaten-Generator für die Trip-Tête-Meldeliste.
 */
public class TripTeteMeldeListeSheetTestDaten extends SheetRunner implements ISheet {

	private final TripTeteKonfigurationSheet konfigurationSheet;
	private final TripTeteMeldeListeSheetNew meldeListe;
	private final int anzTeams;

	public TripTeteMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, 6);
	}

	public TripTeteMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.TRIPTETE);
		konfigurationSheet = new TripTeteKonfigurationSheet(workingSpreadsheet);
		meldeListe = new TripTeteMeldeListeSheetNew(workingSpreadsheet);
		this.anzTeams = anzTeams;
	}

	@Override
	protected TripTeteKonfigurationSheet getKonfigurationSheet() {
		return konfigurationSheet;
	}

	public void generate() throws GenerateException {
		doRun();
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.TRIPTETE)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude(new String[] {});
		meldeListe.createMeldeliste();
		testNamenEinfuegen();
	}

	public void erstelleUndFuelleTestDaten() throws GenerateException {
		meldeListe.createMeldeliste();
		testNamenEinfuegen();
	}

	public void testNamenEinfuegen() throws GenerateException {
		XSpreadsheet meldelisteSheet = meldeListe.getXSpreadSheet();
		TurnierSheet.from(meldelisteSheet, getWorkingSpreadsheet()).setActiv();

		List<String> testNamen = listeMitTestTeamNamen();
		Collections.shuffle(testNamen, RandomSource.asJavaRandom());

		RangeData data = new RangeData();
		int cntr = 0;
		for (String name : testNamen) {
			RowData newTeam = data.addNewRow();
			newTeam.newEmpty();
			newTeam.newString(name);
			if (++cntr >= anzTeams) {
				break;
			}
		}
		Position posSpielerNr = Position.from(MeldeListeKonstanten.SPIELER_NR_SPALTE,
				MeldeListeKonstanten.ERSTE_DATEN_ZEILE - 1);
		RangeHelper.from(this, data.getRangePosition(posSpielerNr)).setDataInRange(data);
		meldeListe.upDateSheet();
	}

	List<String> listeMitTestTeamNamen() {
		List<String> testNamen = new ArrayList<>();
		testNamen.add("Trio Wiesbaden");
		testNamen.add("Trip-Trip Mainz");
		testNamen.add("Boule-Bande Frankfurt");
		testNamen.add("3er Pack Darmstadt");
		testNamen.add("PC Petterweil");
		testNamen.add("Boule-Freunde Fernwald");
		testNamen.add("Pétanque-Kreis Kassel");
		testNamen.add("Trip-Three Köln");
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
