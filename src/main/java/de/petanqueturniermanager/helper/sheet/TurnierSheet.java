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
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheets;
import com.sun.star.table.XCell;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.util.XProtectable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * @author Michael Massee
 *
 */
public class TurnierSheet {
	private static final Logger logger = LogManager.getLogger(TurnierSheet.class);
	private final WeakRefHelper<XSpreadsheet> wkRefxSpreadsheet;
	private final WeakRefHelper<WorkingSpreadsheet> wkRefWorkingSpreadsheet;

	private TurnierSheet(XSpreadsheet xSpreadsheet, WorkingSpreadsheet currentSpreadsheet) {
		wkRefxSpreadsheet = new WeakRefHelper<>(checkNotNull(xSpreadsheet));
		wkRefWorkingSpreadsheet = new WeakRefHelper<>(checkNotNull(currentSpreadsheet));
	}

	public static TurnierSheet from(XSpreadsheet xSpreadsheet, WorkingSpreadsheet currentSpreadsheet) {
		return new TurnierSheet(checkNotNull(xSpreadsheet), checkNotNull(currentSpreadsheet));
	}

	public static TurnierSheet from(String name, WorkingSpreadsheet currentSpreadsheet) throws GenerateException {
		XSpreadsheet xSpreadsheet = new SheetHelper(currentSpreadsheet).findByName(checkNotNull(name));
		if (xSpreadsheet == null) {
			throw new GenerateException("Die Tabelle '" + name + "' ist nicht vorhanden.");
		}
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
		wkRefWorkingSpreadsheet.get().getWorkingSpreadsheetView().setActiveSheet(wkRefxSpreadsheet.get());
		return this;
	}

	/**
	 * Keine änderungen am sheet mehr erlaubt
	 *
	 */
	public TurnierSheet protect() {
		return protect(true);
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
		wkRefWorkingSpreadsheet.get().executeDispatch(".uno:ToggleSheetGrid", "", 0, aArgs);
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
		XProtectable xProtectable = queryInterfaceXSpreadsheet(XProtectable.class);
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
			XPropertySet xPropSet = UnoRuntime.queryInterface(com.sun.star.beans.XPropertySet.class,
					wkRefxSpreadsheet.get());
			if (xPropSet != null) {
				try {
					xPropSet.setPropertyValue("TabColor", new Integer(color));
				} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
						| WrappedTargetException e) {
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
			return wkRefxSpreadsheet.get().getCellRangeByPosition(range.getStartSpalte(), range.getStartZeile(),
					range.getEndeSpalte(), range.getEndeZeile());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return null;
	}

	public <C> C queryInterfaceXSpreadsheet(Class<C> clazz) {
		return UnoRuntime.queryInterface(clazz, wkRefxSpreadsheet.get());
	}

	public <C> C queryInterfaceSpreadsheetView(Class<C> clazz) {
		WorkingSpreadsheet workingSpreadsheet = wkRefWorkingSpreadsheet.get();
		if (workingSpreadsheet != null) {
			return UnoRuntime.queryInterface(clazz, workingSpreadsheet.getWorkingSpreadsheetView());
		}
		return null;
	}

	// iSheetNr = oMySheet.RangeAddress.Sheet
	// https://forum.openoffice.org/en/forum/viewtopic.php?t=86514
	// https://github.com/KWARC/Sally/blob/9950c9e54e11d80073fa4c19377ce6e1a50fd112/LibreAlex/source/info/kwarc/sally/AlexLibre/Sally/SCUtil.java
	// https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Capabilities_of_SheetCellRanges_Container
	// https://wiki.openoffice.org/wiki/Documentation/DevGuide/Spreadsheets/Cell_Ranges_and_Cells_Container

	public int getSheetPosition() {
		XSpreadsheets xSpreadsheets = wkRefWorkingSpreadsheet.get().getWorkingSpreadsheetDocument().getSheets();
		XIndexAccess xIndexAccess = UnoRuntime.queryInterface(XIndexAccess.class, xSpreadsheets);
		int anzSheets = xIndexAccess.getCount();
		String thisSheetName = getName();
		int retIdx = -1;

		try {
			for (int index = 0; index < anzSheets; index++) {
				XSpreadsheet xSpreadsheet = UnoRuntime.queryInterface(XSpreadsheet.class,
						xIndexAccess.getByIndex(index));
				XNamed xsheetname = UnoRuntime.queryInterface(XNamed.class, xSpreadsheet);
				if (StringUtils.equals(thisSheetName, xsheetname.getName())) {
					retIdx = index;
					break;
				}
			}
		} catch (IndexOutOfBoundsException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}

		return retIdx;
	}

	public String getName() {
		XNamed xsheetname = UnoRuntime.queryInterface(XNamed.class, wkRefxSpreadsheet.get());
		return xsheetname.getName();
	}

	public XCell getCell(Position pos) {
		checkNotNull(pos);
		XCell xCell = null;
		try {
			xCell = wkRefxSpreadsheet.get().getCellByPosition(pos.getColumn(), pos.getRow());
		} catch (IndexOutOfBoundsException e) {
			logger.error(e.getMessage(), e);
		}
		return xCell;
	}

}
