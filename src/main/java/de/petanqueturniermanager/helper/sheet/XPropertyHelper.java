package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.Property;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.Lo;
import de.petanqueturniermanager.helper.cellvalue.properties.CommonProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.sheet.numberformat.NumberFormatHelper;

public class XPropertyHelper implements ICommonProperties {

	private static final Logger logger = LogManager.getLogger(XPropertyHelper.class);

	private final XPropertySet xPropertySet;
	private final XSpreadsheetDocument xSpreadsheetDocument;

	private XPropertyHelper(XPropertySet xPropertySet, XSpreadsheetDocument xSpreadsheetDocument) {
		this.xPropertySet = checkNotNull(xPropertySet);
		this.xSpreadsheetDocument = xSpreadsheetDocument;
	}

	public static final XPropertyHelper from(XPropertySet xPropSet, XSpreadsheetDocument xSpreadsheetDocument) {
		checkNotNull(xPropSet);
		return new XPropertyHelper(xPropSet, xSpreadsheetDocument);
	}

	public static final XPropertyHelper from(Object hasProperties, XSpreadsheetDocument xSpreadsheetDocument) {
		checkNotNull(hasProperties);
		XPropertySet xPropSet = Lo.qi(XPropertySet.class, hasProperties);
		return new XPropertyHelper(xPropSet, xSpreadsheetDocument);
	}

	public static final XPropertyHelper from(XPropertySet xPropSet, ISheet iSheet) {
		checkNotNull(xPropSet);
		return new XPropertyHelper(xPropSet, iSheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument());
	}

	public static final XPropertyHelper from(Object hasProperties, ISheet iSheet) {
		checkNotNull(hasProperties);
		XPropertySet xPropSet = Lo.qi(XPropertySet.class, hasProperties);
		return new XPropertyHelper(xPropSet, iSheet.getWorkingSpreadsheet().getWorkingSpreadsheetDocument());
	}

	public XPropertyHelper setProperty(String key, Object val) {
		checkArgument(StringUtils.isNotEmpty(key), "key darf nicht null oder leer sein");
		try {
			xPropertySet.setPropertyValue(key, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException
				| WrappedTargetException e) {
			logger.error("Property '" + key + "' = '" + val + "'\r" + e.getMessage(), e);
		}
		return this;
	}

	public Object getProperty(String key) {
		checkArgument(StringUtils.isNotEmpty(key), "key darf nicht null oder leer sein");
		Object value = null;
		try {
			value = xPropertySet.getPropertyValue(key);
		} catch (UnknownPropertyException | WrappedTargetException e) {
			logger.error(e.getMessage(), e);
		}
		return value;
	}

	public void inpectPropertySet() {
		XPropertySetInfo propertySetInfo = xPropertySet.getPropertySetInfo();
		Property[] properties = propertySetInfo.getProperties();
		Arrays.asList(properties).stream().sorted((o1, o2) -> o1.Name.compareTo(o2.Name)).forEach((property) -> {
			logger.info(property.Name + " - " + property.Type + " - " + getProperty(property.Name));
		});
	}

	/**
	 * @param CommonProperties
	 */
	public void setProperties(CommonProperties<?> properties) {
		properties.forEach((key, value) -> {
			setProperty(key, value);
		});

		// Sonderbehandelung fuer NumberFormat
		if (properties.getUserNumberFormat() != null) {
			int idx = NumberFormatHelper.from(xSpreadsheetDocument).getIdx(properties.getUserNumberFormat());
			if (idx > -1) {
				setProperty(NUMBERFORMAT, Integer.valueOf(idx));
			}
		}
	}

}
