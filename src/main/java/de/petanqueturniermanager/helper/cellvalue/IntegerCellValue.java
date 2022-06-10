/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellvalue;

import org.apache.commons.lang3.math.NumberUtils;

public class IntegerCellValue extends NumberCellValue {

	public static final IntegerCellValue from(StringCellValue cellVal) {
		IntegerCellValue numberCellValue = new IntegerCellValue();
		numberCellValue.copyCommonAttr(cellVal).setValue(NumberUtils.toDouble(cellVal.getValue(), -1));
		return numberCellValue;
	}

}
