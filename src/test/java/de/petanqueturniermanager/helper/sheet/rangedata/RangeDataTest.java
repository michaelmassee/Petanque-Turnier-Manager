package de.petanqueturniermanager.helper.sheet.rangedata;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import de.petanqueturniermanager.helper.position.AbstractPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

public class RangeDataTest {

	private RangeData rangeData;

	@Before
	public void setUp() {
		rangeData = new RangeData();
		rangeData.addNewRow(1, 2, 3);
		rangeData.addNewRow(4, 5, 6);
		rangeData.addNewRow(7, 8, 9);
	}

	@Test
	public void testGetAnzSpalten() {
		assertEquals(3, rangeData.getAnzSpalten());
	}

	@Test
	public void testToDataArray() {
		Object[][] expected = { { 1, 2, 3 }, { 4, 5, 6 }, { 7, 8, 9 } };
		assertArrayEquals(expected, rangeData.toDataArray());
	}

	@Test
	public void testAddNewRow() {
		int initialSize = rangeData.size();
		rangeData.addNewRow(10, 11, 12);
		assertEquals(initialSize + 1, rangeData.size());
	}

	@Test
	public void testAddNewSpalte() {
		rangeData.addNewSpalte("A");
		for (RowData rowData : rangeData) {
			assertEquals("A", rowData.get(rowData.size() - 1).getStringVal());
		}
	}

	@Test
	public void testGetRangePosition() {
		AbstractPosition<?> startPosition = Position.from(1, 1); // Assuming 1-based indexing
		RangePosition rangePosition = rangeData.getRangePosition(startPosition);
		assertEquals(startPosition, rangePosition.getStart());
		assertEquals(3, rangePosition.getEnde().getSpalte());
		assertEquals(3, rangePosition.getEnde().getZeile());
	}

	@Test
	public void testRangeDataIntList() throws Exception {
		Object[][] testData = { { 8, 10 }, { 7, 30 }, { 9, 42 } };
		RangeData rangeData = new RangeData(testData);
		assertThat(rangeData).isNotEmpty().hasSize(3);

		List<CellData> collect = rangeData.stream().flatMap(Collection::stream).toList();

		assertThat(collect).isNotEmpty().hasSize(6);
		assertThat(collect).extracting(t -> t.getIntVal(-1)).containsExactly(8, 10, 7, 30, 9, 42);
	}

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
