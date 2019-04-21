/**
 * Erstellung 14.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XProtectable;

/**
 * @author Michael Massee
 *
 */
public class TurnierSheet {
	private static final Logger logger = LogManager.getLogger(TurnierSheet.class);
	private final XSpreadsheet xSpreadsheet;

	private TurnierSheet(XSpreadsheet xSpreadsheet) {
		this.xSpreadsheet = xSpreadsheet;
	}

	public static TurnierSheet from(XSpreadsheet xSpreadsheet) {
		checkNotNull(xSpreadsheet);
		return new TurnierSheet(xSpreadsheet);
	}

	/**
	 * Keine änderungen am sheet mehr erlaubt
	 * 
	 */
	public TurnierSheet protect() {
		protect(true);
		return this;
	}

	/**
	 * Keine änderungen mehr erlaubt wenn protect = true<br>
	 * <br>
	 * 16.04.2019 <br>
	 * BUG ? lo = 6.2.2 ? Style lassen sich nicht mehr ändern wenn irgendein sheet is protected
	 * 
	 * @param protect true/false
	 */
	public TurnierSheet protect(boolean protect) {
		// XProtectable xProtectable = UnoRuntime.queryInterface(XProtectable.class, xSpreadsheet);
		// if (protect) {
		// xProtectable.protect("");
		// } else {
		// xProtectable.unprotect("");
		// }
		return this;
	}

	public boolean isProtected() {
		XProtectable xProtectable = UnoRuntime.queryInterface(XProtectable.class, xSpreadsheet);
		return xProtectable.isProtected();
	}

	/**
	 * 1. in google nach begriff "color chooser" suchen. -> Color chooser verwenden, hex code ohne #<br>
	 * 2. Color chooser in Zelle verwenden-> hex code kopieren und <br>
	 * setTabColor(XSpreadsheet xSheet, String hex) verwenden <br>
	 * <br>
	 * Property TabColor in Sheet <br>
	 * list of properties <br>
	 * https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1sheet_1_1Spreadsheet.html#details
	 * 
	 * @param color int val. convert from hex z.b. Integer.valueOf(0x003399), Integer.parseInt("003399", 16)
	 */
	public TurnierSheet tabColor(int color) {
		XPropertySet xPropSet = UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, xSpreadsheet);
		try {
			xPropSet.setPropertyValue("TabColor", new Integer(color));
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	/**
	 * 1. in google nach begriff "color chooser" suchen. -> Color chooser verwenden, hex code ohne #<br>
	 * 2. Color chooser in Zelle verwenden-> hex code kopieren <br>
	 * 
	 * @param hex, 6 stellige farbcode, ohne # oder sonstige vorzeichen !
	 */
	public TurnierSheet tabColor(String hex) {
		return tabColor(Integer.parseInt(hex, 16));
	}
}
