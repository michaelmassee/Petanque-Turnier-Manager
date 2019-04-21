/**
 * Erstellung 21.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.cellvalue;

import com.sun.star.sheet.XSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public interface ICellValueWithSheet<V> extends ICellValue<V> {

	XSpreadsheet getSheet();
}
