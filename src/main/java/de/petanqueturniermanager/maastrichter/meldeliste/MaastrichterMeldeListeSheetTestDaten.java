/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;

/**
 * Erstellt eine Maastrichter Meldeliste mit Testdaten (ohne Dialog).
 * Formation: Doublette, 12 Teams, Teamname und Vereinsname aktiv.
 */
public class MaastrichterMeldeListeSheetTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	public static final int ANZ_TEAMS_DEFAULT = 12;
	private static final Formation TEST_FORMATION = Formation.DOUBLETTE;

	private final MaastrichterMeldeListeSheetNew meldeListeNew;
	private final MaastrichterKonfigurationSheet konfigSheet;
	private final TestnamenLoader testnamenLoader;
	private final int anzTeams;

	public MaastrichterMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, ANZ_TEAMS_DEFAULT);
	}

	public MaastrichterMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Meldeliste-Testdaten");
		this.anzTeams = anzTeams;
		meldeListeNew = new MaastrichterMeldeListeSheetNew(workingSpreadsheet);
		konfigSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
		testnamenLoader = new TestnamenLoader();
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return getSheetHelper().findByName(SheetNamen.meldeliste());
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return konfigSheet;
	}

	/** Öffentlicher Einstiegspunkt für externe Testdaten-Generatoren. */
	public void erstelleTestdaten() throws GenerateException {
		doRun();
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.MAASTRICHTER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}

		getSheetHelper().removeAllSheetsExclude();

		// Konfiguration initialisieren (setzt TurnierSystem=MAASTRICHTER im Dokument)
		konfigSheet.update();
		konfigSheet.setAnzVorrunden(3);
		konfigSheet.setAktiveSpielRunde(SpielRundeNr.from(1));

		// Meldeliste erstellen (kein Dialog)
		meldeListeNew.erstelleMeldeliste(TEST_FORMATION, true, true, SpielplanTeamAnzeige.NR);

		// Testnamen + Teamnummern einfügen und Sheet aktualisieren
		testNamenEinfuegen();
	}

	private void testNamenEinfuegen() throws GenerateException {
		var meldeliste = new MaastrichterMeldeListeSheetUpdate(getWorkingSpreadsheet());
		int ersteDatenZeile = meldeliste.getErsteDatenZiele();
		int anzSpielerProTeam = konfigSheet.getMeldeListeFormation().getAnzSpieler();
		boolean teamnameAktiv = konfigSheet.isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = konfigSheet.isMeldeListeVereinsnameAnzeigen();
		var spieler = testnamenLoader.listeMitSpielerTestNamen(anzTeams * anzSpielerProTeam);

		var data = new RangeData();
		for (int team = 0; team < anzTeams; team++) {
			testDoCancelTask();
			var zeile = data.addNewRow();
			zeile.newInt(team + 1); // TeamNr - manuell vergeben
			if (teamnameAktiv) {
				zeile.newString("Team " + (team + 1));
			}
			for (int s = 0; s < anzSpielerProTeam; s++) {
				var stn = spieler.get(team * anzSpielerProTeam + s);
				zeile.newString(stn.vorname());
				zeile.newString(stn.nachname());
				if (vereinsnameAktiv) {
					zeile.newString("Verein " + ((team % 5) + 1));
				}
			}
			zeile.newEmpty(); // Setzposition
			zeile.newInt(MaastrichterMeldeListeSheetUpdate.AKTIV_WERT_NIMMT_TEIL);
		}

		var xSheet = getXSpreadSheet();
		var startPos = Position.from(0, ersteDatenZeile);
		RangeHelper.from(xSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				data.getRangePosition(startPos)).setDataInRange(data);

		// Formatierung + bedingte Färbung anwenden (kein doRun, keine Nummernvergabe nötig)
		meldeliste.upDateSheet();
	}

}
