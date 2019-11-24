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

	public RangeData(Object[][] data) {
		super(Arrays.stream(checkNotNull(data)).map(rowdata -> {
			return new RowData(rowdata);
		}).collect(Collectors.toList()));
	}

}
