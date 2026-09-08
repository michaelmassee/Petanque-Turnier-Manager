/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSheetCondition;
import com.sun.star.sheet.XSheetConditionalEntries;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;
import de.petanqueturniermanager.schweizer.konfiguration.SpielplanTeamAnzeige;

/**
 * Regression für Mêlée-Übernahmen: erscheint ein Spieler in mehreren Teams,
 * muss seine Vor-/Nachname-Kombination durch die rote Fehlerformatierung auffallen.
 */
class MaastrichterMeleeDoppelteNamenFehlerfarbeUITest extends BaseCalcUITest {

	/** Entspricht SchweizerListeDelegate.ERSTE_DATEN_ZEILE (identisches Meldeliste-Layout). */
	private static final int MELDELISTE_ERSTE_DATEN_ZEILE = 3;

	@Test
	void doppelteSpielernamenHabenEineGueltigeFehlerformatierung() throws Exception {
		new MaastrichterMeldeListeSheetNew(wkingSpreadsheet)
				.erstelleMeldeliste(Formation.DOUBLETTE, false, false, SpielplanTeamAnzeige.NR);
		MaastrichterMeldeListeSheetUpdate meldeliste = new MaastrichterMeldeListeSheetUpdate(wkingSpreadsheet);
		XSpreadsheet sheet = meldeliste.getXSpreadSheet();
		int ersteZeile = MELDELISTE_ERSTE_DATEN_ZEILE;

		RangeData data = new RangeData();
		RowData team1 = data.addNewRow();
		team1.newInt(1);
		team1.newString("Anna");
		team1.newString("Müller");
		team1.newString("Peter");
		team1.newString("Schmidt");
		team1.newEmpty();
		team1.newInt(1);
		RowData team2 = data.addNewRow();
		team2.newInt(2);
		team2.newString("Klaus");
		team2.newString("Weber");
		team2.newString("Anna");
		team2.newString("Müller");
		team2.newEmpty();
		team2.newInt(1);
		RangeHelper.from(sheet, doc, data.getRangePosition(Position.from(0, ersteZeile))).setDataInRange(data);

		meldeliste.vollstaendigAktualisieren();

		for (int zeile : List.of(ersteZeile, ersteZeile + 1)) {
			List<String> formeln = conditionalFormatFormeln(sheet, Position.from(meldeliste.getVornameSpalte(0), zeile));
			assertThat(formeln)
					.as("doppelter Spieler in Zeile %d braucht eine COUNTIFS-Fehlerregel", zeile)
					.anySatisfy(formel -> assertThat(formel).containsIgnoringCase("COUNTIFS").doesNotContain("#REF!"));
		}
	}

	private List<String> conditionalFormatFormeln(XSpreadsheet sheet, Position pos) throws GenerateException {
		try {
			XCell cell = sheet.getCellByPosition(pos.getSpalte(), pos.getZeile());
			XPropertySet properties = Lo.qi(XPropertySet.class, cell);
			XSheetConditionalEntries entries = Lo.qi(XSheetConditionalEntries.class,
					properties.getPropertyValue("ConditionalFormat"));
			List<String> formeln = new ArrayList<>();
			for (int i = 0; i < entries.getCount(); i++) {
				formeln.add(Lo.qi(XSheetCondition.class, entries.getByIndex(i)).getFormula1());
			}
			return formeln;
		} catch (Exception e) {
			throw new GenerateException(e.getMessage());
		}
	}
}
