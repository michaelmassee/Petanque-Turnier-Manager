/**
* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.PropertyExistException;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertyContainer;
import com.sun.star.beans.XPropertySet;
import com.sun.star.document.XDocumentProperties;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.msgbox.InfoModal;
import de.petanqueturniermanager.helper.sheet.DocumentHelper;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;

/**
 * http://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XDocumentProperties.html <br>
 * https://forum.openoffice.org/en/forum/viewtopic.php?t=33455 <br>
 *
 */

public class DocumentPropertiesHelper {
	private static final Logger logger = LogManager.getLogger(DocumentPropertiesHelper.class);

	public static final String PROP_NAME_FORMATION = "formation";

	final XComponentContext xContext;

	public DocumentPropertiesHelper(XComponentContext xContext) {
		this.xContext = xContext;
		initDefault();
	}

	private void initDefault() {
		insertIntPropertyIfNotExist(PROP_NAME_FORMATION, Formation.SUPERMELEE.getId());
	}

	public void showProperties() {
		InfoModal msgBox = new InfoModal(this.xContext);
		String propertiesInfo = PROP_NAME_FORMATION + ": " + getIntProperty(PROP_NAME_FORMATION);
		msgBox.show("Konfiguration", propertiesInfo);
	}

	public void insertIntPropertyIfNotExist(String name, int val) {
		XPropertyContainer xpc = getXPropertyContainer();
		try {
			xpc.addProperty(name, (short) 0, "" + val); // Achtung: immer als String speichern, weil Integer nicht
														// funktioniert
		} catch (PropertyExistException e) {
			// Ignorieren
		} catch (IllegalTypeException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private XPropertyContainer getXPropertyContainer() {
		XDocumentPropertiesSupplier xps = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class,
				DocumentHelper.getCurrentSpreadsheetDocument(this.xContext));
		XDocumentProperties xp = xps.getDocumentProperties();
		return xp.getUserDefinedProperties();
	}

	private XPropertySet getXPropertySet() {
		return UnoRuntime.queryInterface(XPropertySet.class, getXPropertyContainer());
	}

	/**
	 * @param propName = name vom property
	 * @return -1 when not found
	 */
	public int getIntProperty(String propName) {
		XPropertySet propSet = getXPropertySet();
		Object propVal = null;
		try {
			propVal = propSet.getPropertyValue(propName);
		} catch (UnknownPropertyException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}

		if (propVal != null && propVal instanceof String) {
			return NumberUtils.toInt((String) propVal, -1);
		}
		return -1;
	}

	/**
	 * @param propName
	 * @param val int val wird als String gespeichert
	 */
	public void setIntProperty(String propName, int val) {
		XPropertySet propSet = getXPropertySet();
		try {
			propSet.setPropertyValue(propName, "" + val);
		} catch (UnknownPropertyException | WrappedTargetException | IllegalArgumentException
				| PropertyVetoException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
