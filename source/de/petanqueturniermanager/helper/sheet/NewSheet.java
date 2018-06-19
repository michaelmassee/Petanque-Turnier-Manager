/**
* Erstellung : 30.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.MessageBox;
import de.petanqueturniermanager.helper.msgbox.MessageBoxResult;
import de.petanqueturniermanager.helper.msgbox.MessageBoxTypeEnum;
import de.petanqueturniermanager.helper.pagestyle.PageStyleDef;
import de.petanqueturniermanager.helper.pagestyle.PageStyleHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class NewSheet {

	private final SheetHelper sheetHelper;
	private final String sheetName;
	private final XComponentContext xContext;
	private boolean didCreate = false;
	private boolean force = false;
	private boolean setActiv = false;
	private short pos = 1;
	private String tabColor = null;
	private PageStyleDef pageStyleDef = null;
	private XSpreadsheet sheet = null;

	private NewSheet(XComponentContext xContext, String sheetName) {
		checkNotNull(xContext);
		checkArgument(StringUtils.isNotBlank(sheetName));
		this.xContext = xContext;
		this.sheetName = sheetName;
		sheetHelper = new SheetHelper(xContext);
	}

	public static final NewSheet from(XComponentContext xContext, String sheetName) {
		return new NewSheet(xContext, sheetName);
	}

	public NewSheet setForceCreate(boolean force) {
		this.force = force;
		return this;
	}

	public NewSheet forceCreate() {
		force = true;
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

	public NewSheet spielTagPageStyle(SpielTagNr spielTagNr) throws GenerateException {
		pageStyleDef = new PageStyleDef(spielTagNr);
		return this;
	}

	public NewSheet create() throws GenerateException {
		sheet = sheetHelper.findByName(sheetName);
		didCreate = false;
		if (sheet != null) {
			sheetHelper.setActiveSheet(sheet);
			MessageBoxResult result = MessageBox.from(xContext, MessageBoxTypeEnum.WARN_YES_NO).caption("Erstelle " + sheetName)
					.message("'" + sheetName + "'\r\nist bereits vorhanden.\r\nLöschen und neu erstellen ?").forceOk(force).show();
			if (MessageBoxResult.YES != result) {
				return this;
			}
			sheetHelper.removeSheet(sheetName);
		}
		sheet = sheetHelper.newIfNotExist(sheetName, pos, tabColor);

		if (setActiv) {
			sheetHelper.setActiveSheet(sheet);
		}

		if (pageStyleDef != null) {
			// Info: alle PageStyles werden in KonfigurationSheet initialisiert, (Header etc)
			// @see KonfigurationSheet#initPageStyles
			// @see SheetRunner#updateKonfigurationSheet
			PageStyleHelper.from(sheet, xContext, pageStyleDef).initDefaultFooter().create().applytoSheet();
		}

		didCreate = true;
		return this;
	}

	public boolean isDidCreate() {
		return didCreate;
	}

	public NewSheet setActiv() {
		setActiv = true;
		return this;
	}

	public XSpreadsheet getSheet() {
		return sheet;
	}

}
