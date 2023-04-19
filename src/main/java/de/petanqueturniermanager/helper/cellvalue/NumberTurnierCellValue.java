/**
 * Erstellung : 27.03.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.math.NumberUtils;

import de.petanqueturniermanager.helper.position.Position;

/**
 * @TODO Remove, wird noch verwendet ?
 */
public class NumberTurnierCellValue extends AbstractCellValue<NumberTurnierCellValue, Double>
		implements ICellValue<Double> {

	protected NumberTurnierCellValue() {
	}

	private NumberTurnierCellValue(Position pos, int value) {
		this(pos, (double) value);
	}

	private NumberTurnierCellValue(Position pos, double value) {
		super(pos);
		setValue(value);
	}

	@Override
	protected NumberTurnierCellValue copyCommonAttr(ICellValue<?> abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		return this;
	}

	public static NumberTurnierCellValue from(StringTurnierCellValue cellVal) {
		NumberTurnierCellValue numberCellValue = new NumberTurnierCellValue();
		numberCellValue.copyCommonAttr(cellVal).setValue(NumberUtils.toDouble(cellVal.getValue(), -1));
		return numberCellValue;
	}

	public static final NumberTurnierCellValue from(NumberTurnierCellValue cellVal) {
		return new NumberTurnierCellValue().copyAttr(cellVal);
	}

	public static final NumberTurnierCellValue from(Position pos) {
		return new NumberTurnierCellValue(Position.from(pos), 0);
	}

	public static final NumberTurnierCellValue from(Position pos, double value) {
		return new NumberTurnierCellValue(Position.from(pos), value);
	}

	public static final NumberTurnierCellValue from(int spalte, int zeile) {
		return NumberTurnierCellValue.from(Position.from(spalte, zeile), 0);
	}

	public static final NumberTurnierCellValue from(int spalte, int zeile, double value) {
		return NumberTurnierCellValue.from(Position.from(spalte, zeile), value);
	}

}
