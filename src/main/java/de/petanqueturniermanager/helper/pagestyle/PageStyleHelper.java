package de.petanqueturniermanager.helper.pagestyle;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XNameAccess;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.sheet.XSpreadsheetDocument;
import com.sun.star.style.XStyleFamiliesSupplier;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.BaseHelper;
import de.petanqueturniermanager.helper.sheet.XPropertyHelper;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Zwischensumme auf jeder Seite in der Fußzeile<br>
 * http://www.oooforum.de/viewtopic.php?t=12916<br>
 * https://www.openoffice.org/api/docs/common/ref/com/sun/star/style/PageProperties.html<br>
 * https://api.libreoffice.org/docs/idl/ref/servicecom_1_1sun_1_1star_1_1sheet_1_1TablePageStyle.html<br>
 *
 * @author michael
 *
 */

public class PageStyleHelper extends BaseHelper {

	private static final Logger logger = LogManager.getLogger(PageStyleHelper.class);

	private final PageStyleDef pageStyleDef;

	private PageStyleHelper(ISheet iSheet, PageStyleDef pageStyleDef) {
		super(iSheet);
		this.pageStyleDef = checkNotNull(pageStyleDef);
	}

	public static PageStyleHelper from(ISheet iSheet, SpielTagNr spielTag) {
		return new PageStyleHelper(iSheet, new PageStyleDef(spielTag));
	}

	public static PageStyleHelper from(ISheet iSheet, String pageStyleName) {
		return new PageStyleHelper(iSheet, new PageStyleDef(pageStyleName, new PageProperties()));
	}

	public static PageStyleHelper from(ISheet iSheet, PageStyleDef pageStyleDef) {
		return new PageStyleHelper(iSheet, pageStyleDef);
	}

	public static PageStyleHelper from(ISheet iSheet, PageStyle pageStyle) {
		return PageStyleHelper.from(iSheet, pageStyle.getName());
	}

	/**
	 * eigene Footer für Pétanque-Turnier-Manager
	 *
	 * @return
	 */
	public PageStyleHelper initDefaultFooter() {
		pageStyleDef.setFooterRight("* Pétanque-Turnier-Manager *\r\nmichael@massee.de");
		return this;
	}

	public PageStyleHelper create() {
		checkNotNull(pageStyleDef);
		String styleName = pageStyleDef.getPageStyleName();

		try {
			XSpreadsheetDocument currentSpreadsheetDocument = getWorkingSpreadsheetDocument();

			XStyleFamiliesSupplier xFamiliesSupplier = UnoRuntime.queryInterface(XStyleFamiliesSupplier.class, currentSpreadsheetDocument);
			XNameAccess xFamiliesNA = xFamiliesSupplier.getStyleFamilies();
			Object pageStylesObj = xFamiliesNA.getByName("PageStyles");
			XNameContainer xCellStylesNA = UnoRuntime.queryInterface(XNameContainer.class, pageStylesObj);

			Object pageStyle = null;
			try {
				pageStyle = xCellStylesNA.getByName(styleName);
			} catch (NoSuchElementException e) {
				// create a new Page style
				XMultiServiceFactory xDocServiceManager = UnoRuntime.queryInterface(XMultiServiceFactory.class, currentSpreadsheetDocument);
				// TablePageStyle
				pageStyle = xDocServiceManager.createInstance("com.sun.star.style.PageStyle");
				xCellStylesNA.insertByName(styleName, pageStyle);
			}

			// modify properties of the (new) style
			XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, pageStyle);
			pageStyleDef.formatHeaderFooter(XPropertyHelper.from(xPropSet, getISheet()));

			// TODO Move this code to XPropertyHelper
			for (String propKey : pageStyleDef.getPageProperties().keySet()) {
				// Props.setProperty(props, "RightPageHeaderContent", header);
				xPropSet.setPropertyValue(propKey, pageStyleDef.getPageProperties().get(propKey));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return this;
	}

	public PageStyleHelper applytoSheet() throws GenerateException {
		XPropertySet xPropertySet = UnoRuntime.queryInterface(XPropertySet.class, getXSpreadSheet());
		try {
			xPropertySet.setPropertyValue("PageStyle", pageStyleDef.getPageStyleName());
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}
		return this;
	}

	public PageStyleHelper setFooterLeft(String string) {
		pageStyleDef.setFooterLeft(string);
		return this;
	}

	public PageStyleHelper setFooterCenter(String string) {
		pageStyleDef.setFooterCenter(string);
		return this;
	}

	public PageStyleHelper setFooterRight(String string) {
		pageStyleDef.setFooterRight(string);
		return this;
	}

	public PageStyleHelper setHeaderLeft(String string) {
		pageStyleDef.setHeaderLeft(string);
		return this;
	}

	public PageStyleHelper setHeaderCenter(String string) {
		pageStyleDef.setHeaderCenter(string);
		return this;
	}

	public PageStyleHelper setHeaderRight(String string) {
		pageStyleDef.setHeaderRight(string);
		return this;
	}

}
