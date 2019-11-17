/**
 * Erstellung 03.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyValue;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDispatchHelper;
import com.sun.star.frame.XDispatchProvider;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * @author Michael Massee <br>
 * beim start active Dokumenten merken
 */
public class WorkingSpreadsheet {

	private static final Logger logger = LogManager.getLogger(WorkingSpreadsheet.class);

	private final XComponentContext xContext;
	private final XSpreadsheetDocument workingpreadsheetDocument;
	private final XSpreadsheetView workingSpreadsheetView;
	private final XController xController;

	public WorkingSpreadsheet(XComponentContext xContext) {
		this.xContext = checkNotNull(xContext);
		// Save the current Aktiv Documents
		workingpreadsheetDocument = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
		workingSpreadsheetView = DocumentHelper.getCurrentSpreadsheetView(xContext);
		xController = DocumentHelper.getXModel(xContext).getCurrentController();
	}

	/**
	 * @return the xContext
	 */
	public XComponentContext getxContext() {
		return xContext;
	}

	/**
	 * @return the currentSpreadsheetDocument
	 */
	public XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return workingpreadsheetDocument;
	}

	/**
	 * @return the currentSpreadsheetView
	 */
	public XSpreadsheetView getWorkingSpreadsheetView() {
		return workingSpreadsheetView;
	}

	/**
	 * @return
	 */
	public XController getCurrentController() {
		return xController;
	}

	public void executeDispatch(String str1, String str2, int val, PropertyValue[] propertyVals) {
		XDispatchHelper xDispatchHelper = getXDispatchHelper();
		XDispatchProvider xDispatchProvider = UnoRuntime.queryInterface(XDispatchProvider.class, getCurrentController().getFrame());
		if (xDispatchHelper != null && xDispatchProvider != null) {
			xDispatchHelper.executeDispatch(xDispatchProvider, str1, str2, val, propertyVals);
		}
	}

	public XDispatchHelper getXDispatchHelper() {
		XDispatchHelper xDPH = null;
		try {
			Object dispatchHelper = getxContext().getServiceManager().createInstanceWithContext("com.sun.star.frame.DispatchHelper", getxContext());
			xDPH = UnoRuntime.queryInterface(XDispatchHelper.class, dispatchHelper);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return xDPH;
	}

}
