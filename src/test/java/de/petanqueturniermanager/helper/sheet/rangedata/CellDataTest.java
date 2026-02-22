package de.petanqueturniermanager.helper.sheet.rangedata;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CellDataTest {

	@Test
	public void testGetData() throws Exception {
		assertThat(new CellData(null).getData()).isNull();
		assertThat(new CellData("wwwee").getData()).isNotNull().isEqualTo("wwwee");
		assertThat(new CellData(200).getData()).isNotNull().isEqualTo(200);
	}

	@Test
	public void testGetIntVal() throws Exception {
		assertThat(new CellData(null).getIntVal(30)).isNotNull().isEqualTo(30);
		assertThat(new CellData("wwwee").getIntVal(99)).isNotNull().isEqualTo(99);
		assertThat(new CellData("500").getIntVal(99)).isNotNull().isEqualTo(500);
		assertThat(new CellData(200).getIntVal(87)).isNotNull().isEqualTo(200);
	}

	@Test
	public void testGetStringVal() throws Exception {
		assertThat(new CellData(null).getStringVal()).isNull();
		assertThat(new CellData("wwwee").getStringVal()).isNotNull().isEqualTo("wwwee");
		assertThat(new CellData("500").getStringVal()).isNotNull().isEqualTo("500");
		assertThat(new CellData(200).getStringVal()).isNotNull().isEqualTo("200");
	}
}