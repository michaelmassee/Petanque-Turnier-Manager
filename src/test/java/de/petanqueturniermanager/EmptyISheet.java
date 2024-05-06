package de.petanqueturniermanager;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.konfiguration.IKonfigurationSheet;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * Erstellung 14.04.2024 / Michael Massee
 */

public class EmptyISheet extends SheetRunner implements ISheet {

	private static final Logger logger = LogManager.getLogger(EmptyISheet.class);

	XSpreadsheet testSheet;
	final String sheetName;

	public EmptyISheet(WorkingSpreadsheet workingSpreadsheet, TurnierSystem spielSystem, String sheetName) {
		super(workingSpreadsheet, spielSystem);
		this.sheetName = checkNotNull(sheetName);
	}

	@Override
	public XSpreadsheet getXSpreadSheet() throws GenerateException {
		return checkNotNull(testSheet);
	}

	@Override
	public Logger getLogger() {
		return logger;
	}

	@Override
	public TurnierSheet getTurnierSheet() throws GenerateException {
		return TurnierSheet.from(getXSpreadSheet(), getWorkingSpreadsheet());
	}

	@Override
	protected IKonfigurationSheet getKonfigurationSheet() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void doRun() throws GenerateException {
		initSheet();
	}

	public void initSheet() throws GenerateException {
		testSheet = getSheetHelper().newIfNotExist(sheetName, 0);
		getTurnierSheet().setActiv();
	}

}
