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
import com.sun.star.uno.UnoRuntime;

import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.cellvalue.properties.CommonProperties;
import de.petanqueturniermanager.helper.cellvalue.properties.ICommonProperties;
import de.petanqueturniermanager.helper.sheet.numberformat.NumberFormatHelper;

public class XPropertyHelper extends BaseHelper implements ICommonProperties {

	private static final Logger logger = LogManager.getLogger(XPropertyHelper.class);

	private final XPropertySet xPropertySet;

	private XPropertyHelper(XPropertySet xPropertySet, ISheet iSheet) {
		super(iSheet);
		this.xPropertySet = checkNotNull(xPropertySet);
	}

	public static final XPropertyHelper from(XPropertySet xPropSet, ISheet iSheet) {
		checkNotNull(xPropSet);
		return new XPropertyHelper(xPropSet, iSheet);
	}

	public static final XPropertyHelper from(Object hasProperties, ISheet iSheet) {
		checkNotNull(hasProperties);
		XPropertySet xPropSet = UnoRuntime.queryInterface(XPropertySet.class, hasProperties);
		return new XPropertyHelper(xPropSet, iSheet);
	}

	public XPropertyHelper setProperty(String key, Object val) {
		checkArgument(StringUtils.isNotEmpty(key), "key darf nicht null oder leer sein");
		try {
			xPropertySet.setPropertyValue(key, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
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
			System.out.println(property.Name + " - " + property.Type + " - " + getProperty(property.Name));
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
			int idx = NumberFormatHelper.from(getISheet()).getIdx(properties.getUserNumberFormat());
			if (idx > -1) {
				setProperty(NUMBERFORMAT, Integer.valueOf(idx));
			}
		}
	}

}
