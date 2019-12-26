/**
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.numberformat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.Locale;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.MalformedNumberFormatException;
import com.sun.star.util.XNumberFormats;
import com.sun.star.util.XNumberFormatsSupplier;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.sheet.BaseHelper;

/**
 * @author Michael Massee
 *
 */
public class NumberFormatHelper extends BaseHelper implements ICommonProperties {

	private static final Logger logger = LogManager.getLogger(NumberFormatHelper.class);
	private final Locale locale = new Locale();

	private NumberFormatHelper(ISheet iSheet) {
		super(iSheet);
	}

	public static NumberFormatHelper from(ISheet sheet) {
		return new NumberFormatHelper(sheet);
	}

	public int getIdx(UserNumberFormat userNumberFormat) {
		int index = -1;

		XNumberFormatsSupplier xNumberFormatsSupplier = UnoRuntime.queryInterface(XNumberFormatsSupplier.class, getWorkingSpreadsheetDocument());
		XNumberFormats xNumberFormats = xNumberFormatsSupplier.getNumberFormats();
		try {
			index = xNumberFormats.queryKey(userNumberFormat.getPattern(), locale, false);
			if (index == -1) {
				index = xNumberFormats.addNew(userNumberFormat.getPattern(), locale);
			}
		} catch (MalformedNumberFormatException e) {
			logger.error(e);
		}
		return index;
	}

	// public void setformat() {
	// XNumberFormatsSupplier xNumberFormatsSupplier = UnoRuntime.queryInterface(XNumberFormatsSupplier.class,
	// wkRefISheet.get().getWorkingSpreadsheet().getWorkingSpreadsheetDocument());
	// XNumberFormats xNumberFormats = xNumberFormatsSupplier.getNumberFormats();
	//
	// String numberFormat = "TTT";
	//
	// try {
	// int index = xNumberFormats.queryKey(numberFormat, locale, false);
	// if (index == -1) {
	// index = xNumberFormats.addNew(numberFormat, locale);
	// }
	// xPropertyHelper.setProperty(NUMBERFORMAT, Integer.valueOf(index));
	// } catch (MalformedNumberFormatException e) {
	// logger.error(e);
	// }
	// }

}
