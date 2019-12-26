/**
 * Erstellung 20.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.beans.PropertyValue;
import com.sun.star.table.TableSortField;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XSortable;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class SortHelper extends BaseHelper {

	// private static final Logger logger = LogManager.getLogger(SortHelper.class);
	private final RangePosition rangePositionToSort;

	// private int sortSpalte = 0; // 0 = erste spalte
	private boolean aufSteigendSortieren = true;
	private boolean caseSensitive = false;
	private boolean bindFormatsToContent = false;
	private int[] sortSpalten = new int[] { 0 }; // default erste spalte

	private SortHelper(ISheet iSheet, RangePosition rangePosition) {
		super(iSheet);

		rangePositionToSort = checkNotNull(rangePosition);
	}

	public static SortHelper from(ISheet iSheet, RangePosition rangePosition) {
		return new SortHelper(iSheet, rangePosition);
	}

	public static SortHelper from(WeakRefHelper<ISheet> sheetWkRef, RangePosition rangePosition) {
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

	/**
	 * Sorting records. Sorting arranges the visible cells on the sheet.<br>
	 * In Calc, you can sort by up to three criteria, with each criterion applied one after the other..<br>
	 *
	 * @return
	 * @throws GenerateException
	 */

	public SortHelper doSort() throws GenerateException {
		checkNotNull(sortSpalten);
		checkArgument(sortSpalten.length > 0);
		checkArgument(sortSpalten.length < 4); // max 3 spalten

		XCellRange xCellRangeToSort = RangeHelper.from(getISheet(), rangePositionToSort).getCellRange();
		if (xCellRangeToSort == null) {
			return this;
		}

		XSortable xSortable = UnoRuntime.queryInterface(XSortable.class, xCellRangeToSort);
		TableSortField[] sortFields = new TableSortField[sortSpalten.length];

		for (int sortSpalteIdx = 0; sortSpalteIdx < sortSpalten.length; sortSpalteIdx++) {
			TableSortField sortField = new TableSortField();
			sortField.Field = sortSpalten[sortSpalteIdx]; // 0 = erste spalte
			sortField.IsAscending = aufSteigendSortieren;
			sortField.IsCaseSensitive = caseSensitive;
			sortFields[sortSpalteIdx] = sortField;
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
		return this;
	}

}
