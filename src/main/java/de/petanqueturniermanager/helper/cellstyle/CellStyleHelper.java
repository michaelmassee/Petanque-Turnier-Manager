/**
 * Erstellung : 21.05.2018 / Michael Massee
 **/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.Exception;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

public class CellStyleHelper {

	private static final Logger logger = LogManager.getLogger(CellStyleHelper.class);

	private final AbstractCellStyleDef cellStyleDef;
	/** Gesetzt wenn über {@link #from(ISheet, AbstractCellStyleDef)} erzeugt. */
	private final ISheet sheet;
	/** Gesetzt wenn über {@link #from(XSpreadsheetDocument, AbstractCellStyleDef)} erzeugt. */
	private final XSpreadsheetDocument spreadsheetDocument;

	private CellStyleHelper(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		this.sheet = sheet;
		this.spreadsheetDocument = null;
		this.cellStyleDef = cellStyleDef;
	}

	private CellStyleHelper(XSpreadsheetDocument spreadsheetDocument, AbstractCellStyleDef cellStyleDef) {
		this.sheet = null;
		this.spreadsheetDocument = spreadsheetDocument;
		this.cellStyleDef = cellStyleDef;
	}

	public static CellStyleHelper from(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		checkNotNull(sheet);
		checkNotNull(cellStyleDef);
		return new CellStyleHelper(sheet, cellStyleDef);
	}

	/**
	 * Erstellt einen CellStyleHelper direkt über das Dokument – für Kontexte ohne {@link ISheet}
	 * (z. B. {@code BlattschutzManager}).
	 */
	public static CellStyleHelper from(XSpreadsheetDocument doc, AbstractCellStyleDef cellStyleDef) {
		checkNotNull(doc);
		checkNotNull(cellStyleDef);
		return new CellStyleHelper(doc, cellStyleDef);
	}

	public CellStyleHelper apply() {
		checkNotNull(cellStyleDef);

		var currentSpreadsheetDocument = holeSpreadsheetDocument();
		checkNotNull(currentSpreadsheetDocument);
		applyAufDokument(currentSpreadsheetDocument);
		return this;
	}

	// -------------------------------------------------------------------------

	private XSpreadsheetDocument holeSpreadsheetDocument() {
		if (spreadsheetDocument != null) {
			return spreadsheetDocument;
		}
		checkNotNull(sheet);
		return sheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument();
	}

	private void applyAufDokument(XSpreadsheetDocument currentSpreadsheetDocument) {
		var styleName = cellStyleDef.getName();

		try {
			var xFamiliesSupplier = Lo.qi(XStyleFamiliesSupplier.class, currentSpreadsheetDocument);
			var xFamiliesNA = xFamiliesSupplier.getStyleFamilies();
			var aCellStylesObj = xFamiliesNA.getByName("CellStyles");
			var xCellStylesNA = Lo.qi(XNameContainer.class, aCellStylesObj);

			Object aCellStyle;
			try {
				aCellStyle = xCellStylesNA.getByName(styleName);
			} catch (NoSuchElementException e) {
				// create a new cell style
				var xDocServiceManager = Lo.qi(XMultiServiceFactory.class, currentSpreadsheetDocument);
				aCellStyle = xDocServiceManager.createInstance("com.sun.star.style.CellStyle");
				xCellStylesNA.insertByName(styleName, aCellStyle);
			}

			// modify properties of the (new) style
			var xPropSet = Lo.qi(XPropertySet.class, aCellStyle);
			for (var propKey : cellStyleDef.getCellProperties().keySet()) {
				if (xPropSet.getPropertySetInfo().hasPropertyByName(propKey)) {
					var neuerWert = cellStyleDef.getCellProperties().get(propKey);
					var aktuellerWert = xPropSet.getPropertyValue(propKey);
					if (!Objects.equals(neuerWert, aktuellerWert)) {
						xPropSet.setPropertyValue(propKey, neuerWert);
					}
				}
			}
		} catch (RuntimeException e) {
			if (SheetHelper.istIrgendeinSheetGeschuetzt(currentSpreadsheetDocument)) {
				getLogger().warn(
						"Zellstil '{}' konnte nicht gesetzt werden – LO-Einschränkung: " +
						"Zellstile können nicht geändert werden, solange ein Sheet im Dokument " +
						"tab-geschützt ist. (sc/source/ui/unoobj/styleuno.cxx)",
						styleName);
			} else {
				getLogger().error(e.getMessage(), e);
			}
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		}
	}

	private Logger getLogger() {
		if (sheet != null) {
			return sheet.getLogger();
		}
		return logger;
	}

}
