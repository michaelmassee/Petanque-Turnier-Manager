package de.petanqueturniermanager.comp;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.sheet.XSpreadsheetView;
import com.sun.star.text.XTextDocument;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.basesheet.konfiguration.BasePropertiesSpalte;
import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.DocumentPropertiesHelper;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

// https://api.libreoffice.org/examples/examples.html#Java_examples
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/OpenOffice.org_Developers_Guide
// Addon Guide
// https://wiki.openoffice.org/wiki/Documentation/DevGuide/WritingUNO/AddOns/Add-Ons

public class DocumentHelper {
	private static final Logger logger = LogManager.getLogger(DocumentHelper.class);

	/** Returns the current XDesktop */
	public static XDesktop getCurrentDesktop(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		XMultiComponentFactory xMCF = Lo.qi(XMultiComponentFactory.class, xContext.getServiceManager());
		Object desktop = null;
		try {
			desktop = xMCF.createInstanceWithContext("com.sun.star.frame.Desktop", xContext);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
			return null;
		}
		return Lo.qi(com.sun.star.frame.XDesktop.class, desktop);
	}

	/** Returns the current XComponent */
	static XComponent getCurrentComponent(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		return getCurrentDesktop(xContext).getCurrentComponent();
	}

	/** Returns the current frame can be null */
	public static XFrame getCurrentFrame(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		XModel xModel = getXModel(xContext);
		if (xModel != null) {
			XController currentController = getXModel(xContext).getCurrentController();
			if (currentController != null) {
				return currentController.getFrame();
			}
		}
		return null;
	}

	/** Returns the current text document (if any) */
	static XTextDocument getCurrentTextDocument(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		return Lo.qi(XTextDocument.class, getCurrentComponent(xContext));
	}

	/**
	 * Returns the current Spreadsheet document (if any) <br>
	 *
	 */
	public static XSpreadsheetDocument getCurrentSpreadsheetDocument(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		return Lo.qi(XSpreadsheetDocument.class, getCurrentComponent(xContext));
	}

	/**
	 * Returns the current SpreadsheetView <br>
	 * mit absicht nicht public, weil mehrere Fenster gleichzeitig offen
	 */
	static XSpreadsheetView getCurrentSpreadsheetView(XComponentContext xContext) {
		checkNotNull(xContext, "xContext = null");
		XModel xModel = getXModel(xContext);
		if (xModel == null) {
			throw new NullPointerException("xModel == null");
		}
		return Lo.qi(XSpreadsheetView.class, xModel.getCurrentController());
	}

	static XModel getXModel(XComponentContext xContext) {
		return Lo.qi(XModel.class, getCurrentComponent(xContext));
	}

	/**
	 * Global fuer alle Calc Documenten
	 *
	 * @param showGrid
	 */

	static void showGrid(XComponentContext xContext, boolean showGrid) {
		// Funktioniert, Aber das ist Global für alle Calc Dokumenten
		XController xController = getXModel(xContext).getCurrentController();
		XPropertySet xProp = Lo.qi(XPropertySet.class, xController);
		try {
			xProp.setPropertyValue("ShowGrid", showGrid);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
				| WrappedTargetException e) {
			// ignore weil nicht wichtig wenn nicht funktioniert
			// e.printStackTrace();
		}
	}

	/**
	 * Speichere Plugin Version im Turnier-Dokument. Nur wenn Turnier-Dokument
	 * 
	 * @param workingSpreadsheet
	 * @throws GenerateException
	 */

	public static void setDocErstelltMitVersion(WorkingSpreadsheet workingSpreadsheet) throws GenerateException {
		DocumentPropertiesHelper docPropHelper = new DocumentPropertiesHelper(workingSpreadsheet);
		TurnierSystem spielsystem = docPropHelper.getTurnierSystemAusDocument();
		// haben wir ein Turnier Dokument ?
		if (spielsystem != TurnierSystem.KEIN) {
			String pluginVersionNummer = ExtensionsHelper.from(workingSpreadsheet.getxContext()).getVersionNummer();
			docPropHelper.setStringProperty(BasePropertiesSpalte.KONFIG_PROP_ERSTELLT_MIT_VERSION, pluginVersionNummer);
			ProcessBox.from().info("Speichere Plugin-Version '" + pluginVersionNummer + "' im Dokument.");
		}
	}

}
