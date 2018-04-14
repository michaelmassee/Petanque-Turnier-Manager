/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.math.NumberUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

public class NumberCellValue extends AbstractCellValue<NumberCellValue, Double> {

	public NumberCellValue() {
		super();
	}

	public NumberCellValue(XSpreadsheet sheet, Position pos, int value) {
		this(sheet, pos, (double) value);
	}

	public NumberCellValue(XSpreadsheet sheet, Position pos, double value) {
		super(sheet, pos);
		this.setValue(value);
	}

	@Override
	protected NumberCellValue copyCommonAttr(@SuppressWarnings("rawtypes") AbstractCellValue abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		return this;
	}

	public static NumberCellValue from(StringCellValue cellVal) {
		NumberCellValue numberCellValue = new NumberCellValue();
		numberCellValue.copyCommonAttr(cellVal).setValue(NumberUtils.toDouble(cellVal.getValue(), -1));
		return numberCellValue;
	}

	public static final NumberCellValue from(NumberCellValue cellVal) {
		return new NumberCellValue().copyAttr(cellVal);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, Position pos) {
		return new NumberCellValue(sheet, Position.from(pos), 0);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, Position pos, double value) {
		return new NumberCellValue(sheet, Position.from(pos), value);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, int spalte, int zeile, double value) {
		return new NumberCellValue(sheet, Position.from(spalte, zeile), value);
	}

}
