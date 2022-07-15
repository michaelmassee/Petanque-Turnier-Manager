package de.petanqueturniermanager.helper.sheet.rangedata;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

public class RangeDataTest {

	@Test
	public void testRangeData() throws Exception {

		Object[][] testData = { { "sadd", 1, 10 }, { "qqwwqqw", 2, 30 }, { "dffdfsdfs", 3, 40 } };

		RangeData rangeData = new RangeData(testData);
		assert !rangeData.isEmpty();
		assert rangeData.size() == 3;
		assert rangeData.get(0) != null;
		assert rangeData.get(0).getLast() != null;
		assert rangeData.get(0).getLast().getIntVal(-1) == 10;
		assert rangeData.get(0).size() == 3;
		RowData elem1 = rangeData.get(0);
		assert elem1.get(0).getStringVal().equals("sadd");
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

}
