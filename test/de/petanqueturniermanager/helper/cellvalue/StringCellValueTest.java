/**
* Erstellung : 29.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.cellvalue.NumberCellValue;
import de.petanqueturniermanager.helper.cellvalue.StringCellValue;
import de.petanqueturniermanager.helper.position.Position;

@RunWith(PowerMockRunner.class)
public class StringCellValueTest {
	XSpreadsheet spreadsheetMock;

	@Before
	public void setup() {
		this.spreadsheetMock = PowerMockito.mock(XSpreadsheet.class);
	}

	@Test
	public void testFromNumberCellValue() throws Exception {
		int spalte = 10;
		int zeile = 30;

		Position pos = Position.from(spalte, zeile);
		NumberCellValue testNumbCellVal = NumberCellValue.from(this.spreadsheetMock, pos, 12);

		StringCellValue testStrVal = StringCellValue.from(testNumbCellVal);
		assertThat(testStrVal.getPos().getSpalte()).isEqualTo(spalte);
	}

}
