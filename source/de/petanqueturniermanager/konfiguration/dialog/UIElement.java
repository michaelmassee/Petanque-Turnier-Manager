/**
 * Erstellung 11.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
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
public abstract class UIElement<T> {
	private static final Logger logger = LogManager.getLogger(UIElement.class);

	private final String PROP_POSX = "PositionX";
	private final String PROP_POSY = "PositionY";
	private final String PROP_WIDTH = "Width";
	private final String PROP_HEIGHT = "Height";

	final XPropertySet xPropertySet;

	protected UIElement(XMultiServiceFactory xMultiServiceFactory) throws Exception {
		checkNotNull(xMultiServiceFactory);
		Object model = xMultiServiceFactory.createInstance(getModelClassName());
		this.xPropertySet = UnoRuntime.queryInterface(XPropertySet.class, checkNotNull(model));
	}

	abstract String getModelClassName();

	public final T posX(int posX) {
		setProperty(PROP_POSX, Integer.valueOf(posX));
		return (T) this;
	}

	public final void insert() {
		// xNameCont.insertByName(fieldname, labelModel);
	}

	public final T setProperty(String name, Object val) {
		try {
			xPropertySet.setPropertyValue(name, val);
		} catch (IllegalArgumentException | UnknownPropertyException | PropertyVetoException | WrappedTargetException e) {
			logger.error(e);
		}
		return (T) this;
	}

}
