/**
* Erstellung : 27.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.math.NumberUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

public class NumberCellValue extends AbstractCellValueWithSheet<NumberCellValue, Double> {

	protected NumberCellValue() {
	}

	private NumberCellValue(XSpreadsheet sheet, Position pos, int value) {
		this(sheet, pos, (double) value);
	}

	private NumberCellValue(XSpreadsheet sheet, Position pos, double value) {
		super(sheet, pos);
		setValue(value);
	}

	@Override
	protected NumberCellValue copyCommonAttr(ICellValueWithSheet<?> abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		return this;
	}

	public static NumberCellValue from(StringCellValue cellVal) {
		NumberCellValue numberCellValue = new NumberCellValue();
		numberCellValue.copyCommonAttr(cellVal).setValue(NumberUtils.toDouble(cellVal.getValue(), -1));
		return numberCellValue;
	}

	public static final NumberCellValue from(NumberCellValue cellVal) {
		return new NumberCellValue().copyCommonAttr(cellVal).setValue(cellVal.getValue());
	}

	public static final NumberCellValue from(XSpreadsheet sheet, Position pos) {
		return new NumberCellValue(sheet, Position.from(pos), 0);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, Position pos, double value) {
		return new NumberCellValue(sheet, Position.from(pos), value);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, int spalte, int zeile) {
		return NumberCellValue.from(sheet, Position.from(spalte, zeile), 0);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, int spalte, int zeile, double value) {
		return NumberCellValue.from(sheet, Position.from(spalte, zeile), value);
	}

	public NumberCellValue setValue(Integer value) {
		super.setValue((double) value);
		return this;
	}

}
