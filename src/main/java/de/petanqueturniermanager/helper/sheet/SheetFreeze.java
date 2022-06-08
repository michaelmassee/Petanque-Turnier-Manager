package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XViewFreezable;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * Erstellung 08.06.2022 / Michael Massee
 * 
 * Headerzeilen festsetzen<br>
 * https://api.libreoffice.org/docs/idl/ref/interfacecom_1_1sun_1_1star_1_1sheet_1_1XViewFreezable.html<br>
 */

public class SheetFreeze {

	// To freeze only horizontally, specify nRows as 0. To freeze only vertically, specify nColumns as 0.

	private final XViewFreezable sheetFreeze;
	private int anzZeilen = 0;
	private int anzSpalten = 0;

	private SheetFreeze(TurnierSheet turnierSheet) {
		checkNotNull(turnierSheet);
		this.sheetFreeze = turnierSheet.queryInterfaceSpreadsheetView(XViewFreezable.class);
	}

	public static SheetFreeze from(TurnierSheet turnierSheet) {
		return new SheetFreeze(turnierSheet);
	}

	public static SheetFreeze from(XSpreadsheet xSpreadsheet, WorkingSpreadsheet currentSpreadsheet) {
		return new SheetFreeze(TurnierSheet.from(xSpreadsheet, currentSpreadsheet));
	}

	public SheetFreeze anzZeilen(int anzZeilen) {
		this.anzZeilen = anzZeilen;
		return this;
	}

	public SheetFreeze anzSpalten(int anzSpalten) {
		this.anzSpalten = anzSpalten;
		return this;
	}

	public SheetFreeze doFreeze() {
		sheetFreeze.freezeAtPosition(this.anzSpalten, this.anzZeilen);
		return this;
	}

}
