package de.petanqueturniermanager.helper.sheet.rangedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

public class RowDataTest {

	@Test
	public void testRowDataSingle() throws Exception {
		RowData rwData = new RowData("testtest");
		assertThat(rwData).hasSize(1);
		assertThat(rwData.get(0)).hasFieldOrPropertyWithValue("data", "testtest");
	}

	@Test
	public void testRowDataIntegerArray() {
		RowData rwData = new RowData(10, 5, 20);
		assertThat(rwData).hasSize(3);
		assertThat(rwData.get(0).getIntVal(0)).isEqualTo(10);
		assertThat(rwData.get(1).getIntVal(0)).isEqualTo(5);
		assertThat(rwData.get(2).getIntVal(0)).isEqualTo(20);
	}

	@Test
	public void testLeererKonstruktor_und_newString_newInt_newEmpty() {
		RowData rwData = new RowData();
		assertThat(rwData).isEmpty();

		rwData.newString("a");
		rwData.newInt(42);
		rwData.newEmpty();

		assertThat(rwData).hasSize(3);
		assertThat(rwData.get(0).getStringVal()).isEqualTo("a");
		assertThat(rwData.get(1).getIntVal(-1)).isEqualTo(42);
		assertThat(rwData.get(2).getStringVal()).isEmpty();
	}

	@Test
	public void testGetLast_aufLeererRow_wirftIndexOutOfBounds() {
		RowData rwData = new RowData();
		assertThatThrownBy(rwData::getLast).isInstanceOf(IndexOutOfBoundsException.class);
	}

	@Test
	public void testGetLast_liefertLetztesElement() {
		RowData rwData = new RowData();
		rwData.newInt(1);
		rwData.newInt(2);
		rwData.newInt(3);

		assertThat(rwData.getLast().getIntVal(-1)).isEqualTo(3);
	}

	@Test
	public void testIteratorReihenfolgeEntsprichtInsertionOrder() {
		RowData rwData = new RowData();
		rwData.newString("erst");
		rwData.newString("zweit");
		rwData.newString("dritt");

		assertThat(rwData).extracting(CellData::getStringVal).containsExactly("erst", "zweit", "dritt");
	}

	@Test
	public void testToDataArray_fuelltMitLeerstringsAuf() {
		RowData rwData = new RowData();
		rwData.newString("x");
		rwData.newInt(7);

		Object[] result = rwData.toDataArray(5);
		assertThat(result).hasSize(5);
		assertThat(result[0]).isEqualTo("x");
		assertThat(result[1]).isEqualTo(7);
		assertThat(result[2]).isEqualTo("");
		assertThat(result[3]).isEqualTo("");
		assertThat(result[4]).isEqualTo("");
	}

	@Test
	public void testToDataArray_arraySizeKleinerAlsRow_wirftIllegalArgument() {
		RowData rwData = new RowData(1, 2, 3);
		assertThatThrownBy(() -> rwData.toDataArray(2))
				.isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	public void testMischtypen_intUndString() {
		Object[] data = { "label", 42, "ende" };
		RowData rwData = new RowData(data);
		assertThat(rwData).hasSize(3);
		assertThat(rwData.get(0).getStringVal()).isEqualTo("label");
		assertThat(rwData.get(1).getIntVal(-1)).isEqualTo(42);
		assertThat(rwData.get(2).getStringVal()).isEqualTo("ende");
	}
}
