package de.petanqueturniermanager.comp;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

// https://api.libreoffice.org/examples/examples.html#Java_examples
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/OpenOffice.org_Developers_Guide
// Addon Guide
//https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/AddOns/Add-Ons

public class DocumentHelper {
	private static final Logger logger = LogManager.getLogger(DocumentHelper.class);

	/** Returns the current XDesktop */
	static XDesktop getCurrentDesktop(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		XMultiComponentFactory xMCF = UnoRuntime.queryInterface(XMultiComponentFactory.class, xContext.getServiceManager());
		Object desktop = null;
		try {
			desktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return UnoRuntime.queryInterface(com.sun.star.frame.XDesktop.class, desktop);
	}

	/** Returns the current XComponent */
	static XComponent getCurrentComponent(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		return getCurrentDesktop(xContext).getCurrentComponent();
	}

	/** Returns the current frame */
	public static XFrame getCurrentFrame(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		XModel xModel = UnoRuntime.queryInterface(XModel.class, getCurrentComponent(xContext));
		return xModel.getCurrentController().getFrame();
	}

	/** Returns the current text document (if any) */
	static XTextDocument getCurrentTextDocument(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		return UnoRuntime.queryInterface(XTextDocument.class, getCurrentComponent(xContext));
	}

	/**
	 * Returns the current Spreadsheet document (if any) <br>
	 * mit absicht nicht public, weil mehrere Fenster gleichzeitig offen
	 */
	static XSpreadsheetDocument getCurrentSpreadsheetDocument(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		return UnoRuntime.queryInterface(XSpreadsheetDocument.class, getCurrentComponent(xContext));
	}

	/**
	 * Returns the current SpreadsheetView <br>
	 * mit absicht nicht public, weil mehrere Fenster gleichzeitig offen
	 */
	static XSpreadsheetView getCurrentSpreadsheetView(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		XModel xModel = UnoRuntime.queryInterface(XModel.class, getCurrentComponent(xContext));
		return UnoRuntime.queryInterface(XSpreadsheetView.class, xModel.getCurrentController());
	}
}
