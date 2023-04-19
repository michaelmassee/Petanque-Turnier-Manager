package de.petanqueturniermanager.helper.sheet.rangedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RangeDataTest {

	@Test
	public void testRangeData() throws Exception {

		Object[][] testData = { { "sadd", 1, 10 }, { "qqwwqqw", 2, 30 }, { "dffdfsdfs", 3, 40 } };

		RangeData rangeData = new RangeData(testData);
		assertThat(rangeData).isNotEmpty().hasSize(3);
		assertThat(rangeData.get(0)).isNotNull().hasSize(3);
		assertThat(rangeData.get(0).getLast()).isNotNull();
		assertThat(rangeData.get(0).getLast().getIntVal(-1)).isEqualTo(10);
		RowData elem1 = rangeData.get(0);
		assertEquals("sadd", elem1.get(0).getStringVal());
	}

	@Test
	public void testRangeDataSinglelist() throws Exception {
		Object[] testData = { "sadd", "qqwwqqw", "dffdfsdfs" };
		RangeData rangeData = new RangeData(Arrays.asList(testData));
		assertThat(rangeData).isNotNull().hasSize(3);

		List<String> expected0 = Arrays.asList(new String[] { "sadd" });
		assertThat(rangeData.get(0)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected0);

		List<String> expected1 = Arrays.asList(new String[] { "qqwwqqw" });
		assertThat(rangeData.get(1)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected1);

		List<String> expected2 = Arrays.asList(new String[] { "dffdfsdfs" });
		assertThat(rangeData.get(2)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected2);

	}

	@Test
	public void testRangeDataIntIntegerArray() {
		RangeData rangeData = new RangeData(5, 10, 4, 8);

		assertThat(rangeData).hasSize(5);
		assertThat(rangeData.get(0)).hasSize(3);
		assertThat(rangeData.get(0).get(0).getIntVal(0)).isEqualTo(10);
		assertThat(rangeData.get(0).get(1).getIntVal(0)).isEqualTo(4);
		assertThat(rangeData.get(0).get(2).getIntVal(0)).isEqualTo(8);

	}

}
