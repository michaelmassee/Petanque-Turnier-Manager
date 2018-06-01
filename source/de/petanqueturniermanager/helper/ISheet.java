/**
* Erstellung : 01.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

public interface ISheet {

	public SheetHelper getSheetHelper() throws GenerateException;

	public XSpreadsheet getSheet() throws GenerateException;

	public Logger getLogger();

	public XComponentContext getxContext();

	public void processBoxinfo(String infoMsg);

}
