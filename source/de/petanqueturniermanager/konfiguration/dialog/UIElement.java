/**
 * Erstellung 11.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XControlContainer;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.ElementExistException;
import com.sun.star.container.XNameContainer;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

/**
 * @author Michael Massee
 *
 */
@SuppressWarnings("unchecked")
public abstract class UIElement<T, I> {
	private static final Logger logger = LogManager.getLogger(UIElement.class);

	private final String PROP_POSX = "PositionX";
	private final String PROP_POSY = "PositionY";
	private final String PROP_WIDTH = "Width";
	private final String PROP_HEIGHT = "Height";
	private final String PROP_NAME = "Name";
	private final String PROP_TABINDEX = "TabIndex";

	private String fieldname;

	private boolean didInsert = false;

	private final XPropertySet xPropertySet;
	private final XNameContainer xNameCont;
	private final Object model;
	private XControlContainer xControlCont;

	protected UIElement(Object dialogModel) throws Exception {
		checkNotNull(dialogModel);
		XMultiServiceFactory xMultiServiceFactory = UnoRuntime.queryInterface(XMultiServiceFactory.class, dialogModel);
		checkNotNull(xMultiServiceFactory);
		this.model = checkNotNull(xMultiServiceFactory.createInstance(getModelClassName()));
		this.xPropertySet = checkNotNull(UnoRuntime.queryInterface(XPropertySet.class, checkNotNull(model)));
		this.xNameCont = checkNotNull(UnoRuntime.queryInterface(XNameContainer.class, dialogModel));

	}

	public I doInsert(XControlContainer xControlCont) {
		checkNotNull(getFieldname());
		this.xControlCont = checkNotNull(xControlCont);
		try {
			xNameCont.insertByName(getFieldname(), model);
			didInsert = true;
		} catch (IllegalArgumentException | ElementExistException | WrappedTargetException e) {
			logger.error(e);
		}
		return null;
	}

	abstract String getModelClassName();

	public final T name(String fieldname) {
		StringUtils.isEmpty(fieldname);
		this.fieldname = fieldname;
		setProperty(PROP_NAME, fieldname);
		return (T) this;
	}

	public final T posX(int posX) {
		setProperty(PROP_POSX, Integer.valueOf(posX));
		return (T) this;
	}

	public final T posY(int posY) {
		setProperty(PROP_POSY, Integer.valueOf(posY));
		return (T) this;
	}

	public final T width(int width) {
		setProperty(PROP_WIDTH, Integer.valueOf(width));
		return (T) this;
	}

	public final T height(int height) {
		setProperty(PROP_HEIGHT, Integer.valueOf(height));
		return (T) this;
	}

	public final T TabIndex(int tabIndex) {
		setProperty(PROP_TABINDEX, Short.valueOf((short) tabIndex));
		return (T) this;
	}

	public final T setProperty(String name, Object val) {
		if (didInsert) {
			logger.error("Property " + name + " not Set. UI-Element " + getFieldname() + " bereits eingefuegt.");
			return (T) this;
		}

		try {
			xPropertySet.setPropertyValue(name, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error(e);
		}
		return (T) this;
	}

	/**
	 * @return the xControlCont
	 */
	protected XControlContainer getxControlCont() {
		return xControlCont;
	}

	protected String getFieldname() {
		return fieldname;
	}

}
