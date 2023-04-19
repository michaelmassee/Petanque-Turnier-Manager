/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * @author Michael Massee
 *
 */
@SuppressWarnings("serial")
public class RowData extends ArrayList<CellData> {

	public RowData() {
		super();
	}

	public RowData(Integer... intVals) {
		super();
		Arrays.stream(intVals).forEach(this::newInt);
	}

	// ein dimension
	public RowData(Object data) {
		super(Arrays.asList(getRowdataArray(data)));
	}

	private static CellData[] getRowdataArray(Object data) {
		return new CellData[] { new CellData(data) };
	}

	public RowData(Object[] data) {
		super(Arrays.stream(checkNotNull(data)).map(CellData::new).toList());
	}

	public CellData getLast() {
		return get(size() - 1);
	}

	public CellData newString(String val) {
		CellData cellData = new CellData(val);
		this.add(cellData);
		return cellData;
	}

	public CellData newInt(int val) {
		CellData cellData = new CellData(Integer.valueOf(val));
		this.add(cellData);
		return cellData;
	}

	/**
	 *
	 */
	public CellData newEmpty() {
		CellData cellData = new CellData("");
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
			dataArray[idx++] = (cellData.getData() != null) ? cellData.getData() : "";
		}
		// array auffuellen
		while (idx < arraySize) {
			dataArray[idx++] = "";
		}

		return dataArray;
	}

}
