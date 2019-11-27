/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Michael Massee
 *
 */
public class RangeData extends ArrayList<RowData> {

	public RangeData() {
		super();
	}

	public RangeData(Object[][] data) {
		super(Arrays.stream(checkNotNull(data)).map(rowdata -> {
			return new RowData(rowdata);
		}).collect(Collectors.toList()));
	}

	public RowData newRow() {
		RowData newRowData = new RowData();
		this.add(newRowData);
		return newRowData;
	}

	/**
	 * @return
	 */
	public Object[][] toDataArray() {

		int maxSize = 0;
		for (RowData rowdata : this) {
			maxSize = (maxSize < rowdata.size()) ? rowdata.size() : maxSize;
		}
		Object[][] dataArray = new Object[size()][maxSize];

		int idx = 0;
		for (RowData rowdata : this) {
			dataArray[idx++] = rowdata.toDataArray(maxSize);
		}

		return dataArray;
	}

}
