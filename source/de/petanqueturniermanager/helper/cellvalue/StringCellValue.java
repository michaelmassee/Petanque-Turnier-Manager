/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

public class StringCellValue extends AbstractCellValue<StringCellValue, String> {

	private StringCellValue() {
		super();
	}

	private StringCellValue(XSpreadsheet sheet, Position pos, int value) {
		this(sheet, pos, "" + value);
	}

	private StringCellValue(XSpreadsheet sheet, Position pos, String value) {
		super(sheet, pos);
		setValue(value);
	}

	@Override
	protected StringCellValue copyCommonAttr(@SuppressWarnings("rawtypes") AbstractCellValue abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		return this;
	}

	public static final StringCellValue from(NumberCellValue cellVal) {
		StringCellValue stringCellValue = new StringCellValue();
		stringCellValue.copyCommonAttr(cellVal).setValue(cellVal.getValue().toString());
		return stringCellValue;
	}

	public static final StringCellValue from(StringCellValue cellVal) {
		return new StringCellValue().copyAttr(cellVal);
	}

	public static final StringCellValue from(XSpreadsheet sheet) {
		return new StringCellValue(sheet, Position.from(0, 0), "");
	}

	public static final StringCellValue from(XSpreadsheet sheet, Position pos) {
		return new StringCellValue(sheet, Position.from(pos), "");
	}

	public static final StringCellValue from(XSpreadsheet sheet, Position pos, String value) {
		return new StringCellValue(sheet, Position.from(pos), value);
	}

	public static final StringCellValue from(XSpreadsheet sheet, int spalte, int zeile) {
		return new StringCellValue(sheet, Position.from(spalte, zeile), "");
	}

	public static final StringCellValue from(XSpreadsheet sheet, int spalte, int zeile, String value) {
		return new StringCellValue(sheet, Position.from(spalte, zeile), value);
	}

	/**
	 * StringUtils.isEmpty(null) = true <br>
	 * StringUtils.isEmpty("") = true<br>
	 * StringUtils.isEmpty(" ") = false<br>
	 *
	 * @return
	 */

	public boolean isValueEmpty() {
		return StringUtils.isEmpty(getValue());
	}

	public StringCellValue appendValue(String string) {
		if (StringUtils.isNotEmpty(string)) {
			if (getValue() == null) {
				setValue(string);
			} else {
				setValue(getValue() + string);
			}
		}
		return this;
	}
}
