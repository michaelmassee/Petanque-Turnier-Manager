/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.cellvalue.properties.ColumnProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.position.Position;

@RunWith(PowerMockRunner.class)
public class NumberCellValueTest {

	XSpreadsheet spreadsheetMock;

	@Before
	public void setup() {
		spreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
	}

	@Test
	public void testFromNumberCellValue() throws Exception {
		int spalte = 10;
		int zeile = 30;

		Position pos = Position.from(spalte, zeile);
		NumberCellValue intcellValue = NumberCellValue.from(spreadsheetMock, pos, 42);
		String aComment = "bla bla";
		intcellValue.setComment(aComment);
		assertThat(intcellValue.getValue()).isEqualTo(42);
		assertThat(intcellValue.getComment()).isEqualTo(aComment);

		NumberCellValue intcellValueNew = NumberCellValue.from(intcellValue);
		assertThat(intcellValueNew.getValue()).isEqualTo(42);
		assertThat(intcellValueNew.getComment()).isEqualTo(aComment);
		assertThat(intcellValueNew.getPos().getSpalte()).isEqualTo(spalte);
		assertThat(intcellValueNew.getPos().getZeile()).isEqualTo(zeile);

		// test if correct clone
		intcellValueNew.setValue((double) 888);
		assertThat(intcellValue.getValue()).isEqualTo(42); // orginal bleibt
		assertThat(intcellValueNew.getValue()).isEqualTo(888);

		intcellValueNew.getPos().spaltePlusEins();
		assertThat(intcellValue.getPos().getSpalte()).isEqualTo(spalte); // orginal bleibt
		assertThat(intcellValueNew.getPos().getSpalte()).isEqualTo(spalte + 1);
	}

	@Test
	public void testFromStringCellValue() throws Exception {

		int spalte = 10;
		int zeile = 30;
		double val = 3412;
		String testComent = "bla bla";
		int columnWidth = 3000;
		int charcolor = 123465;

		Position pos = Position.from(spalte, zeile);
		ColumnProperties columnProperties = ColumnProperties.from().setWidth(columnWidth);

		StringCellValue strcellValue = StringCellValue.from(spreadsheetMock, pos, "" + val).setComment(testComent).addColumnProperties(columnProperties)
				.addCellProperty(ICommonProperties.CHAR_COLOR, charcolor).nichtUeberschreiben();

		NumberCellValue numberCellValue = NumberCellValue.from(strcellValue);

		assertThat(numberCellValue.getPos().getZeile()).isEqualTo(zeile);
		assertThat(numberCellValue.getPos().getSpalte()).isEqualTo(spalte);
		assertThat(numberCellValue.getValue()).isEqualTo(val);
		assertThat(numberCellValue.getComment()).isEqualTo(testComent);
		assertThat(numberCellValue.getColumnProperties()).containsEntry(ICommonProperties.WIDTH, columnWidth);
		assertThat(numberCellValue.getCellProperties()).containsEntry(ICommonProperties.CHAR_COLOR, charcolor);
		assertThat(numberCellValue.isUeberschreiben()).isFalse();
		assertThat(numberCellValue.getSheet()).isNotNull();
	}

	@Test
	public void testFromXSpreadsheetIntInt() throws Exception {
		int spalte = 12;
		int zeile = 700;

		NumberCellValue result = NumberCellValue.from(spreadsheetMock, spalte, zeile);
		assertThat(result.getPos().getZeile()).isEqualTo(zeile);
		assertThat(result.getPos().getRow()).isEqualTo(zeile);
		assertThat(result.getPos().getSpalte()).isEqualTo(spalte);
		assertThat(result.getPos().getColumn()).isEqualTo(spalte);
	}

	@Test
	public void testFromXSpreadsheetIntIntDouble() throws Exception {
		int spalte = 12;
		int zeile = 700;
		double val = 4500D;

		NumberCellValue result = NumberCellValue.from(spreadsheetMock, spalte, zeile, val);
		assertThat(result.getPos().getZeile()).isEqualTo(zeile);
		assertThat(result.getPos().getRow()).isEqualTo(zeile);
		assertThat(result.getPos().getSpalte()).isEqualTo(spalte);
		assertThat(result.getPos().getColumn()).isEqualTo(spalte);
		assertThat(result.getValue()).isEqualTo(val);
	}

	@Test
	public void testFromXSpreadsheetPosition() throws Exception {
		int spalte = 19;
		int zeile = 389;

		NumberCellValue result = NumberCellValue.from(spreadsheetMock, Position.from(spalte, zeile));
		assertThat(result.getPos().getZeile()).isEqualTo(zeile);
		assertThat(result.getPos().getRow()).isEqualTo(zeile);
		assertThat(result.getPos().getSpalte()).isEqualTo(spalte);
		assertThat(result.getPos().getColumn()).isEqualTo(spalte);
	}
}