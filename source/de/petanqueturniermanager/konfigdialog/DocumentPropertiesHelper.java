/**
* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfigdialog;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.IllegalTypeException;
import com.sun.star.beans.PropertyAttribute;
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

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * http://www.openoffice.org/api/docs/common/ref/com/sun/star/document/XDocumentProperties.html <br>
 * https://forum.openoffice.org/en/forum/viewtopic.php?t=33455 <br>
 *
 */

public class DocumentPropertiesHelper {
	private static final Logger logger = LogManager.getLogger(DocumentPropertiesHelper.class);

	// public static final String PROP_NAME_FORMATION = "formation";

	final WorkingSpreadsheet workingSpreadsheet;

	public DocumentPropertiesHelper(WorkingSpreadsheet currentSpreadsheet) {
		workingSpreadsheet = checkNotNull(currentSpreadsheet);
	}

	/**
	 * @param propTurnierSpieltag1Info
	 * @param string
	 */
	public void insertStringPropertyIfNotExist(String name, String val) {
		XPropertyContainer xpc = getXPropertyContainer();
		try {
			xpc.addProperty(name, PropertyAttribute.OPTIONAL, val); // PropertyAttribute.READONLY
		} catch (PropertyExistException e) {
			// Ignorieren
		} catch (IllegalTypeException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void showProperties() {
		// InfoModal msgBox = new InfoModal(this.xContext);
		// String propertiesInfo = PROP_NAME_FORMATION + ": " + getIntProperty(PROP_NAME_FORMATION);
		// msgBox.show("Konfiguration", propertiesInfo);
	}

	public void insertIntPropertyIfNotExist(String name, int val) {
		insertStringPropertyIfNotExist(name, "" + val);
	}

	private XPropertyContainer getXPropertyContainer() {
		XDocumentPropertiesSupplier xps = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, workingSpreadsheet.getWorkingSpreadsheetDocument());
		XDocumentProperties xp = xps.getDocumentProperties();
		return xp.getUserDefinedProperties();
	}

	private XPropertySet getXPropertySet() {
		return UnoRuntime.queryInterface(XPropertySet.class, getXPropertyContainer());
	}

	/**
	 * @param propName = name vom property
	 * @return null when not found
	 */
	public String getStringProperty(String propName) {
		XPropertySet propSet = getXPropertySet();
		Object propVal = null;
		try {
			propVal = propSet.getPropertyValue(propName);
		} catch (UnknownPropertyException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}

		if (propVal != null && propVal instanceof String) {
			return (String) propVal;
		}
		return null;
	}

	/**
	 * @param propName
	 * @param val int val wird als String gespeichert
	 */
	public void setStringProperty(String propName, String val) {
		XPropertySet propSet = getXPropertySet();
		try {
			propSet.setPropertyValue(propName, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error(e);
		}
	}

	/**
	 * @param propName = name vom property
	 * @return -1 when not found
	 */
	public int getIntProperty(String propName) {
		String stringProperty = getStringProperty(propName);
		if (stringProperty != null) {
			return NumberUtils.toInt(stringProperty, -1);
		}
		return -1;
	}

	/**
	 * @param propName
	 * @param val int val wird als String gespeichert
	 */
	public void setIntProperty(String propName, int val) {
		setStringProperty(propName, "" + val);
	}
}
