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
public class RowData extends ArrayList<CellData> {

	public RowData(Object[] data) {
		super(Arrays.stream(checkNotNull(data)).map(rowdata -> {
			return new CellData(rowdata);
		}).collect(Collectors.toList()));
	}

	public CellData getLast() {
		return get(size() - 1);
	}

}
