/**
 * Erstellung : 21.04.2019 / Michael Massee
 * 
 **/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.StringUtils;

import de.petanqueturniermanager.helper.position.Position;

/**
 * neue Version von StringCellValue
 * 
 * @TODO Remove, wird noch verwendet ?
 */

public class StringTurnierCellValue extends AbstractCellValue<StringTurnierCellValue, String>
		implements ICellValue<String> {

	protected StringTurnierCellValue() {
		super();
	}

	private StringTurnierCellValue(Position pos, String value) {
		super(pos);
		setValue(value);
	}

	@Override
	protected StringTurnierCellValue copyCommonAttr(ICellValue<?> abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		return this;
	}

	public static final StringTurnierCellValue from(ICellValue<?> cellVal) {
		StringTurnierCellValue stringCellValue = new StringTurnierCellValue();
		stringCellValue.copyCommonAttr(cellVal).setValue(cellVal.getValue().toString());
		return stringCellValue;
	}

	public static final StringTurnierCellValue from(Position pos, String value) {
		return new StringTurnierCellValue(Position.from(pos), value);
	}

	public static final StringTurnierCellValue from() {
		return from(Position.from(0, 0), "");
	}

	public static final StringTurnierCellValue from(Position pos) {
		return from(Position.from(pos), "");
	}

	public static final StringTurnierCellValue from(int spalte, int zeile) {
		return from(Position.from(spalte, zeile), "");
	}

	public static final StringTurnierCellValue from(int spalte, int zeile, String value) {
		return from(Position.from(spalte, zeile), value);
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

	public StringTurnierCellValue appendValue(String string) {
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
