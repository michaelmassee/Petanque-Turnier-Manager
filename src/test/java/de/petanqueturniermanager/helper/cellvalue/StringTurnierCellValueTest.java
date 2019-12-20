package de.petanqueturniermanager.helper.cellvalue;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import de.petanqueturniermanager.helper.position.Position;

public class StringTurnierCellValueTest {

	public void testFromNumberCellValue() throws Exception {
		int spalte = 10;
		int zeile = 30;

		Position pos = Position.from(spalte, zeile);
		NumberTurnierCellValue testNumbCellVal = NumberTurnierCellValue.from(pos, 12);

		StringTurnierCellValue testStrVal = StringTurnierCellValue.from(testNumbCellVal);
		assertThat(testStrVal.getPos().getSpalte()).isEqualTo(spalte);
	}

	@Test
	public void testAppendValue() throws Exception {
		Position pos = Position.from(12, 12);
		StringTurnierCellValue testStrVal = StringTurnierCellValue.from(pos);
		assertThat(testStrVal.getValue()).isEqualTo("");

		testStrVal.appendValue("test");
		assertThat(testStrVal.getValue()).isEqualTo("test");

		testStrVal.appendValue(" test12");
		assertThat(testStrVal.getValue()).isEqualTo("test test12");
	}
}
