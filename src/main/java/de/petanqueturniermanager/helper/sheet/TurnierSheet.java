/**
 * Erstellung 14.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class TurnierSheet {
	private static final Logger logger = LogManager.getLogger(TurnierSheet.class);
	private final WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet;
	private final WeakRefHelper<WorkingSpreadsheet> wkRefCurrentSpreadsheet;

	private TurnierSheet(XSpreadsheet xSpreadsheet, WorkingSpreadsheet currentSpreadsheet) {
		wkRefxSpreadsheet = new WeakRefHelper<>(checkNotNull(xSpreadsheet));
		wkRefCurrentSpreadsheet = new WeakRefHelper<>(checkNotNull(currentSpreadsheet));
	}

	public static TurnierSheet from(XSpreadsheet xSpreadsheet, WorkingSpreadsheet currentSpreadsheet) {
		return new TurnierSheet(checkNotNull(xSpreadsheet), checkNotNull(currentSpreadsheet));
	}

	/**
	 * @param setActiv
	 */
	public TurnierSheet setActiv(boolean setActiv) {
		if (setActiv) {
			setActiv();
		}
		return this;
	}

	public TurnierSheet setActiv() {
		wkRefCurrentSpreadsheet.get().getWorkingSpreadsheetView().setActiveSheet(wkRefxSpreadsheet.get());
		return this;
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
	 * grid nur auch aktuelle sheet ein oder ausschalten.<br>
	 * verwendet DispatchHelper<br>
	 * Sheet wird Aktiviert !
	 *
	 * @return
	 */
	public TurnierSheet toggleSheetGrid() {
		// sheet muss activ sein !
		setActiv();

		// VB Code, mit Macro recorder aufgezeichnet
		// document = ThisComponent.CurrentController.Frame
		// dispatcher = createUnoService("com.sun.star.frame.DispatchHelper")
		// dispatcher.executeDispatch (document, ".uno: ToggleSheetGrid", "", 0, Array ())
		PropertyValue[] aArgs = new PropertyValue[0];
		wkRefCurrentSpreadsheet.get().executeDispatch(".uno:ToggleSheetGrid", "", 0, aArgs);
		return this;
	}

	//

	/**
	 * Keine änderungen mehr erlaubt wenn protect = true<br>
	 * <br>
	 * 16.04.2019 <br>
	 * BUG ? lo = 6.2.2 ? Style lassen sich nicht mehr ändern wenn irgendein sheet is protected
	 *
	 * @param protect true/false
	 */
	TurnierSheet protect(boolean protect) {
		// XProtectable xProtectable = UnoRuntime.queryInterface(XProtectable.class, xSpreadsheet);
		// if (protect) {
		// xProtectable.protect("");
		// } else {
		// xProtectable.unprotect("");
		// }
		return this;
	}

	public boolean isProtected() {
		XProtectable xProtectable = queryInterface(XProtectable.class);
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
		if (color > 0) {
			XPropertySet xPropSet = UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class, wkRefxSpreadsheet.get());
			if (xPropSet != null) {
				try {
					xPropSet.setPropertyValue("TabColor", new Integer(color));
				} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
					logger.error(e.getMessage(), e);
				}
			}
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
		if (StringUtils.isNotBlank(hex)) {
			tabColor(Integer.parseInt(hex, 16));
		}
		return this;
	}

	public XCellRange getCellRangeByPosition(RangePosition range) {
		checkNotNull(range);
		try {
			return wkRefxSpreadsheet.get().getCellRangeByPosition(range.getStartSpalte(), range.getStartZeile(), range.getEndeSpalte(), range.getEndeZeile());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public <C> C queryInterface(Class<C> clazz) {
		return UnoRuntime.queryInterface(clazz, wkRefxSpreadsheet.get());
	}

}
