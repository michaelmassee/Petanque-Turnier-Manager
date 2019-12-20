/**
* Erstellung : 01.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.TurnierSheet;

public interface ISheet {

	SheetHelper getSheetHelper() throws GenerateException;

	XSpreadsheet getXSpreadSheet() throws GenerateException;

	Logger getLogger();

	XComponentContext getxContext();

	WorkingSpreadsheet getWorkingSpreadsheet();

	void processBoxinfo(String infoMsg);

	TurnierSheet getTurnierSheet() throws GenerateException;

}
