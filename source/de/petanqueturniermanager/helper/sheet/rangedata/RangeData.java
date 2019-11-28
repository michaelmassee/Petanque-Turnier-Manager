/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

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

	public RangePosition getRangePosition(Position start) {
		checkNotNull(start);
		checkArgument(size() > 0);
		Position endPos = Position.from(start);
		return RangePosition.from(start, endPos.spaltePlus(getAnzSpalten() - 1).zeilePlus(size() - 1));
	}

	private int getAnzSpalten() {
		int maxSize = 0;
		for (RowData rowdata : this) {
			maxSize = (maxSize < rowdata.size()) ? rowdata.size() : maxSize;
		}
		return maxSize;
	}

	/**
	 * @return
	 */
	public Object[][] toDataArray() {
		int maxSize = getAnzSpalten();
		Object[][] dataArray = new Object[size()][maxSize];

		int idx = 0;
		for (RowData rowdata : this) {
			dataArray[idx++] = rowdata.toDataArray(maxSize);
		}

		return dataArray;
	}

}
