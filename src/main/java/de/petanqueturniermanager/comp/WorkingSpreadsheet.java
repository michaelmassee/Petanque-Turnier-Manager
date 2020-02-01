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
import com.sun.star.frame.XModel;
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
	private final XModel xModel;

	// private final XSpreadsheetDocument workingSpreadsheetDocument;
	// private final XSpreadsheetView workingSpreadsheetView;
	// private final XController xController;

	public WorkingSpreadsheet(XComponentContext xContext) {
		this.xContext = checkNotNull(xContext);
		// Save the current Aktiv Document
		XSpreadsheetDocument workingSpreadsheetDocument = DocumentHelper.getCurrentSpreadsheetDocument(xContext);
		xModel = checkNotNull(UnoRuntime.queryInterface(XModel.class, workingSpreadsheetDocument));
		// XSpreadsheetDocument doc = UnoRuntime.queryInterface(XSpreadsheetDocument.class, xModel);
		// XSpreadsheetView view = UnoRuntime.queryInterface(XSpreadsheetView.class, xModel.getCurrentController());
		// XController currentController = xModel.getCurrentController();
		//
		// workingSpreadsheetView = DocumentHelper.getCurrentSpreadsheetView(xContext);
		// xController = DocumentHelper.getXModel(xContext).getCurrentController();

	}

	/**
	 * @param getxContext
	 * @param xSpreadsheetDocument
	 * @param xSpreadsheetView
	 */
	public WorkingSpreadsheet(XComponentContext xContext, XModel xModel) {
		this.xContext = checkNotNull(xContext);
		this.xModel = xModel;
		// workingSpreadsheetDocument = checkNotNull(xSpreadsheetDocument);
		// workingSpreadsheetView = checkNotNull(xSpreadsheetView);
		// xController = DocumentHelper.getXModel(xContext).getCurrentController();
	}

	public boolean compareSpreadsheetDocument(WorkingSpreadsheet workingSpreadsheet) {
		if (workingSpreadsheet == null) {
			return false;
		}
		if (getWorkingSpreadsheetDocument() == null || workingSpreadsheet.getWorkingSpreadsheetDocument() == null) {
			return false;
		}
		return getWorkingSpreadsheetDocument().equals(workingSpreadsheet.getWorkingSpreadsheetDocument());
	}

	/**
	 * @return the xContext
	 */
	public XComponentContext getxContext() {
		return xContext;
	}

	public XController getXController() {
		return xModel.getCurrentController();
	}

	/**
	 * @return the currentSpreadsheetDocument
	 */
	public XSpreadsheetDocument getWorkingSpreadsheetDocument() {
		return UnoRuntime.queryInterface(XSpreadsheetDocument.class, xModel);
	}

	/**
	 * @return the currentSpreadsheetView
	 */
	public XSpreadsheetView getWorkingSpreadsheetView() {
		return UnoRuntime.queryInterface(XSpreadsheetView.class, xModel.getCurrentController());
	}

	public void executeDispatch(String str1, String str2, int val, PropertyValue[] propertyVals) {
		XDispatchHelper xDispatchHelper = getXDispatchHelper();
		XDispatchProvider xDispatchProvider = UnoRuntime.queryInterface(XDispatchProvider.class, getXController().getFrame());
		if (xDispatchHelper != null && xDispatchProvider != null) {
			xDispatchHelper.executeDispatch(xDispatchProvider, str1, str2, val, propertyVals);
		}
	}

	public XDispatchHelper getXDispatchHelper() {
		return createInstanceMCF(XDispatchHelper.class, "com.sun.star.frame.DispatchHelper");
	}

	/**
	 * @param <T>
	 * @param aType
	 * @param serviceName
	 * @return
	 */
	public <T> T createInstanceMCF(Class<T> aType, String serviceName) {
		checkNotNull(aType);
		checkNotNull(serviceName);

		T interfaceObj = null;
		try {
			Object multiComponentFactory = getxContext().getServiceManager().createInstanceWithContext(serviceName, getxContext());
			// create service component using the specified component context
			interfaceObj = UnoRuntime.queryInterface(aType, multiComponentFactory);
			// uses bridge to obtain proxy to remote interface inside service;
			// implements casting across process boundaries
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return interfaceObj;
	}
}
