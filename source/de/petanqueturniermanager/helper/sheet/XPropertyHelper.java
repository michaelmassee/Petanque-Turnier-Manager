package de.petanqueturniermanager.helper.sheet;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.assertj.core.util.Arrays;

import com.sun.star.beans.Property;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.beans.XPropertySetInfo;
import com.sun.star.lang.WrappedTargetException;

public class XPropertyHelper {

	private static final Logger logger = LogManager.getLogger(XPropertyHelper.class);

	private final XPropertySet xPropertySet;

	private XPropertyHelper(XPropertySet xPropertySet) {
		this.xPropertySet = xPropertySet;
	}

	public static final XPropertyHelper from(XPropertySet xPropSet) {
		checkNotNull(xPropSet);
		return new XPropertyHelper(xPropSet);
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
		Arrays.asList(properties).forEach((property) -> {
			System.out.println(((Property) property).Name);
			System.out.println(((Property) property).Type);
		});
	}

}
