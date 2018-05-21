/**
* Erstellung : 21.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.cellstyle;

import static com.google.common.base.Preconditions.*;

import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
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
		return new CellStyleHelper(sheet, cellStyleDef);
	}

	public CellStyleHelper apply() throws GenerateException {
		checkNotNull(this.sheet);
		checkNotNull(this.cellStyleDef);

		String styleName = this.cellStyleDef.getName();

		try {
			XSpreadsheetDocument currentSpreadsheetDocument = DocumentHelper.getCurrentSpreadsheetDocument(this.sheet.getxContext());

			XStyleFamiliesSupplier xFamiliesSupplier = UnoRuntime.queryInterface(com.sun.star.style.XStyleFamiliesSupplier.class, currentSpreadsheetDocument);
			com.sun.star.container.XNameAccess xFamiliesNA = xFamiliesSupplier.getStyleFamilies();
			Object aCellStylesObj;
			aCellStylesObj = xFamiliesNA.getByName("CellStyles");
			com.sun.star.container.XNameContainer xCellStylesNA = UnoRuntime.queryInterface(com.sun.star.container.XNameContainer.class, aCellStylesObj);

			Object aCellStyle = null;
			try {
				aCellStyle = xCellStylesNA.getByName(styleName);
			} catch (NoSuchElementException e) {
				// create a new cell style
				XMultiServiceFactory xDocServiceManager = UnoRuntime.queryInterface(com.sun.star.lang.XMultiServiceFactory.class, currentSpreadsheetDocument);
				aCellStyle = xDocServiceManager.createInstance("com.sun.star.style.CellStyle");
				String aStyleName = styleName;
				xCellStylesNA.insertByName(aStyleName, aCellStyle);
			}

			// modify properties of the (new) style
			XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, aCellStyle);
			for (String propKey : this.cellStyleDef.getCellProperties().keySet()) {
				xPropSet.setPropertyValue(propKey, this.cellStyleDef.getCellProperties().get(propKey));
			}
			// xPropSet.setPropertyValue("CellBackColor", geradeColor);
			// xPropSet.setPropertyValue("IsCellBackgroundTransparent", Boolean.FALSE);
		} catch (Exception e) {
			this.sheet.getLogger().error(e.getMessage(), e);
		}
		return this;
	}

}
