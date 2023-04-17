/**
 * Erstellung 21.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellvalue;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.helper.position.Position;

/**
 * @author Michael Massee
 *
 */
@SuppressWarnings("rawtypes")
public abstract class AbstractCellValueWithSheet<T extends AbstractCellValueWithSheet, V>
		extends AbstractCellValue<T, V> implements ICellValueWithSheet<V> {

	private XSpreadsheet sheet;

	protected AbstractCellValueWithSheet(XSpreadsheet sheet) {
		this(sheet, Position.from(0, 0));
	}

	protected AbstractCellValueWithSheet(XSpreadsheet sheet, Position pos) {
		super(pos);
		checkNotNull(sheet, "sheet=null");
		this.setSheet(sheet);
	}

	protected AbstractCellValueWithSheet copyCommonAttr(ICellValueWithSheet<?> abstractCellValue) {
		super.copyCommonAttr(abstractCellValue);
		this.setSheet(abstractCellValue.getSheet());
		return this;
	}

	@Override
	public XSpreadsheet getSheet() {
		return this.sheet;
	}

	@SuppressWarnings("unchecked")
	public T setSheet(XSpreadsheet sheet) {
		this.sheet = checkNotNull(sheet);
		return (T) this;
	}

}
