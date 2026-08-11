/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.sun.star.beans.XPropertySet;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCell;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.basesheet.meldeliste.Formation;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ColorHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Regressionstest (Bug-Report): nach Korrektur eines doppelten Spielernamens blieb die zuvor rot
 * markierte Schrift dauerhaft rot, weil {@code MeldeListeHelper.testDoppelteMeldungen()} nur beim
 * jeweils ERSTEN gefundenen Duplikat eine Zeile rot färbt und danach abbricht (throw) - eine
 * inzwischen korrigierte, nicht mehr doppelte Zeile wurde nie aktiv zurück auf Schwarz gesetzt.
 */
class SchweizerMeldeListeDoppelteNamenFarbeZuruecksetzenUITest extends BaseCalcUITest {

	@Test
	void korrigierterDoppelterNameWirdWiederSchwarz() throws Exception {
		SchweizerMeldeListeSheetNew meldeListeNew = new SchweizerMeldeListeSheetNew(wkingSpreadsheet);
		meldeListeNew.createMeldelisteWithParams(Formation.TETE, false, false);

		int ersteDatenZeile = SchweizerListeDelegate.ERSTE_DATEN_ZEILE;
		int vornameSpalte = meldeListeNew.getVornameSpalte(0);
		int nachnameSpalte = meldeListeNew.getNachnameSpalte(0);
		XSpreadsheet xSheet = meldeListeNew.getXSpreadSheet();

		RangeData data = new RangeData();
		for (int team = 1; team <= 2; team++) {
			RowData zeile = data.addNewRow();
			zeile.newInt(team);
			zeile.newString("Anna");
			zeile.newString("Duplikat");
			zeile.newEmpty(); // Setzposition
			zeile.newInt(SchweizerListeDelegate.AKTIV_WERT_NIMMT_TEIL);
		}
		RangeHelper.from(xSheet, doc, data.getRangePosition(Position.from(0, ersteDatenZeile))).setDataInRange(data);

		// 1. Sync: Duplikat wird erkannt und eine Zeile rot markiert.
		assertThatThrownBy(meldeListeNew::upDateSheet).isInstanceOf(GenerateException.class);

		boolean roteZeileGefunden = false;
		for (int zeile = ersteDatenZeile; zeile <= ersteDatenZeile + 1; zeile++) {
			if (istRot(xSheet, vornameSpalte, zeile)) {
				roteZeileGefunden = true;
				break;
			}
		}
		assertThat(roteZeileGefunden).as("nach dem ersten Sync muss eine Zeile rot markiert sein").isTrue();

		// 2. Duplikat korrigieren.
		meldeListeNew.getSheetHelper().setStringValueInCell(
				StringCellValue.from(xSheet, Position.from(nachnameSpalte, ersteDatenZeile + 1), "Andere"));

		// 3. Erneuter Sync: darf keine Zeile mehr rot markiert lassen.
		meldeListeNew.upDateSheet();

		for (int zeile = ersteDatenZeile; zeile <= ersteDatenZeile + 1; zeile++) {
			assertThat(istRot(xSheet, vornameSpalte, zeile))
					.as("Zeile %d muss nach Korrektur des Duplikats wieder schwarz sein", zeile).isFalse();
			assertThat(istRot(xSheet, nachnameSpalte, zeile))
					.as("Zeile %d muss nach Korrektur des Duplikats wieder schwarz sein", zeile).isFalse();
		}
	}

	private boolean istRot(XSpreadsheet sheet, int spalte, int zeile) throws Exception {
		XCell xCell = sheet.getCellByPosition(spalte, zeile);
		XPropertySet props = Lo.qi(XPropertySet.class, xCell);
		int charColor = (Integer) props.getPropertyValue("CharColor");
		return charColor == ColorHelper.CHAR_COLOR_RED;
	}
}
