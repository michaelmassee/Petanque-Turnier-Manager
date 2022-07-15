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

}