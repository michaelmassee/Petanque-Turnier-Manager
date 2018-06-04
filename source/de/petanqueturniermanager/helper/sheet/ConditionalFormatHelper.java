/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.ConditionOperator;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellstyle.AbstractCellStyleDef;
import de.petanqueturniermanager.helper.cellstyle.CellStyleHelper;
import de.petanqueturniermanager.helper.cellstyle.FehlerStyle;
import de.petanqueturniermanager.helper.position.RangePosition;

public class ConditionalFormatHelper {

	private static final String FORMULA_ISEVEN_ROW = "ISEVEN(ROW())";
	private static final String FORMULA_ISODD_ROW = "ISODD(ROW())";
	public static final String FORMULA_CURRENT_CELL = "INDIRECT(ADDRESS(ROW();COLUMN()))";

	private final ISheet sheet;
	private final RangePosition rangePos;
	private boolean doClear = false;
	private ConditionOperator conditionOperator;
	private String formula1;
	private String formula2;
	private String styleName;

	private ConditionalFormatHelper(ISheet sheet, RangePosition rangePos) {
		checkNotNull(sheet);
		checkNotNull(rangePos);
		this.sheet = sheet;
		this.rangePos = rangePos;
	}

	public static ConditionalFormatHelper from(ISheet sheet, RangePosition rangePos) {
		return new ConditionalFormatHelper(sheet, rangePos);
	}

	public ConditionalFormatHelper append() {
		this.doClear = false;
		return this;
	}

	public ConditionalFormatHelper clear() {
		this.doClear = true;
		return this;
	}

	public ConditionalFormatHelper operator(ConditionOperator conditionOperator) {
		this.conditionOperator = conditionOperator;
		return this;
	}

	public ConditionalFormatHelper formulaIsText() {
		this.formula1 = "ISTEXT(" + ConditionalFormatHelper.FORMULA_CURRENT_CELL + ")";
		this.conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper formulaIsEvenRow() {
		this.formula1 = FORMULA_ISEVEN_ROW;
		this.conditionOperator = ConditionOperator.FORMULA;
		return this;
	}

	public ConditionalFormatHelper formulaIsOddRow() {
		this.formula1 = FORMULA_ISODD_ROW;
		this.conditionOperator = ConditionOperator.FORMULA;
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

	public ConditionalFormatHelper styleIsFehler() throws GenerateException {
		FehlerStyle fehlerStyle = new FehlerStyle();
		CellStyleHelper.from(this.sheet, fehlerStyle).apply();
		this.styleName = fehlerStyle.getName();
		return this;
	}

	public ConditionalFormatHelper style(AbstractCellStyleDef cellStyleDef) throws GenerateException {
		// add/update Style
		CellStyleHelper.from(this.sheet, cellStyleDef).apply();
		this.styleName = cellStyleDef.getName();
		return this;
	}

	/**
	 * Formatierung anwenden und properties auf default
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public ConditionalFormatHelper applyNew() throws GenerateException {
		apply();
		this.doClear = false;
		this.conditionOperator = null;
		this.formula1 = null;
		this.formula2 = null;
		this.styleName = null;
		return this;
	}

	/**
	 * Formatierung anwenden
	 * 
	 * @return
	 * @throws GenerateException
	 */

	public ConditionalFormatHelper apply() throws GenerateException {
		checkNotNull(this.conditionOperator);
		checkNotNull(this.formula1);
		checkNotNull(this.styleName);

		XCellRange xCellRange = this.sheet.getSheetHelper().getCellRange(this.sheet.getSheet(), this.rangePos);

		XPropertySet xPropSet = UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xCellRange);
		com.sun.star.sheet.XSheetConditionalEntries xEntries;
		try {
			xEntries = UnoRuntime.queryInterface(com.sun.star.sheet.XSheetConditionalEntries.class, xPropSet.getPropertyValue("ConditionalFormat"));
			if (this.doClear) {
				xEntries.clear(); // clears all condition entries.
			}

			// create a condition and apply it to the range
			// https://www.openoffice.org/api/docs/common/ref/com/sun/star/sheet/XSheetConditionalEntries.html
			PropertyValue[] aCondition;
			if (this.formula2 != null) {
				aCondition = new PropertyValue[4];
			} else {
				aCondition = new PropertyValue[3];
			}
			int idx = 0;
			aCondition[idx] = new com.sun.star.beans.PropertyValue();
			aCondition[idx].Name = "Operator";
			aCondition[idx].Value = this.conditionOperator;

			// string Formula1 contains the value or formula for the operation.
			idx++;
			aCondition[idx] = new com.sun.star.beans.PropertyValue();
			aCondition[idx].Name = "Formula1";
			aCondition[idx].Value = this.formula1;

			// string Formula2 contains the second value or formula for the operation (used with ConditionOperator::BETWEEN or ConditionOperator::NOT_BETWEEN operations).
			if (this.formula2 != null) {
				idx++;
				aCondition[idx] = new com.sun.star.beans.PropertyValue();
				aCondition[idx].Name = "Formula2";
				aCondition[idx].Value = this.formula2;
			}

			idx++;
			aCondition[idx] = new com.sun.star.beans.PropertyValue();
			aCondition[idx].Name = "StyleName";
			aCondition[idx].Value = this.styleName;
			xEntries.addNew(aCondition);
			xPropSet.setPropertyValue("ConditionalFormat", xEntries);
		} catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException | PropertyVetoException e) {
			this.sheet.getLogger().error(e.getMessage(), e);
		}
		return this;
	}

}
