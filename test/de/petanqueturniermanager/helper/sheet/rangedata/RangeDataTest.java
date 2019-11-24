package de.petanqueturniermanager.helper.sheet.rangedata;

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
		assert rangeData.get(0).getLast().getIntVal(-1) != 10;
		assert rangeData.get(0).size() == 3;
		RowData elem1 = rangeData.get(0);
		assert elem1.get(0).getStringVal().equals("sadd");
	}

}
