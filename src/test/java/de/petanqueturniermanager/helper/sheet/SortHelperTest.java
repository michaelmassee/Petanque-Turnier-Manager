package de.petanqueturniermanager.helper.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sun.star.frame.XComponentLoader;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.comp.OfficeDocumentHelper;
import de.petanqueturniermanager.comp.OfficeStarter;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.rangedata.CellData;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

public class SortHelperTest {
	static XComponentLoader loader;
	static XSpreadsheetDocument doc;
	static OfficeStarter starter = OfficeStarter.from();

	@BeforeClass
	public static void startup() {
		loader = starter.loadOffice().getComponentLoader();
		doc = OfficeDocumentHelper.from(loader).createCalc();
		if (doc == null) {
			System.out.println("Document creation failed");
			return;
		}
		OfficeDocumentHelper.setVisible(doc, true);
	}

	@AfterClass
	public static void shutDown() {
		if (doc != null) {
			OfficeDocumentHelper.closeDoc(doc);
		}
		starter.closeOffice();
	}

	@Test
	public void testDoSort() throws Exception {
		SheetHelper sheetHlp = new SheetHelper(starter.getxComponentContext(), doc);
		XSpreadsheet sheet = sheetHlp.getSheetByIdx(0);

		// @formatter:off
	    Object[][] vals = {
	    	      { "Level", "Code", "No.", "Team", "Name" },
	    	      { "BS", 20, 4, "C", "Sweet" },
	    	      { "BS", 20, 4, "B", "Elle" },
	    	      { "BS", 20, 4, "B", "Ally" },
	    	      { "BS", 20, 4, "B", "Chcomic" },
	    	      { "BS", 20, 2, "A", "Chcomic" }, 
	    	      { "CS", 30, 5, "A", "Ally" },
	    	      { "MS", 10, 1, "A", "Joker" }, 
	    	      { "MS", 10, 3, "B", "Kevin" },
	    	      { "CS", 30, 7, "C", "Tom" } };
		// @formatter:on
		RangeData data = new RangeData(vals);
		Position startPos = Position.from(0, 0);
		RangePosition rangePos = RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).setDataInRange(data)
				.getRangePos();

		assertThat(rangePos.getAddress()).isEqualTo("A1:E10");

		// in 2 schritte weil max 3 spalten to sort
		int[] sortSpalten = { 3, 4 }; // 0 = erste spalte
		SortHelper.from(sheet, doc, rangePos).aufSteigendSortieren().spaltenToSort(sortSpalten).doSort();

		// in 2 schritte weil max 3 spalten to sort
		int[] sortSpalten2 = { 0, 1, 2 }; // 0 = erste spalte
		SortHelper.from(sheet, doc, rangePos).aufSteigendSortieren().spaltenToSort(sortSpalten2).doSort();

		RangeData dataFromRange = RangeHelper.from(sheet, doc, data.getRangePosition(startPos)).getDataFromRange();

		assertThat(dataFromRange).hasSize(10);

		List<String> expected = Arrays.asList(new String[] { "Level", "Code", "No.", "Team", "Name" });
		assertThat(dataFromRange.get(0)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected);

		//		BS	20	2	A	Chcomic
		//		BS	20	4	B	Ally
		//		BS	20	4	B	Chcomic
		//		BS	20	4	B	Elle
		//		BS	20	4	C	Sweet

		List<String> expected1 = Arrays.asList(new String[] { "BS", "20", "2", "A", "Chcomic" });
		assertThat(dataFromRange.get(1)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected1);
		List<String> expected2 = Arrays.asList(new String[] { "BS", "20", "4", "B", "Ally" });
		assertThat(dataFromRange.get(2)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected2);
		List<String> expected3 = Arrays.asList(new String[] { "BS", "20", "4", "B", "Chcomic" });
		assertThat(dataFromRange.get(3)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected3);
		List<String> expected4 = Arrays.asList(new String[] { "BS", "20", "4", "B", "Elle" });
		assertThat(dataFromRange.get(4)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected4);
		List<String> expected5 = Arrays.asList(new String[] { "BS", "20", "4", "C", "Sweet" });
		assertThat(dataFromRange.get(5)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected5);

		//		CS	30	5	A	Ally
		//		CS	30	7	C	Tom
		//		MS	10	1	A	Joker
		//		MS	10	3	B	Kevin

		List<String> expected6 = Arrays.asList(new String[] { "CS", "30", "5", "A", "Ally" });
		assertThat(dataFromRange.get(6)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected6);
		List<String> expected7 = Arrays.asList(new String[] { "CS", "30", "7", "C", "Tom" });
		assertThat(dataFromRange.get(7)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected7);
		List<String> expected8 = Arrays.asList(new String[] { "MS", "10", "1", "A", "Joker" });
		assertThat(dataFromRange.get(8)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected8);
		List<String> expected9 = Arrays.asList(new String[] { "MS", "10", "3", "B", "Kevin" });
		assertThat(dataFromRange.get(9)).extracting(CellData::getStringVal).containsExactlyElementsOf(expected9);

		// Lo.saveDoc(doc, "dataSort.ods");
		// Lo.waitEnter();

	}
}