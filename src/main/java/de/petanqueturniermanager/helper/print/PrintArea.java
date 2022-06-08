/**
 * Erstellung 26.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.print;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

/**
 * @author Michael Massee
 *
 */
public class PrintArea {

	private final TurnierSheet turnierSheet;

	private PrintArea(TurnierSheet turnierSheet) {
		checkNotNull(turnierSheet);
		this.turnierSheet = turnierSheet;
	}

	public static PrintArea from(TurnierSheet turnierSheet) {
		return new PrintArea(turnierSheet);
	}

	public static PrintArea from(XSpreadsheet xSpreadsheet, WorkingSpreadsheet currentSpreadsheet) {
		return new PrintArea(TurnierSheet.from(xSpreadsheet, currentSpreadsheet));
	}

	public PrintArea setPrintArea(RangePosition range) {
		checkNotNull(range);
		XCellRange aRange = turnierSheet.getCellRangeByPosition(range);
		CellRangeAddress aRangeAddress = UnoRuntime.queryInterface(XCellRangeAddressable.class, aRange).getRangeAddress();
		XPrintAreas printArea = turnierSheet.queryInterfaceXSpreadsheet(XPrintAreas.class);
		printArea.setPrintAreas(new CellRangeAddress[] { aRangeAddress });
		return this;
	}
}
