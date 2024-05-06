/**
 * Erstellung 24.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.rangedata;

import org.apache.commons.lang3.math.NumberUtils;

import com.google.common.base.MoreObjects;

/**
 * @author Michael Massee
 *
 */
public class CellData {

	private Object data;

	public CellData(Object data) {
		this.data = data;
	}

	/**
	 * @return the data
	 */
	public final Object getData() {
		return data;
	}

	public void setVal(Object val) {
		data = val;
	}

	public void setIntVal(int intval) {
		data = intval;
	}

	public void setStringVal(String val) {
		data = val;
	}

	/**
	 * wenn data = null or not a number then return defaultval
	 * 
	 * @return
	 */
	public int getIntVal() {
		return getIntVal(-1);
	}

	/**
	 * @param defaultval wenn data = null or not a number then return defaultval
	 */
	public int getIntVal(int defaultval) {
		if (data != null) {
			if (data instanceof String string && NumberUtils.isParsable(string)) {
				return NumberUtils.toInt(string);
			} else if (data instanceof Number) {
				return (int) ((Number) data).doubleValue();
			}
		}
		return defaultval;
	}

	/**
	 * when data of type int then return String
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
