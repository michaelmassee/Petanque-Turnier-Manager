package de.petanqueturniermanager.comp;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.net.MalformedURLException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XWindow;
import com.sun.star.beans.PropertyValue;
import com.sun.star.document.MacroExecMode;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.XComponent;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.uno.Exception;
import com.sun.star.util.CloseVetoException;
import com.sun.star.util.XCloseable;

import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.PropertyValueHelper;

/**
 * Erstellung 10.07.2022 / Michael Massee
 */

public class OfficeDocumentHelper {
	private static final Logger logger = LogManager.getLogger(OfficeDocumentHelper.class);

	// docType ints
	public static final int UNKNOWN = 0;
	public static final int WRITER = 1;
	public static final int BASE = 2;
	public static final int CALC = 3;
	public static final int DRAW = 4;
	public static final int IMPRESS = 5;
	public static final int MATH = 6;

	// docType strings
	public static final String UNKNOWN_STR = "unknown";
	public static final String WRITER_STR = "swriter";
	public static final String BASE_STR = "sbase";
	public static final String CALC_STR = "scalc";
	public static final String DRAW_STR = "sdraw";
	public static final String IMPRESS_STR = "simpress";
	public static final String MATH_STR = "smath";

	// docType service names
	public static final String UNKNOWN_SERVICE = "com.sun.frame.XModel";
	public static final String WRITER_SERVICE = "com.sun.star.text.TextDocument";
	public static final String BASE_SERVICE = "com.sun.star.sdb.OfficeDatabaseDocument";
	public static final String CALC_SERVICE = "com.sun.star.sheet.SpreadsheetDocument";
	public static final String DRAW_SERVICE = "com.sun.star.drawing.DrawingDocument";
	public static final String IMPRESS_SERVICE = "com.sun.star.presentation.PresentationDocument";
	public static final String MATH_SERVICE = "com.sun.star.formula.FormulaProperties";

	// CLSIDs for Office documents
	// defined in <OFFICE>\officecfg\registry\data\org\openoffice\Office\Embedding.xcu
	public static final String WRITER_CLSID = "8BC6B165-B1B2-4EDD-aa47-dae2ee689dd6";
	public static final String CALC_CLSID = "47BBB4CB-CE4C-4E80-a591-42d9ae74950f";
	public static final String DRAW_CLSID = "4BAB8970-8A3B-45B3-991c-cbeeac6bd5e3";
	public static final String IMPRESS_CLSID = "9176E48A-637A-4D1F-803b-99d9bfac1047";
	public static final String MATH_CLSID = "078B7ABA-54FC-457F-8551-6147e776a997";
	public static final String CHART_CLSID = "12DCAE26-281F-416F-a234-c3086127382e";

	private final XComponentLoader loader;

	private OfficeDocumentHelper(XComponentLoader loader) {
		this.loader = checkNotNull(loader);
	}

	static public OfficeDocumentHelper from(XComponentLoader loader) {
		return new OfficeDocumentHelper(loader);
	}

	public XSpreadsheetDocument createCalc() {
		XComponent doc = createDoc(CALC_STR);
		return Lo.qi(XSpreadsheetDocument.class, doc);
	}

	private XComponent createDoc(String docType) {
		return createDoc(docType, PropertyValueHelper.from().add("Hidden", true).propList());
	}

	private XComponent createMacroDoc(String docType) {
		// ("MacroExecutionMode", MacroExecMode.ALWAYS_EXECUTE)
		return createDoc(docType, PropertyValueHelper.from().add("Hidden", true)
				.add("MacroExecutionMode", MacroExecMode.ALWAYS_EXECUTE_NO_WARN).propList());
	}

	// create a new document of the specified type
	private XComponent createDoc(String docType, PropertyValue[] props) {
		logger.info("Creating Office document " + docType);
		// PropertyValue[] props = Props.makeProps("Hidden", true);
		// if Hidden == true, office will not terminate properly
		XComponent doc = null;
		try {
			doc = loader.loadComponentFromURL("private:factory/" + docType, "_blank", 0, props);
			// msFactory = Lo.qi(XMultiServiceFactory.class, doc);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return doc;
	}

	// create a new document using the specified template
	private XComponent createDocFromTemplate(String templatePath, XComponentLoader loader) {
		File templateFile = new File(templatePath);
		if (!templateFile.canRead()) {
			return null;
		}
		XComponent doc = null;
		logger.info("Opening template " + templatePath);
		try {
			String templateURL = templateFile.toURI().toURL().toExternalForm();
			if (templateURL == null)
				return null;

			PropertyValue[] props = PropertyValueHelper.from().add("Hidden", true).add("AsTemplate", true).propList();

			doc = loader.loadComponentFromURL(templateURL, "_blank", 0, props);
			//			msFactory = Lo.qi(XMultiServiceFactory.class, doc);
		} catch (Exception | MalformedURLException e) {
			logger.error("Could not create document from template: " + e.getMessage(), e);
		}
		return doc;
	}

	public static void closeDoc(Object doc) {
		try {
			XCloseable closeable = Lo.qi(XCloseable.class, doc);
			close(closeable);
		} catch (com.sun.star.lang.DisposedException e) {
			logger.error("Document close failed since Office link disposed", e);
		}
	}

	public static void close(XCloseable closeable) {
		if (closeable == null) {
			return;
		}
		logger.info("Closing the document");
		try {
			closeable.close(false); // true to force a close
			// set modifiable to false to close a modified doc without complaint setModified(False)
		} catch (CloseVetoException e) {
			logger.error("Close was vetoed", e);
		}
	} // end of close()

	public static void setVisible(Object objDoc, boolean isVisible) {
		XComponent doc = Lo.qi(XComponent.class, objDoc);
		XWindow xWindow = getFrame(doc).getContainerWindow();
		xWindow.setVisible(isVisible);
		xWindow.setFocus();
	}

	public static void setVisible(boolean isVisible, XDesktop desktop) {
		XWindow xWindow = getWindow(desktop);
		if (xWindow != null) {
			xWindow.setVisible(isVisible);
			xWindow.setFocus();
		}
	}

	public static XFrame getFrame(XComponent doc) {
		return getCurrentController(doc).getFrame();
	}

	// was XComponent
	public static XController getCurrentController(Object odoc) {
		XComponent doc = Lo.qi(XComponent.class, odoc);
		XModel model = Lo.qi(XModel.class, doc);
		if (model == null) {
			logger.error("Document has no data model");
			return null;
		}
		return model.getCurrentController();
	}

	public static XWindow getWindow(XDesktop desktop) {
		XFrame frame = desktop.getCurrentFrame();
		if (frame == null) {
			System.out.println("No current frame");
			return null;
		} else
			return frame.getContainerWindow();
	} // end of getWindow()

}
