/**
* Erstellung : 24.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
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
import com.sun.star.uno.Any;
import com.sun.star.uno.Type;
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
	 * @param propName
	 * @param val int val wird als String gespeichert
	 */
	public void setStringProperty(String propName, String val) {
		insertStringPropertyIfNotExist(propName, val);
		XPropertySet propSet = getXPropertySet();
		try {
			propSet.setPropertyValue(propName, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error(e);
		}
	}

	/**
	 * https://api.libreoffice.org/docs/idl/ref/namespacecom_1_1sun_1_1star_1_1beans_1_1PropertyAttribute.html#a04101331ecfe0e8dc89c54c7e557c07f
	 *
	 * @param name
	 * @param val
	 */
	private boolean insertStringPropertyIfNotExist(String name, String val) {
		checkNotNull(name);
		checkNotNull(val);

		boolean didExist = false;
		XPropertyContainer xpc = getXPropertyContainer();

		// userProperties.addProperty(property, com.sun.star.beans.PropertyAttribute.REMOVEABLE,
		// new Any(Type.STRING, value));
		// const short BOUND = 2
		// indicates that a PropertyChangeEvent will be fired to all registered
		// XPropertyChangeListeners whenever the value of this property changes.
		// PropertyAttribute.READONLY hat kein auswirkung ?
		// PropertyAttribute.READONLY

		try {
			xpc.addProperty(name, (short) 0, new Any(Type.STRING, val));
		} catch (PropertyExistException e) {
			didExist = true;
		} catch (IllegalTypeException | IllegalArgumentException e) {
			logger.error(e.getMessage(), e);
		}
		return didExist;
	}

	private XPropertyContainer getXPropertyContainer() {
		XDocumentPropertiesSupplier xps = UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, workingSpreadsheet.getWorkingSpreadsheetDocument());
		XDocumentProperties xp = xps.getDocumentProperties();
		return xp.getUserDefinedProperties();
	}

	// TODO
	// private void addListners() {
	// XPropertySet propSet = getXPropertySet();
	// // BOUND
	// propSet.addPropertyChangeListener(arg0, arg1);
	// propSet.addVetoableChangeListener(arg0, arg1);
	// }

	private XPropertySet getXPropertySet() {
		return UnoRuntime.queryInterface(XPropertySet.class, getXPropertyContainer());
	}

	/**
	 * @param propName = name vom property
	 * @return default when not found
	 */
	public String getStringProperty(String propName, boolean ignoreNotFound, String defaultVal) {
		XPropertySet propSet = getXPropertySet();
		Object propVal = null;
		try {
			propVal = propSet.getPropertyValue(propName);
		} catch (UnknownPropertyException | WrappedTargetException e) {
			if (!ignoreNotFound) {
				logger.error(e.getMessage(), e);
			}
		}

		if (propVal != null && propVal instanceof String) {
			return (String) propVal;
		}
		return defaultVal;
	}

	/**
	 * @param propName = name vom property
	 * @return -1 when not found
	 */
	public int getIntProperty(String propName, int defaultVal) {
		String stringProperty = getStringProperty(propName, true, "" + defaultVal);
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

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public boolean getBooleanProperty(String propName, Boolean defaultVal) {
		String stringProperty = getStringProperty(propName, true, booleanToString(defaultVal));
		return stringToBoolean(stringProperty);
	}

	/**
	 * @param key
	 * @param defaultVal
	 * @return
	 */
	public void setBooleanProperty(String propName, Boolean newVal) {
		setStringProperty(propName, booleanToString(newVal));
	}

	private String booleanToString(boolean booleanProp) {
		if (booleanProp) {
			return "J";
		}
		return "N";
	}

	private boolean stringToBoolean(String booleanProp) {
		if (StringUtils.isBlank(booleanProp) || StringUtils.containsIgnoreCase(booleanProp, "N")) {
			return false;
		}
		return true;
	}

}
