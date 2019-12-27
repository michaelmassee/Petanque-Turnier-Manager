/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.pagestyle.PageStyle;
import de.petanqueturniermanager.helper.pagestyle.PageStyleDef;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class NewSheet extends BaseHelper {

	private static final Logger logger = LogManager.getLogger(NewSheet.class);

	private final SheetHelper sheetHelper;
	private final String sheetName;
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

	private NewSheet(ISheet iSheet, String sheetName) {
		super(iSheet);
		checkArgument(StringUtils.isNotBlank(sheetName));
		this.sheetName = sheetName;
		sheetHelper = new SheetHelper(iSheet.getWorkingSpreadsheet());
	}

	public static final NewSheet from(ISheet iSheet, String sheetName) {
		return new NewSheet(iSheet, sheetName);
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
	 * @throws GenerateException
	 */
	public NewSheet create() throws GenerateException {
		sheet = sheetHelper.findByName(sheetName);
		didCreate = false;

		TurnierSheet turnierSheet = null;

		if (sheet != null) {
			turnierSheet = TurnierSheet.from(sheet, getWorkingSpreadsheet());
			if (createNewIfExist) {
				turnierSheet.setActiv();
				MessageBoxResult result = MessageBox.from(getWorkingSpreadsheet().getxContext(), MessageBoxTypeEnum.WARN_YES_NO).caption("Erstelle " + sheetName)
						.message("'" + sheetName + "'\r\nist bereits vorhanden.\r\nLöschen und neu erstellen ?").forceOk(forceOkCreateNewWhenExist).show();
				if (MessageBoxResult.YES != result) {
					return this;
				}
				sheetHelper.removeSheet(sheetName);
				sheet = null;
				turnierSheet = null;
			}
		}

		if (sheet == null) {
			try {
				getWorkingSpreadsheetDocument().getSheets().insertNewByName(sheetName, pos);
				sheet = sheetHelper.findByName(sheetName);
				turnierSheet = TurnierSheet.from(sheet, getWorkingSpreadsheet());
				if (!showGrid) {
					// nur bei Neu, einmal abschalten
					turnierSheet.toggleSheetGrid();
				}

				if (pageStyleDef != null) {
					// Info: alle PageStyles werden in KonfigurationSheet initialisiert, (Header etc)
					// @see KonfigurationSheet#initPageStyles
					// @see SheetRunner#updateKonfigurationSheet
					PageStyleHelper.from(getISheet(), pageStyleDef).initDefaultFooter().create().applytoSheet();
				} else {
					// dann nur der default, mit copyright footer
					PageStyleHelper.from(getISheet(), new PageStyleDef(PageStyle.PETTURNMNGR)).initDefaultFooter().create().applytoSheet();
				}
				didCreate = true;

			} catch (IllegalArgumentException e) {
				logger.error(e.getMessage(), e);
			}
		}

		if (turnierSheet != null) {
			turnierSheet.protect(protect).tabColor(tabColor).setActiv(setActiv);
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
