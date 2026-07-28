package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import com.sun.star.sheet.XCellRangeFormula;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;
import com.sun.star.text.XText;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

public class SheetHelperTest {

	private SheetHelper sheetHelper = null;
	private XSpreadsheet xSpreadsheetMock;
	private WorkingSpreadsheet workingSpreadsheetMock;

	@BeforeEach
	public void setUp() {
		xSpreadsheetMock = Mockito.mock(XSpreadsheet.class);
		workingSpreadsheetMock = Mockito.mock(WorkingSpreadsheet.class);
		sheetHelper = new SheetHelper(workingSpreadsheetMock);
	}

	@Test
	public void testSetStringValueInCellXSpreadsheetPositionStringBoolean() throws Exception {

		int zeile = 12;
		int spalte = 8;

		String testWert = "Testwert";

		XText xTextMock = Mockito.mock(XText.class);

		sheetHelper = new SheetHelper(workingSpreadsheetMock) {
			@SuppressWarnings("unchecked")
			@Override
			<C> C queryInterface(Class<C> clazz, Object arg) {
				return (C) xTextMock;
			}
		};
		Position testPos = Position.from(spalte, zeile);

		Mockito.when(xTextMock.getString()).thenReturn("     ");
		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, true);// überschreiben
		Mockito.verify(xTextMock, Mockito.times(1)).setString(testWert);

		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, false); // überschreiben weil leer
		Mockito.verify(xTextMock, Mockito.times(2)).setString(testWert);

		Mockito.when(xTextMock.getString()).thenReturn("bla bla");
		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, true); // überschreiben
		Mockito.verify(xTextMock, Mockito.times(3)).setString(testWert);

		sheetHelper.setStringValueInCell(xSpreadsheetMock, testPos, testWert, false); // nicht überschreiben
		Mockito.verify(xTextMock, Mockito.times(3)).setString(any(String.class));
	}

	@Test
	public void testSetFormulaArrayInRange_fehlendesGleichWirdErgaenzt() throws Exception {
		XCellRange xCellRangeMock = Mockito.mock(XCellRange.class,
				Mockito.withSettings().extraInterfaces(XCellRangeFormula.class));
		XCellRangeFormula xCellRangeFormulaMock = (XCellRangeFormula) xCellRangeMock;

		RangePosition rangePos = RangePosition.from(1, 3, 1, 4);
		Mockito.when(xSpreadsheetMock.getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(),
				rangePos.getEndeSpalte(), rangePos.getEndeZeile())).thenReturn(xCellRangeMock);

		String[][] formulas = new String[][] { { "PTM.ALG.TEAMANZEIGE(1;3;1;A1)" }, { "=SUM(A1:A2)" } };

		sheetHelper.setFormulaArrayInRange(xSpreadsheetMock, rangePos, formulas);

		ArgumentCaptor<String[][]> captor = ArgumentCaptor.forClass(String[][].class);
		Mockito.verify(xCellRangeFormulaMock, Mockito.times(1)).setFormulaArray(captor.capture());
		String[][] geschrieben = captor.getValue();

		assertThat(geschrieben[0][0]).isEqualTo("=PTM.ALG.TEAMANZEIGE(1;3;1;A1)");
		assertThat(geschrieben[1][0]).isEqualTo("=SUM(A1:A2)");
	}

	@Test
	public void testSetFormulaArrayInRange_leereZelleBleibtLeer() throws Exception {
		XCellRange xCellRangeMock = Mockito.mock(XCellRange.class,
				Mockito.withSettings().extraInterfaces(XCellRangeFormula.class));
		XCellRangeFormula xCellRangeFormulaMock = (XCellRangeFormula) xCellRangeMock;

		RangePosition rangePos = RangePosition.from(1, 3, 2, 3);
		Mockito.when(xSpreadsheetMock.getCellRangeByPosition(rangePos.getStartSpalte(), rangePos.getStartZeile(),
				rangePos.getEndeSpalte(), rangePos.getEndeZeile())).thenReturn(xCellRangeMock);

		String[][] formulas = new String[][] { { "A1", "" } };

		sheetHelper.setFormulaArrayInRange(xSpreadsheetMock, rangePos, formulas);

		ArgumentCaptor<String[][]> captor = ArgumentCaptor.forClass(String[][].class);
		Mockito.verify(xCellRangeFormulaMock, Mockito.times(1)).setFormulaArray(captor.capture());
		String[][] geschrieben = captor.getValue();

		assertThat(geschrieben[0][0]).isEqualTo("=A1");
		assertThat(geschrieben[0][1]).isEmpty();
	}
}
