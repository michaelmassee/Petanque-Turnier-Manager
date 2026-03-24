/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;
import de.petanqueturniermanager.schweizer.meldeliste.SchweizerMeldeListeSheetUpdate;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt eine Maastrichter Meldeliste mit Testdaten (ohne Dialog).
 * Formation: Doublette, 12 Teams, Teamname und Vereinsname aktiv.
 */
public class MaastrichterMeldeListeSheetTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	public static final int ANZ_TEAMS_DEFAULT = 12;
	private static final Formation TEST_FORMATION = Formation.DOUBLETTE;

	private final MaastrichterMeldeListeSheetNew meldeListeNew;
	private final MaastrichterKonfigurationSheet konfigSheet;
	private final int anzTeams;

	public MaastrichterMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		this(workingSpreadsheet, ANZ_TEAMS_DEFAULT);
	}

	public MaastrichterMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Meldeliste-Testdaten");
		this.anzTeams = anzTeams;
		meldeListeNew = new MaastrichterMeldeListeSheetNew(workingSpreadsheet);
		konfigSheet = new MaastrichterKonfigurationSheet(workingSpreadsheet);
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
		// SchweizerMeldeListeSheetUpdate für Spaltenzugriff (Format identisch)
		SchweizerMeldeListeSheetUpdate meldeliste = new SchweizerMeldeListeSheetUpdate(getWorkingSpreadsheet());
		XSpreadsheet xSheet = getXSpreadSheet();

		int ersteDatenZeile = meldeliste.getErsteDatenZiele();
		Formation formation = konfigSheet.getMeldeListeFormation();
		int anzSpielerProTeam = formation.getAnzSpieler();
		boolean teamnameAktiv = konfigSheet.isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = konfigSheet.isMeldeListeVereinsnameAnzeigen();

		List<String> testNamen = new TestnamenLoader().listeMitTestNamen(anzTeams * anzSpielerProTeam * 2);

		int nameIdx = 0;
		for (int team = 0; team < anzTeams && nameIdx < testNamen.size(); team++) {
			SheetRunner.testDoCancelTask();
			int zeile = ersteDatenZeile + team;

			// Teamnummer manuell vergeben
			getSheetHelper().setNumberValueInCell(NumberCellValue.from(xSheet,
					Position.from(meldeliste.getTeamNrSpalte(), zeile), team + 1));

			if (teamnameAktiv) {
				getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet,
						Position.from(meldeliste.getTeamnameSpalte(), zeile), "Team " + (team + 1)));
			}

			for (int s = 0; s < anzSpielerProTeam && nameIdx + 1 < testNamen.size(); s++) {
				getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet,
						Position.from(meldeliste.getVornameSpalte(s), zeile), testNamen.get(nameIdx++)));
				getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet,
						Position.from(meldeliste.getNachnameSpalte(s), zeile), testNamen.get(nameIdx++)));
				if (vereinsnameAktiv) {
					getSheetHelper().setStringValueInCell(StringCellValue.from(xSheet,
							Position.from(meldeliste.getVereinsnameSpalte(s), zeile),
							"Verein " + ((team % 5) + 1)));
				}
			}

			// Aktiv-Spalte: alle Teams nehmen teil
			getSheetHelper().setNumberValueInCell(NumberCellValue.from(xSheet,
					Position.from(meldeliste.getAktivSpalte(), zeile),
					SchweizerMeldeListeSheetUpdate.AKTIV_WERT_NIMMT_TEIL));
		}

		// Formatierung + bedingte Färbung anwenden (kein doRun, keine Nummernvergabe nötig)
		meldeliste.upDateSheet();
	}

}
