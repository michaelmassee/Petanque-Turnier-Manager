/**
* Erstellung : 29.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

public class StringCellValueTest {
	XSpreadsheet spreadsheetMock;

	@BeforeEach
	public void setup() {
		spreadsheetMock = Mockito.mock(XSpreadsheet.class);
	}

	@Test
	public void testFromNumberCellValue() throws Exception {
		int spalte = 10;
		int zeile = 30;

		Position pos = Position.from(spalte, zeile);
		NumberCellValue testNumbCellVal = NumberCellValue.from(spreadsheetMock, pos, 12);

		StringCellValue testStrVal = StringCellValue.from(testNumbCellVal);
		assertThat(testStrVal.getPos().getSpalte()).isEqualTo(spalte);
		assertThat(testStrVal.getSheet()).isNotNull();
	}

	@Test
	public void testAppendValue() throws Exception {
		Position pos = Position.from(12, 12);
		StringCellValue testStrVal = StringCellValue.from(spreadsheetMock, pos);
		assertThat(testStrVal.getValue()).isEqualTo("");

		testStrVal.appendValue("test");
		assertThat(testStrVal.getValue()).isEqualTo("test");

		testStrVal.appendValue(" test12");
		assertThat(testStrVal.getValue()).isEqualTo("test test12");
	}

}
