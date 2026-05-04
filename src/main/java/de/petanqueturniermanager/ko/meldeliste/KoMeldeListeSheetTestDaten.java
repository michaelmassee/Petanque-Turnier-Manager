/**
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.ko.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.SheetRunner;
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
	private final TestnamenLoader testnamenLoader;

	public KoMeldeListeSheetTestDaten(WorkingSpreadsheet workingSpreadsheet, int anzTeams) {
		super(workingSpreadsheet, TurnierSystem.KO, "KO-Meldeliste-Testdaten");
		this.anzTeams = anzTeams;
		delegate = new KoListeDelegate(this);
		meldeListeNew = new KoMeldeListeSheetNew(workingSpreadsheet);
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
		var konfiguration = delegate.getKonfigurationSheet();
		int anzSpieler = konfiguration.getMeldeListeFormation().getAnzSpieler();
		boolean teamnameAktiv = konfiguration.isMeldeListeTeamnameAnzeigen();
		boolean vereinsnameAktiv = konfiguration.isMeldeListeVereinsnameAnzeigen();
		var spieler = testnamenLoader.listeMitSpielerTestNamen(anzTeams * anzSpieler);

		var data = new RangeData();
		for (int i = 0; i < anzTeams; i++) {
			testDoCancelTask();
			var zeile = data.addNewRow();
			if (teamnameAktiv) {
				zeile.newString("Team " + (i + 1));
			}
			for (int s = 0; s < anzSpieler; s++) {
				var stn = spieler.get(i * anzSpieler + s);
				zeile.newString(stn.vorname());
				zeile.newString(stn.nachname());
				if (vereinsnameAktiv) {
					zeile.newString("Verein " + ((i % 5) + 1));
				}
			}
			zeile.newInt(i + 1); // Ranglistespalte (Setzreihenfolge)
			zeile.newInt(KoListeDelegate.AKTIV_WERT_NIMMT_TEIL);
		}

		var xSheet = meldeListeNew.getXSpreadSheet();
		var startPos = Position.from(1, KoListeDelegate.ERSTE_DATEN_ZEILE);
		RangeHelper.from(xSheet, getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				data.getRangePosition(startPos)).setDataInRange(data);
	}

}
