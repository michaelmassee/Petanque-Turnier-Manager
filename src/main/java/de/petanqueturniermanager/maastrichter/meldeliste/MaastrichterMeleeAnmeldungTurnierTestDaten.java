/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.basesheet.meldeliste.MeleeAnmeldungKonstanten;
import de.petanqueturniermanager.basesheet.meldeliste.TurnierSystem;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.NewTestDatenValidator;
import de.petanqueturniermanager.helper.TestnamenLoader;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.maastrichter.konfiguration.MaastrichterKonfigurationSheet;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Maastrichter-Beispiel für die Anmeldung einzelner Spieler vor der Teambildung.
 * <p>
 * Es erstellt eine leere Doublette-Meldeliste und sieben eingecheckte Mêlée-Anmeldungen.
 * Beim Menüpunkt „Mêlée-Anmeldungen übernehmen" werden drei Doublettes angelegt; die siebte
 * Anmeldung bleibt bewusst offen, bis ein weiterer Spieler eingecheckt wird.
 */
public class MaastrichterMeleeAnmeldungTurnierTestDaten extends SheetRunner implements MeleeAnmeldungKonstanten {

	private static final int ANZ_ANMELDUNGEN = 7;

	public MaastrichterMeleeAnmeldungTurnierTestDaten(WorkingSpreadsheet workingSpreadsheet) {
		super(workingSpreadsheet, TurnierSystem.MAASTRICHTER, "Maastrichter-Mêlée-Beispiel");
	}

	@Override
	protected MaastrichterKonfigurationSheet getKonfigurationSheet() {
		return new MaastrichterKonfigurationSheet(getWorkingSpreadsheet());
	}

	@Override
	public void doRun() throws GenerateException {
		if (!NewTestDatenValidator.from(getWorkingSpreadsheet(), getSheetHelper(), TurnierSystem.MAASTRICHTER)
				.prefix(getLogPrefix()).validate()) {
			return;
		}
		getSheetHelper().removeAllSheetsExclude();

		MaastrichterMeldeListeSheetNew meldeliste = new MaastrichterMeldeListeSheetNew(getWorkingSpreadsheet());
		meldeliste.erstelleMeldeliste(Formation.DOUBLETTE, false, false, SpielplanTeamAnzeige.NR);
		new DocumentPropertiesHelper(getWorkingSpreadsheet()).setBooleanProperty(
				BasePropertiesSpalte.KONFIG_PROP_MELEE_ANMELDUNG, true);

		MaastrichterMeleeAnmeldungSheet meleeAnmeldung = new MaastrichterMeleeAnmeldungSheet(getWorkingSpreadsheet());
		meleeAnmeldung.generate();
		anmeldungenEinfuegen(meleeAnmeldung);
		getSheetHelper().setActiveSheet(meleeAnmeldung.getXSpreadSheet());
	}

	private void anmeldungenEinfuegen(MaastrichterMeleeAnmeldungSheet meleeAnmeldung) throws GenerateException {
		var namen = new TestnamenLoader().listeMitSpielerTestNamen(ANZ_ANMELDUNGEN);
		RangeData data = new RangeData();
		for (int i = 0; i < ANZ_ANMELDUNGEN; i++) {
			RowData zeile = data.addNewRow();
			zeile.newInt(i + 1);
			zeile.newString(namen.get(i).vorname());
			zeile.newString(namen.get(i).nachname());
			zeile.newEmpty();
			zeile.newString(MARKIERUNG);
			zeile.newEmpty();
		}
		RangeHelper.from(meleeAnmeldung.getXSpreadSheet(), getWorkingSpreadsheet().getWorkingSpreadsheetDocument(),
				data.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE))).setDataInRange(data);
	}
}
