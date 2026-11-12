/*
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
	 * @return Ganzzahl-Wert; -1 wenn data null oder keine Zahl ist
	 */
	public int getIntVal() {
		return getIntVal(-1);
	}

	/**
	 * @param defaultval Rückgabe, wenn data null oder keine Zahl ist
	 * @return Ganzzahl-Wert (Nachkommastellen werden abgeschnitten)
	 */
	public int getIntVal(int defaultval) {
		if (data instanceof String string && NumberUtils.isParsable(string)) {
			return NumberUtils.toInt(string);
		}
		if (data instanceof Number number) {
			return (int) number.doubleValue();
		}
		return defaultval;
	}

	/**
	 * @param defaultval Rückgabe, wenn data null oder keine Zahl ist
	 * @return Zahlenwert inklusive Nachkommastellen
	 */
	public double getDoubleVal(double defaultval) {
		if (data instanceof String string && NumberUtils.isParsable(string)) {
			return NumberUtils.toDouble(string);
		}
		if (data instanceof Number number) {
			return number.doubleValue();
		}
		return defaultval;
	}

	/**
	 * @return Zeichenkette; Zahlen als Ganzzahl-Text, null wenn data null ist
	 */
	public String getStringVal() {
		if (data instanceof String string) {
			return string;
		}
		if (data instanceof Number) {
			return String.valueOf(getIntVal(-1));
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
