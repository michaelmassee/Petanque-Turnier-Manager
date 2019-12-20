/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import com.google.common.base.MoreObjects;

/**
 * @author Michael Massee
 *
 */
public class CellData {

	private final Object data;

	public CellData(Object data) {
		this.data = data;
	}

	/**
	 * @return the data
	 */
	public final Object getData() {
		return data;
	}

	/**
	 * @param i
	 */
	public int getIntVal(int defaultval) {
		if (data != null && data instanceof Number) {
			return (int) ((Number) data).doubleValue();
		}
		return defaultval;
	}

	/**
	 */
	public String getStringVal() {
		if (data != null) {
			if (data instanceof String) {
				return (String) data;
			} else if (data instanceof Number) {
				return "" + getIntVal(-1);
			}
		}
		return null;
	}

	@Override
	public String toString() {

		// @formatter:off
		return MoreObjects.toStringHelper(this).
				add("Value", getStringVal()).
				toString();
		// @formatter:on

	}

}
