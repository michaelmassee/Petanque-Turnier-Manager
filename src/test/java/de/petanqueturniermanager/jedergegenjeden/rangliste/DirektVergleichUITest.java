package de.petanqueturniermanager.jedergegenjeden.rangliste;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.BaseCalcUITest;
import de.petanqueturniermanager.addins.GlobalImpl;
import de.petanqueturniermanager.algorithmen.DirektvergleichResult;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;

/**
 * Erstellung 06.08.2022 / Michael Massee
 */

public class DirektVergleichUITest extends BaseCalcUITest {

	static final int ERSTE_ZEILE = 0;
	static final int SPIELPAARUNGEN_SPALTE = 0;
	static final int SIEGE_SPALTE = SPIELPAARUNGEN_SPALTE + 2;
	static final int SPIELPUNKTE_SPALTE = SIEGE_SPALTE + 2;
	static final int FORMULA_SPALTE = SPIELPUNKTE_SPALTE + 2;
	static final int FORMULA_1_ZEILE = ERSTE_ZEILE;
	static final int FORMULA_2_ZEILE = FORMULA_1_ZEILE + 1;
	static final int FORMULA_3_ZEILE = FORMULA_2_ZEILE + 1;
	static final int FORMULA_4_ZEILE = FORMULA_3_ZEILE + 1;
	static final int FORMULA_5_ZEILE = FORMULA_4_ZEILE + 1;

	private static final Logger logger = LogManager.getLogger(DirektVergleichUITest.class);

	private static final String TEST_SHEET_NAME = "Direktvergleich Test";

	@Test
	public void testDirektVergleich() throws IOException, GenerateException {
		// direktvergleich test erstellen
		XSpreadsheet testSheet = sheetHlp.newIfNotExist(TEST_SHEET_NAME, 0);
		sheetHlp.setActiveSheet(testSheet);
		assertThat(testSheet).isNotNull();

		//@formatter:off
		Object [][] spielPaarungen = new Object[][] { 
			{ 1, 2 },
			{ 2, 1 },
			{ 3, 4 },
			{ 4, 3 },
			{ 5, 6 },
			{ 6, 5 },
			{ 7, 8 },
			{ 7, 8 },
			{ 7, 8 }
			};
			
		Object [][] siege = new Object[][] { 
			{ 1, 0 },
			{ 1, 0},
			{ 1, 0},
			{ 1, 0},
			{ 1, 0}, //5,6
			{ 1, 0}, //6,5
			
			{ 1, 0}, //7,8
			{ 0, 1}, //7,8
			{ 1, 0}, //7,8
 			};
			
		Object [][]  spielPunkte = new Object[][] { 
			{ 13, 7 },
			{ 13, 7 },
			{ 13, 7 },
			{ 13, 8 },
			
			{ 13, 8 },//5,6
			{ 13, 7 },//6,5
			
			{ 13, 8 },//7,8
			{ 10, 13 },//7,8
			{ 13, 7 },//7,8
		};
		//@formatter:on

		RangePosition spielPaarungenRangePos = RangeHelper
				.from(testSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), SPIELPAARUNGEN_SPALTE, ERSTE_ZEILE)
				.setDataInRange(spielPaarungen, true).getRangePos();

		RangePosition siegeRangePos = RangeHelper
				.from(testSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), SIEGE_SPALTE, ERSTE_ZEILE)
				.setDataInRange(siege, true).getRangePos();

		RangePosition spielpunkteRangePos = RangeHelper
				.from(testSheet, wkingSpreadsheet.getWorkingSpreadsheetDocument(), SPIELPUNKTE_SPALTE, ERSTE_ZEILE)
				.setDataInRange(spielPunkte, true).getRangePos();

		// Formula einfuegen
		String formula1Str = direktVergleichFormula(1, 2, spielPaarungenRangePos.getAddress(),
				siegeRangePos.getAddress(), spielpunkteRangePos.getAddress());
		StringCellValue formula1 = StringCellValue.from(testSheet).setValue(formula1Str)
				.setPos(Position.from(FORMULA_SPALTE, FORMULA_1_ZEILE));
		sheetHlp.setFormulaInCell(formula1);

		Integer formula1Erg = sheetHlp.getIntFromCell(testSheet, formula1.getPos());
		assertThat(formula1Erg).isEqualTo(DirektvergleichResult.GLEICH.getCode()); // unentschieden

		String formula2Str = direktVergleichFormula(3, 4, spielPaarungenRangePos.getAddress(),
				siegeRangePos.getAddress(), spielpunkteRangePos.getAddress());
		StringCellValue formula2 = StringCellValue.from(testSheet).setValue(formula2Str)
				.setPos(Position.from(FORMULA_SPALTE, FORMULA_2_ZEILE));
		sheetHlp.setFormulaInCell(formula2);

		Integer formula2Erg = sheetHlp.getIntFromCell(testSheet, formula2.getPos());
		assertThat(formula2Erg).isEqualTo(DirektvergleichResult.GEWONNEN.getCode());

		String formula3Str = direktVergleichFormula(5, 6, spielPaarungenRangePos.getAddress(),
				siegeRangePos.getAddress(), spielpunkteRangePos.getAddress());
		StringCellValue formula3 = StringCellValue.from(testSheet).setValue(formula3Str)
				.setPos(Position.from(FORMULA_SPALTE, FORMULA_3_ZEILE));
		sheetHlp.setFormulaInCell(formula3);

		Integer formula3Erg = sheetHlp.getIntFromCell(testSheet, formula3.getPos());
		assertThat(formula3Erg).isEqualTo(DirektvergleichResult.VERLOREN.getCode());

		String formula4Str = direktVergleichFormula(7, 8, spielPaarungenRangePos.getAddress(),
				siegeRangePos.getAddress(), spielpunkteRangePos.getAddress());
		StringCellValue formula4 = StringCellValue.from(testSheet).setValue(formula4Str)
				.setPos(Position.from(FORMULA_SPALTE, FORMULA_4_ZEILE));
		sheetHlp.setFormulaInCell(formula4);

		Integer formula4Erg = sheetHlp.getIntFromCell(testSheet, formula4.getPos());
		assertThat(formula4Erg).isEqualTo(DirektvergleichResult.GEWONNEN.getCode());

		String formula5Str = direktVergleichFormula(8, 7, spielPaarungenRangePos.getAddress(),
				siegeRangePos.getAddress(), spielpunkteRangePos.getAddress());
		StringCellValue formula5 = StringCellValue.from(testSheet).setValue(formula5Str)
				.setPos(Position.from(FORMULA_SPALTE, FORMULA_5_ZEILE));
		sheetHlp.setFormulaInCell(formula5);

		Integer formula5Erg = sheetHlp.getIntFromCell(testSheet, formula5.getPos());
		assertThat(formula5Erg).isEqualTo(DirektvergleichResult.VERLOREN.getCode());

		// waitEnter();

	}

	private String direktVergleichFormula(int tmA, int tmB, String begegnungenVerweis, String spieleVerweis,
			String spielPunkteVerweis) {
		return GlobalImpl.PTM_DIREKTVERGLEICH + "(" + tmA + ";" + tmB + ";" + begegnungenVerweis + ";" + spieleVerweis
				+ ";" + spielPunkteVerweis + ")";
	}

}
