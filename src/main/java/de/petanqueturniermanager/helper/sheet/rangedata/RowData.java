/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * @author Michael Massee
 *
 */
public class RowData extends ArrayList<CellData> {

	public RowData() {
		super();
	}

	public RowData(Object[] data) {
		super(Arrays.stream(checkNotNull(data)).map(rowdata -> {
			return new CellData(rowdata);
		}).collect(Collectors.toList()));
	}

	public CellData getLast() {
		return get(size() - 1);
	}

	public CellData newString(String val) {
		CellData cellData = new CellData(new String(val));
		this.add(cellData);
		return cellData;
	}

	public CellData newInt(int val) {
		CellData cellData = new CellData(new Integer(val));
		this.add(cellData);
		return cellData;
	}

	/**
	 *
	 */
	public CellData newEmpty() {
		CellData cellData = new CellData(new String());
		this.add(cellData);
		return cellData;
	}

	/**
	 * @return
	 */
	public Object[] toDataArray(int arraySize) {
		checkArgument(size() <= arraySize);
		Object[] dataArray = new Object[arraySize];

		int idx = 0;
		for (CellData cellData : this) {
			dataArray[idx++] = (cellData.getData() != null) ? cellData.getData() : new String();
		}
		// array auffuellen
		while (idx < arraySize) {
			dataArray[idx++] = new String();
		}

		return dataArray;
	}

}
