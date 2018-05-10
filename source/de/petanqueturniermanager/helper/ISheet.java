/**
* Erstellung : 01.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.Logger;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.exception.GenerateException;

public interface ISheet {
	public XSpreadsheet getSheet() throws GenerateException;

	public Logger getLogger();
}
