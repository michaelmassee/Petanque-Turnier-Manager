/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.DocumentHelper;

public class CellStyleHelper {

	private final AbstractCellStyleDef cellStyleDef;
	private final ISheet sheet;

	private CellStyleHelper(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		this.sheet = sheet;
		this.cellStyleDef = cellStyleDef;
	}

	public static CellStyleHelper from(ISheet sheet, AbstractCellStyleDef cellStyleDef) {
		checkNotNull(sheet);
		checkNotNull(cellStyleDef);
		return new CellStyleHelper(sheet, cellStyleDef);
	}

	public CellStyleHelper apply() throws GenerateException {
		checkNotNull(this.sheet);
		checkNotNull(this.cellStyleDef);

		String styleName = this.cellStyleDef.getName();

		try {
			XSpreadsheetDocument currentSpreadsheetDocument = DocumentHelper.getCurrentSpreadsheetDocument(this.sheet.getxContext());

			XStyleFamiliesSupplier xFamiliesSupplier = UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, currentSpreadsheetDocument);
			XNameAccess xFamiliesNA = xFamiliesSupplier.getStyleFamilies();
			Object aCellStylesObj = xFamiliesNA.getByName("CellStyles");
			XNameContainer xCellStylesNA = UnoRuntime.queryInterface(XNameContainer.class, aCellStylesObj);

			Object aCellStyle = null;
			try {
				aCellStyle = xCellStylesNA.getByName(styleName);
			} catch (NoSuchElementException e) {
				// create a new cell style
				XMultiServiceFactory xDocServiceManager = UnoRuntime.queryInterface(XMultiServiceFactory.class, currentSpreadsheetDocument);
				aCellStyle = xDocServiceManager.createInstance("com.sun.star.style.CellStyle");
				xCellStylesNA.insertByName(styleName, aCellStyle);
			}

			// modify properties of the (new) style
			XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, aCellStyle);
			for (String propKey : this.cellStyleDef.getCellProperties().keySet()) {
				xPropSet.setPropertyValue(propKey, this.cellStyleDef.getCellProperties().get(propKey));
			}
		} catch (Exception e) {
			this.sheet.getLogger().error(e.getMessage(), e);
		}
		return this;
	}

}
