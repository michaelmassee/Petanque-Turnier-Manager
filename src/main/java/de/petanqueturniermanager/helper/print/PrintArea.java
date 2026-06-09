/*
 * Erstellung 26.04.2019 / Michael Massee
 */
package de.petanqueturniermanager.helper.print;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XCellRangeAddressable;
import com.sun.star.sheet.XPrintAreas;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.table.CellRangeAddress;
import com.sun.star.table.XCellRange;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.Lo;
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
		CellRangeAddress aRangeAddress = Lo.qi(XCellRangeAddressable.class, aRange).getRangeAddress();
		XPrintAreas printArea = turnierSheet.queryInterfaceXSpreadsheet(XPrintAreas.class);
		printArea.setPrintAreas(new CellRangeAddress[] { aRangeAddress });
		return this;
	}

	/** Markiert Zeilen als Wiederholungszeilen (Drucktitel), so dass TabellenMapper sie als &lt;thead&gt; erkennt. */
	public PrintArea setTitelZeilen(int startZeile, int endZeile, int letzteSpalte) {
		XPrintAreas printAreas = turnierSheet.queryInterfaceXSpreadsheet(XPrintAreas.class);
		if (printAreas == null) {
			return this;
		}
		var titelZeilen = new CellRangeAddress();
		titelZeilen.StartColumn = 0;
		titelZeilen.EndColumn = letzteSpalte;
		titelZeilen.StartRow = startZeile;
		titelZeilen.EndRow = endZeile;
		printAreas.setTitleRows(titelZeilen);
		printAreas.setPrintTitleRows(true);
		return this;
	}
}
