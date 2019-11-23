/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.pagestyle.PageStyleDef;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class NewSheet {

	private static final Logger logger = LogManager.getLogger(NewSheet.class);

	private final SheetHelper sheetHelper;
	private final String sheetName;
	private final WeakRefHelper<WorkingSpreadsheet> wkRefworkingSpreadsheet;
	private boolean didCreate = false;
	private boolean forceOkCreateNewWhenExist = false;
	private boolean setActiv = false;
	private boolean protect = false;
	private short pos = 1;
	private String tabColor = null;
	private PageStyleDef pageStyleDef = null;
	private XSpreadsheet sheet = null;
	private boolean showGrid = true;
	private boolean createNewIfExist = true;

	private NewSheet(WorkingSpreadsheet workingSpreadsheet, String sheetName) {
		wkRefworkingSpreadsheet = new WeakRefHelper<>(checkNotNull(workingSpreadsheet));
		checkArgument(StringUtils.isNotBlank(sheetName));
		this.sheetName = sheetName;
		sheetHelper = new SheetHelper(workingSpreadsheet);
	}

	public static final NewSheet from(WorkingSpreadsheet workingSpreadsheet, String sheetName) {
		return new NewSheet(workingSpreadsheet, sheetName);
	}

	public NewSheet setForceCreate(boolean force) {
		forceOkCreateNewWhenExist = force;
		return this;
	}

	public NewSheet forceCreate() {
		forceOkCreateNewWhenExist = true;
		return this;
	}

	public NewSheet pos(short pos) {
		this.pos = pos;
		return this;
	}

	public NewSheet tabColor(String color) {
		tabColor = color;
		return this;
	}

	public NewSheet spielTagPageStyle(SpielTagNr spielTagNr) {
		pageStyleDef = new PageStyleDef(spielTagNr);
		return this;
	}

	public NewSheet newIfExist() {
		createNewIfExist = true;
		return this;
	}

	/**
	 * wenn bereits vorhanden, dann nichts tun, sondern einfach return.
	 *
	 * @return
	 */
	public NewSheet useIfExist() {
		createNewIfExist = false;
		return this;
	}

	/**
	 *
	 * @return
	 */
	public NewSheet create() {
		sheet = sheetHelper.findByName(sheetName);
		didCreate = false;

		if (sheet != null) {
			if (createNewIfExist) {
				MessageBoxResult result = MessageBox.from(wkRefworkingSpreadsheet.get().getxContext(), MessageBoxTypeEnum.WARN_YES_NO).caption("Erstelle " + sheetName)
						.message("'" + sheetName + "'\r\nist bereits vorhanden.\r\nLöschen und neu erstellen ?").forceOk(forceOkCreateNewWhenExist).show();
				if (MessageBoxResult.YES != result) {
					return this;
				}
				sheetHelper.removeSheet(sheetName);
				sheet = null;
			} else {
				didCreate = true;
			}
		}

		if (sheet == null) {
			try {
				wkRefworkingSpreadsheet.get().getWorkingSpreadsheetDocument().getSheets().insertNewByName(sheetName, pos);
				sheet = sheetHelper.findByName(sheetName);
				if (!showGrid) {
					// nur bei Neu, einmal abschalten
					TurnierSheet.from(sheet, wkRefworkingSpreadsheet.get()).toggleSheetGrid();
				}
				TurnierSheet.from(sheet, wkRefworkingSpreadsheet.get()).protect(protect).tabColor(tabColor).setActiv(setActiv);

				if (pageStyleDef != null) {
					// Info: alle PageStyles werden in KonfigurationSheet initialisiert, (Header etc)
					// @see KonfigurationSheet#initPageStyles
					// @see SheetRunner#updateKonfigurationSheet
					PageStyleHelper.from(sheet, wkRefworkingSpreadsheet.get(), pageStyleDef).initDefaultFooter().create().applytoSheet();
				}
				didCreate = true;

			} catch (IllegalArgumentException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return this;
	}

	public boolean isDidCreate() {
		return didCreate;
	}

	public NewSheet setActiv() {
		setActiv = true;
		return this;
	}

	public NewSheet showGrid() {
		showGrid = true;
		return this;
	}

	public NewSheet hideGrid() {
		showGrid = false;
		return this;
	}

	public NewSheet protect() {
		protect = true;
		return this;
	}

	public XSpreadsheet getSheet() {
		return sheet;
	}
}
