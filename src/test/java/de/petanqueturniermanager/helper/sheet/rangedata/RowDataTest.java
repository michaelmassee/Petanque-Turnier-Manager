package de.petanqueturniermanager.helper.sheet.rangedata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

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

}