/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
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
import de.petanqueturniermanager.ko.konfiguration.KoKonfigurationSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellt eine K.-O.-Meldeliste mit Testdaten (ohne Dialog).<br>
 * Kann alleinstehend verwendet werden ("Nur Meldeliste") oder
 * von KoTurnierTestDaten als Vorstufe für den Turnierbaum.
 */
public class KoMeldeListeSheetTestDaten extends SheetRunner implements ISheet, MeldeListeKonstanten {

	private final int anzTeams;
	private final KoListeDelegate delegate;
	private final KoMeldeListeSheetNew meldeListeNew;

	public KoMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Meldeliste-Testdaten");
		this.anzTeams = anzTeams;
		delegate = new KoListeDelegate(this);
		meldeListeNew = new KoMeldeListeSheetNew(workingSpreadsheet);
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
	protected KoKonfigurationSheet getKonfigurationSheet() {
		return delegate.getKonfigurationSheet();
	}

	/**
	 * Füllt die Meldeliste mit Testnamen und führt danach ein Update durch.
	 * Wird auch von KoTurnierTestDaten aufgerufen.
	 */
	public void erstelleMeldelisteWithTestdaten() throws GenerateException {
		getSheetHelper().removeAllSheetsExclude();

		// Konfigurationsblatt und Meldeliste anlegen
		getKonfigurationSheet().update();
		meldeListeNew.createMeldelisteWithParams();

		// Testnamen einfügen
		teamNamenEinfuegen();

		// Nummern vergeben + sortieren + formatieren
		new KoMeldeListeSheetUpdate(getWorkingSpreadsheet()).doRun();
	}

	@Override
	protected void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.KO)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		erstelleMeldelisteWithTestdaten();
	}

	// ---------------------------------------------------------------

	private void teamNamenEinfuegen() throws GenerateException {
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();
		getSheetHelper().setActiveSheet(xSheet);

		List<String> teamnamen = new TestnamenLoader().listeMitTestNamen(anzTeams);
		int teamnameSpalte = delegate.getTeamnameSpalte();
		int aktivSpalte = delegate.getAktivSpalte();
		int ersteDatenZeile = KoListeDelegate.ERSTE_DATEN_ZEILE;

		for (int i = 0; i < anzTeams && i < teamnamen.size(); i++) {
			SheetRunner.testDoCancelTask();
			int zeile = ersteDatenZeile + i;
			getSheetHelper().setStringValueInCell(
					StringCellValue.from(xSheet, Position.from(teamnameSpalte, zeile), teamnamen.get(i)));
			getSheetHelper().setNumberValueInCell(
					NumberCellValue.from(xSheet, Position.from(aktivSpalte, zeile),
							KoListeDelegate.AKTIV_WERT_NIMMT_TEIL));
		}
	}

}
