/**
 * Erstellung : 27.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.math.NumberUtils;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

public class NumberCellValue extends AbstractCellValueWithSheet<NumberCellValue, Double> {

	private NumberCellValue(XSpreadsheet sheet, Position pos, double value) {
		super(sheet, pos);
		setValue(value);
	}

	public NumberCellValue(XSpreadsheet sheet) {
		super(sheet);
	}

	@Override
	protected NumberCellValue copyCommonAttr(ICellValueWithSheet<?> abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		return this;
	}

	public static NumberCellValue from(StringCellValue cellVal) {
		NumberCellValue numberCellValue = new NumberCellValue(cellVal.getSheet());
		numberCellValue.copyCommonAttr(cellVal).setValue(NumberUtils.toDouble(cellVal.getValue(), -1));
		return numberCellValue;
	}

	public static final NumberCellValue from(XSpreadsheet sheet) {
		return NumberCellValue.from(sheet, Position.from(0, 0));
	}

	public static final NumberCellValue from(NumberCellValue cellVal) {
		return new NumberCellValue(cellVal.getSheet()).copyCommonAttr(cellVal).setValue(cellVal.getValue());
	}

	public static final NumberCellValue from(XSpreadsheet sheet, Position pos) {
		return NumberCellValue.from(sheet, Position.from(pos), 0);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, Position pos, double value) {
		return new NumberCellValue(sheet, Position.from(pos), value);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, int spalte, int zeile) {
		return NumberCellValue.from(sheet, Position.from(spalte, zeile), 0);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, int spalte, int zeile, Integer value) {
		return NumberCellValue.from(sheet, Position.from(spalte, zeile)).setValue(value);
	}

	public static final NumberCellValue from(XSpreadsheet sheet, int spalte, int zeile, double value) {
		return NumberCellValue.from(sheet, Position.from(spalte, zeile), value);
	}

	public final NumberCellValue setValue(Integer value) {
		super.setValue((double) value);
		return this;
	}

}
