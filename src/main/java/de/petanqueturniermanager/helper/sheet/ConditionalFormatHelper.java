/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.AbstractCellStyleDef;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.position.RangePosition;

public class ConditionalFormatHelper extends BaseHelper {
	private static final Logger logger = LogManager.getLogger(ConditionalFormatHelper.class);

	public static final String FORMULA_ISEVEN_ROW = "ISEVEN(ROW())";
	public static final String FORMULA_ISODD_ROW = "ISODD(ROW())";
	public static final String FORMULA_CURRENT_CELL = "INDIRECT(ADDRESS(ROW();COLUMN()))";

	public static String FORMULAISEVENANDEQUALTOINT_STR(int val) {
		return "AND(" + FORMULA_ISEVEN_ROW + ";" + FORMULA_CURRENT_CELL + "=" + val + ")";
	}

	public static String FORMULAISODDANDEQUALTOINT_STR(int val) {
		return "AND(" + FORMULA_ISODD_ROW + ";" + FORMULA_CURRENT_CELL + "=" + val + ")";
	}

	private final RangePosition rangePos;
	private boolean doClear = false;
	private ConditionOperator conditionOperator;
	private String formula1;
	private String formula2;
	private String styleName;

	private ConditionalFormatHelper(ISheet sheet, RangePosition rangePos) {
		super(sheet);
		this.rangePos = checkNotNull(rangePos);
	}

	public static ConditionalFormatHelper from(ISheet sheet, RangePosition rangePos) {
		return new ConditionalFormatHelper(sheet, rangePos);
	}

	public ConditionalFormatHelper append() {
		doClear = false;
		return this;
	}

	public ConditionalFormatHelper clear() {
		doClear = true;
		return this;
	}

	public ConditionalFormatHelper operator(ConditionOperator conditionOperator) {
		this.conditionOperator = conditionOperator;
		return this;
	}

	public ConditionalFormatHelper formulaIsText() {
		formula1 = "ISTEXT(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")";
		conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper formulaIsEvenAndEqualToInt(int val) {
		formula1 = FORMULAISEVENANDEQUALTOINT_STR(val);
		conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper formulaIsOddAndEqualToInt(int val) {
		formula1 = FORMULAISODDANDEQUALTOINT_STR(val);
		conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper operatorIsEqualTo(String val) {
		formula1 = val;
		conditionOperator = ConditionOperator.EQUAL;
		return this;
	}

	public ConditionalFormatHelper formulaIsEvenRow() {
		formula1 = FORMULA_ISEVEN_ROW;
		conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper formulaIsOddRow() {
		formula1 = FORMULA_ISODD_ROW;
		conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	/**
	 * set ConditionOperator nach FORMULA
	 */

	public ConditionalFormatHelper isFormula() {
		conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper formula1(String formula1) {
		this.formula1 = formula1;
		return this;
	}

	public ConditionalFormatHelper formula2(String formula2) {
		this.formula2 = formula2;
		return this;
	}

	public ConditionalFormatHelper style(String styleName) {
		this.styleName = styleName;
		return this;
	}

	public ConditionalFormatHelper styleIsFehler() {
		FehlerStyle fehlerStyle = new FehlerStyle();
		CellStyleHelper.from(getISheet(), fehlerStyle).apply();
		styleName = fehlerStyle.getName();
		return this;
	}

	public ConditionalFormatHelper style(AbstractCellStyleDef cellStyleDef) {
		// add/update Style
		CellStyleHelper.from(getISheet(), cellStyleDef).apply();
		styleName = cellStyleDef.getName();
		return this;
	}

	/**
	 * properties auf default
	 *
	 * @return
	 * @throws GenerateException
	 */

	public ConditionalFormatHelper reset() throws GenerateException {
		doClear = false;
		conditionOperator = null;
		formula1 = null;
		formula2 = null;
		styleName = null;
		return this;
	}

	/**
	 * Formatierung anwenden<br>
	 * Fehler wenn irgend ein Sheet hat ein Lock
	 *
	 * @return
	 * @throws GenerateException
	 */

	public ConditionalFormatHelper applyAndDoReset() throws GenerateException {
		checkNotNull(conditionOperator);
		checkNotNull(formula1);
		checkNotNull(styleName);

		XPropertySet xPropSet = RangeHelper.from(getISheet(), rangePos).getPropertySet();
		com.sun.star.sheet.XSheetConditionalEntries xEntries;
		try {
			xEntries = UnoRuntime.queryInterface(com.sun.star.sheet.XSheetConditionalEntries.class,
					xPropSet.getPropertyValue("ConditionalFormat"));
			if (doClear) {
				xEntries.clear(); // clears all condition entries.
			}

			// create a condition and apply it to the range
			// https://www.openoffice.org/api/docs/common/ref/com/sun/star/sheet/XSheetConditionalEntries.html
			PropertyValue[] aCondition;
			if (formula2 != null) {
				aCondition = new PropertyValue[4];
			} else {
				aCondition = new PropertyValue[3];
			}
			int idx = 0;
			aCondition[idx] = new com.sun.star.beans.PropertyValue();
			aCondition[idx].Name = "Operator";
			aCondition[idx].Value = conditionOperator;

			// string Formula1 contains the value or formula for the operation.
			idx++;
			aCondition[idx] = new com.sun.star.beans.PropertyValue();
			aCondition[idx].Name = "Formula1";
			aCondition[idx].Value = formula1;

			// string Formula2 contains the second value or formula for the operation (used with ConditionOperator::BETWEEN or ConditionOperator::NOT_BETWEEN operations).
			if (formula2 != null) {
				idx++;
				aCondition[idx] = new com.sun.star.beans.PropertyValue();
				aCondition[idx].Name = "Formula2";
				aCondition[idx].Value = formula2;
			}

			idx++;
			aCondition[idx] = new com.sun.star.beans.PropertyValue();
			aCondition[idx].Name = "StyleName";
			aCondition[idx].Value = styleName;
			xEntries.addNew(aCondition);
			xPropSet.setPropertyValue("ConditionalFormat", xEntries);
		} catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
				| PropertyVetoException e) {
			logger.error(e.getMessage(), e);
		}
		reset();
		return this;
	}

}
