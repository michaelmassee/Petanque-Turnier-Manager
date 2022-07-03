/**
 * Erstellung 26.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet.numberformat;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.Locale;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.MalformedNumberFormatException;
import com.sun.star.util.XNumberFormats;
import com.sun.star.util.XNumberFormatsSupplier;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;

/**
 * @author Michael Massee
 *
 */
public class NumberFormatHelper implements ICommonProperties {

	private static final Logger logger = LogManager.getLogger(NumberFormatHelper.class);
	private final Locale locale = new Locale();
	private final XSpreadsheetDocument xSpreadsheetDocument;

	private NumberFormatHelper(XSpreadsheetDocument xSpreadsheetDocument) {
		this.xSpreadsheetDocument = xSpreadsheetDocument;
	}

	public static NumberFormatHelper from(ISheet sheet) {
		return new NumberFormatHelper(sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument());
	}

	public static NumberFormatHelper from(XSpreadsheetDocument xSpreadsheetDocument) {
		return new NumberFormatHelper(xSpreadsheetDocument);
	}

	public int getIdx(UserNumberFormat userNumberFormat) {
		int index = -1;

		XNumberFormatsSupplier xNumberFormatsSupplier = UnoRuntime.queryInterface(XNumberFormatsSupplier.class,
				xSpreadsheetDocument);
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

}
