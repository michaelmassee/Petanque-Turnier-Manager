/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.i18n.SheetNamen;

/** Ein Reiter mit überholter Übersetzung wird umbenannt; Calc zieht vorhandene Formelbezüge mit. */
class AlteBlattnamenUITest extends BaseCalcUITest {

	private static final String ALTER_NAME = "Deelnemerslijst";

	@Test
	void alterReiterWirdUmbenanntUndFormelnBleibenGueltig() throws Exception {
		XSpreadsheets sheets = doc.getSheets();
		sheets.insertNewByName(ALTER_NAME, (short) 0);
		sheets.insertNewByName("Runde", (short) 1);
		Lo.qi(XSpreadsheet.class, sheets.getByName(ALTER_NAME)).getCellByPosition(0, 0).setValue(5);
		XSpreadsheet runde = Lo.qi(XSpreadsheet.class, sheets.getByName("Runde"));
		runde.getCellByPosition(0, 0).setFormula("=$'" + ALTER_NAME + "'.A1*2");

		AlteBlattnamen.heile(wkingSpreadsheet);
		AlteBlattnamen.heile(wkingSpreadsheet);

		assertThat(sheets.hasByName(SheetNamen.meldeliste())).isTrue();
		assertThat(sheets.hasByName(ALTER_NAME)).isFalse();
		assertThat(runde.getCellByPosition(0, 0).getFormula()).contains(SheetNamen.meldeliste());
		assertThat(runde.getCellByPosition(0, 0).getValue()).isEqualTo(10);
	}

	@Test
	void vorhandenerAktuellerReiterBleibtUnangetastet() throws Exception {
		XSpreadsheets sheets = doc.getSheets();
		sheets.insertNewByName(SheetNamen.meldeliste(), (short) 0);
		sheets.insertNewByName(ALTER_NAME, (short) 1);

		AlteBlattnamen.heile(wkingSpreadsheet);

		assertThat(sheets.hasByName(SheetNamen.meldeliste())).isTrue();
		assertThat(sheets.hasByName(ALTER_NAME)).isTrue();
	}
}
