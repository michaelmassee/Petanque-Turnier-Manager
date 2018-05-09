/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.*;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.awt.MessageBoxResults;
import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.msgbox.WarningBox;

public class NewSheet {

	private final SheetHelper sheetHelper;
	private final String sheetName;
	private final XComponentContext xContext;
	private boolean didCreate = false;
	private boolean force = false;
	private boolean setActiv = false;
	private short pos = 1;
	private String tabColor = null;

	private NewSheet(XComponentContext xContext, String sheetName) {
		checkNotNull(xContext);
		checkArgument(StringUtils.isNotBlank(sheetName));
		this.xContext = xContext;
		this.sheetName = sheetName;
		this.sheetHelper = new SheetHelper(xContext);
	}

	public static final NewSheet from(XComponentContext xContext, String sheetName) {
		return new NewSheet(xContext, sheetName);
	}

	public NewSheet forceCreate() {
		this.force = true;
		return this;
	}

	public NewSheet pos(short pos) {
		this.pos = pos;
		return this;
	}

	public NewSheet tabColor(String color) {
		this.tabColor = color;
		return this;
	}

	public boolean create() {
		XSpreadsheet sheet = this.sheetHelper.findByName(this.sheetName);
		if (sheet != null && !this.force) {
			WarningBox warnBox = new WarningBox(this.xContext);
			short result = warnBox.showYesNo("Erstelle Tabelle",
					"Tabelle '" + this.sheetName + "'\r\nist bereits vorhanden.\r\nLöschen und neu erstellen ?");
			if (result != MessageBoxResults.YES) {
				return false;
			}
			this.sheetHelper.removeSheet(this.sheetName);
		}
		sheet = this.sheetHelper.newIfNotExist(this.sheetName, this.pos, this.tabColor);

		if (this.setActiv) {
			this.sheetHelper.setActiveSheet(sheet);
		}

		this.didCreate = true;
		return this.didCreate;
	}

	public boolean isDidCreate() {
		return this.didCreate;
	}

	public NewSheet setActiv() {
		this.setActiv = true;
		return this;
	}
}
