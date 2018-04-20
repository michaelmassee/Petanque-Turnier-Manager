/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

public class StringCellValue extends AbstractCellValue<StringCellValue, String> {

	public StringCellValue() {
		super();
	}

	public StringCellValue(XSpreadsheet sheet, Position pos, int value) {
		this(sheet, pos, "" + value);
	}

	public StringCellValue(XSpreadsheet sheet, Position pos, String value) {
		super(sheet, pos);
		this.setValue(value);
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
		return new StringCellValue(sheet, Position.from(0, 0), "xx");
	}

	public static final StringCellValue from(XSpreadsheet sheet, Position pos) {
		return new StringCellValue(sheet, Position.from(pos), "xx");
	}

	public static final StringCellValue from(XSpreadsheet sheet, Position pos, String value) {
		return new StringCellValue(sheet, Position.from(pos), value);
	}

	public static final StringCellValue from(XSpreadsheet sheet, int spalte, int zeile, String value) {
		return new StringCellValue(sheet, Position.from(spalte, zeile), value);
	}

}
