/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.sun.star.beans.PropertyValue;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class SortHelper {

	// private static final Logger logger = LogManager.getLogger(SortHelper.class);
	private final RangePosition rangePositionToSort;

	// private int sortSpalte = 0; // 0 = erste spalte
	private boolean aufSteigendSortieren = true;
	private boolean caseSensitive = false;
	private boolean bindFormatsToContent = false;
	private int[] sortSpalten = new int[] { 0 }; // default erste spalte
	private final XSpreadsheet xSpreadsheet;
	private final XSpreadsheetDocument workingSpreadsheetDocument;

	private SortHelper(ISheet iSheet, RangePosition rangePosition) throws GenerateException {
		this(iSheet.getXSpreadSheet(), iSheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument(), rangePosition);
	}

	private SortHelper(XSpreadsheet xSpreadsheet, XSpreadsheetDocument xSpreadsheetDocument,
			RangePosition rangePosition) throws GenerateException {
		rangePositionToSort = checkNotNull(rangePosition);
		this.xSpreadsheet = checkNotNull(xSpreadsheet);
		workingSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
	}

	public static SortHelper from(ISheet iSheet, RangePosition rangePosition) throws GenerateException {
		return new SortHelper(iSheet, rangePosition);
	}

	public static SortHelper from(XSpreadsheet xSpreadsheet, XSpreadsheetDocument xSpreadsheetDocument,
			RangePosition rangePosition) throws GenerateException {
		return new SortHelper(xSpreadsheet, xSpreadsheetDocument, rangePosition);
	}

	public static SortHelper from(WeakRefHelper<ISheet> sheetWkRef, RangePosition rangePosition)
			throws GenerateException {
		return new SortHelper(checkNotNull(sheetWkRef).get(), rangePosition);
	}

	/**
	 * default = 0 = erste Spalte
	 *
	 * @param sortSpalte
	 * @return
	 */
	public SortHelper spalteToSort(int sortSpalte) {
		checkArgument(sortSpalte > -1);
		sortSpalten = new int[] { sortSpalte };
		return this;
	}

	/**
	 * default = 0 = erste Spalte
	 *
	 * @param sortSpalte
	 * @return
	 */
	public SortHelper spaltenToSort(int[] sortSpalten) {
		checkArgument(sortSpalten.length > 0);
		this.sortSpalten = sortSpalten;
		return this;
	}

	/**
	 * ist default
	 *
	 * @return
	 */
	public SortHelper aufSteigendSortieren() {
		aufSteigendSortieren = true;
		return this;
	}

	public SortHelper abSteigendSortieren() {
		aufSteigendSortieren = false;
		return this;
	}

	public SortHelper aufSteigendSortieren(boolean ja) {
		aufSteigendSortieren = ja;
		return this;
	}

	/**
	 * Groß- / Kleinschreibung beachten, default = false
	 */
	public SortHelper caseSensitive() {
		caseSensitive = true;
		return this;
	}

	/**
	 * specifies if cell formats are moved with the contents they belong to.<br>
	 * Formatierung mit sortieren ? <br>
	 * default = false
	 *
	 * @return
	 */

	public SortHelper bindFormatsToContent() {
		bindFormatsToContent = true;
		return this;
	}

	/**
	 * Nicht Groß- / Kleinschreibung beachten, default = true
	 */
	public SortHelper notCaseSensitive() {
		caseSensitive = false;
		return this;
	}

	public SortHelper doSort() throws GenerateException {
		checkNotNull(sortSpalten);
		checkArgument(sortSpalten.length > 0);

		// in bloecke von max 3 aufteilen, reverse order
		List<List<Integer>> sortBloecke = splitSortBloecke(sortSpalten);
		for (List<Integer> block : sortBloecke) {
			doSortMax3Spalten(block);
		}

		return this;
	}

	/**
	 * das Array sortspalten aufteilen in mehrere bloecke von max 3 in reverse order
	 */
	@VisibleForTesting
	List<List<Integer>> splitSortBloecke(int[] sortSpaltenToSplit) {

		final int chunkSize = 3;
		final AtomicInteger counter = new AtomicInteger();

		Map<Integer, List<Integer>> groups = Arrays.stream(sortSpaltenToSplit).boxed()
				.collect(Collectors.groupingBy(s -> counter.getAndIncrement() / chunkSize));

		List<List<Integer>> bloecke = new ArrayList<>();
		bloecke = groups.values().stream().collect(Collectors.toList());

		Collections.reverse(bloecke);

		return bloecke;
	}

	/**
	 * Sorting records. Sorting arranges the visible cells on the sheet.<br>
	 * Anmrk 1.7.2022 Stimt das mit nur 3 Spalten ??<br>
	 * In Calc, you can sort by up to three criteria, with each criterion applied one after the other..<br>
	 * Bug mit nur 3 ist gefixt ?<br>
	 * https://bugs.documentfoundation.org/show_bug.cgi?id=45747&redirected_from=fdo<br>
	 * 26.03.2023 still no Fix in 7.0.0<br>
	 * https://bugs.documentfoundation.org/show_bug.cgi?id=135242<br>
	 * https://ask.libreoffice.org/t/macro-sort-only-allows-3-criteria/26617/12<br>
	 *
	 * @return
	 * @throws GenerateException
	 */

	private void doSortMax3Spalten(List<Integer> sortMax3Spalten) throws GenerateException {

		checkNotNull(sortMax3Spalten);
		checkArgument(sortMax3Spalten.size() > 0);

		// lo 7 ist das noch so
		checkArgument(sortMax3Spalten.size() < 4); // max 3 spalten

		XCellRange xCellRangeToSort = RangeHelper.from(xSpreadsheet, workingSpreadsheetDocument, rangePositionToSort)
				.getCellRange();

		if (xCellRangeToSort == null) {
			return;
		}

		XSortable xSortable = Lo.qi(XSortable.class, xCellRangeToSort);
		TableSortField[] sortFields = new TableSortField[sortMax3Spalten.size()];

		int idx = 0;
		for (Integer spalteToSort : sortMax3Spalten) {
			TableSortField sortField = new TableSortField();
			sortField.Field = spalteToSort; // 0 = erste spalte
			sortField.IsAscending = aufSteigendSortieren;
			sortField.IsCaseSensitive = caseSensitive;
			sortFields[idx] = sortField;
			idx++;
		}

		PropertyValue[] aSortDesc = new PropertyValue[2];
		PropertyValue propVal = new PropertyValue();
		propVal.Name = "SortFields";
		propVal.Value = sortFields;
		aSortDesc[0] = propVal;

		// specifies if cell formats are moved with the contents they belong to.
		propVal = new PropertyValue();
		propVal.Name = "BindFormatsToContent";
		propVal.Value = bindFormatsToContent;
		aSortDesc[1] = propVal;

		xSortable.sort(aSortDesc);
	}

}
