/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.petanqueturniermanager.helper.position.AbstractPosition;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
@SuppressWarnings("serial")
public class RangeData extends ArrayList<RowData> {

	public RangeData() {
		super();
	}

	public RangeData(int anzZeilen, Integer... intSpalteVals) {
		while (anzZeilen-- > 0) {
			RowData dta = new RowData(intSpalteVals);
			add(dta);
		}
	}

	public RangeData(Object[][] data) {
		super(Arrays.stream(checkNotNull(data)).map(RowData::new).toList());
	}

	public RangeData(List<?> data) {
		super(checkNotNull(data).stream().map(RowData::new).toList());
	}

	public void addData(Object[][] data) {
		addAll(Arrays.stream(checkNotNull(data)).map(RowData::new).toList());
	}

	public void addNewSpalte(String val) {
		for (RowData rowdata : this) {
			rowdata.add(new CellData(val));
		}
	}

	public void addNewSpalte(Integer val) {
		for (RowData rowdata : this) {
			rowdata.add(new CellData(val));
		}
	}

	public void addNewEmptySpalte() {
		for (RowData rowdata : this) {
			rowdata.add(new CellData(""));
		}
	}

	/**
	 * neue Zeile hinzufuegen
	 * 
	 * @return
	 */
	public RowData addNewRow() {
		RowData newRowData = new RowData();
		this.add(newRowData);
		return newRowData;
	}

	public RangePosition getRangePosition(AbstractPosition<?> start) {
		checkNotNull(start);
		checkArgument(size() > 0);
		Position endPos = Position.from(start);
		return RangePosition.from(start, endPos.spaltePlus(getAnzSpalten() - 1).zeilePlus(size() - 1));
	}

	public final int getAnzSpalten() {
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
