/**
 * Erstellung 22.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.XMultiPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.newrelease.ExtensionsHelper;
import de.petanqueturniermanager.helper.sheet.WeakRefHelper;
import de.petanqueturniermanager.sidebar.GuiFactory;
import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public abstract class BaseField<T> {

	static final Logger logger = LogManager.getLogger(BaseField.class);

	private WeakRefHelper<GuiFactoryCreateParam> guiFactoryCreateParamWkRef;
	private Layout hLayout;
	private XMultiPropertySet properties;
	private final String imageUrlDir;

	// wird nur intial verwendet, dan bei jeden resize von layout neu berechnet
	public static final int lineHeight = 29;
	private static final int lineWidth = 10;
	protected static final Rectangle BASE_RECTANGLE = new Rectangle(0, 0, lineWidth, lineHeight);

	protected BaseField(GuiFactoryCreateParam guiFactoryCreateParam) {
		this.guiFactoryCreateParamWkRef = new WeakRefHelper<>(guiFactoryCreateParam);
		hLayout = new HorizontalLayout();
		imageUrlDir = ExtensionsHelper.from(guiFactoryCreateParam.getContext()).getImageUrlDir();
		doCreate();
	}

	protected final void setProperties(XMultiPropertySet properties) {
		this.properties = properties;
	}

	protected abstract void doCreate();

	protected final XMultiComponentFactory getxMCF() {
		return guiFactoryCreateParamWkRef.get().getxMCF();
	}

	protected final XComponentContext getxContext() {
		return guiFactoryCreateParamWkRef.get().getContext();
	}

	protected final XToolkit getToolkit() {
		return guiFactoryCreateParamWkRef.get().getToolkit();
	}

	protected final XWindowPeer getWindowPeer() {
		return guiFactoryCreateParamWkRef.get().getWindowPeer();
	}

	/**
	 * @return the hLayout
	 */
	public final Layout getLayout() {
		return hLayout;
	}

	protected final void setGuiFactoryCreateParam(GuiFactoryCreateParam guiFactoryCreateParam) {
		if (guiFactoryCreateParam == null) {
			guiFactoryCreateParamWkRef = null;
		} else {
			this.guiFactoryCreateParamWkRef = new WeakRefHelper<>(guiFactoryCreateParam);
		}
	}

	protected final GuiFactoryCreateParam getGuiFactoryCreateParam() {
		return guiFactoryCreateParamWkRef.get();
	}

	@SuppressWarnings("unchecked")
	public T helpText(String text) {
		setProperty(GuiFactory.HELP_TEXT, text);
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T readOnly(boolean readOnly) {
		if (readOnly) {
			setProperty(GuiFactory.READ_ONLY, true);
			setProperty(GuiFactory.ENABLED, false);
		} else {
			setProperty(GuiFactory.READ_ONLY, false);
			setProperty(GuiFactory.ENABLED, true);
		}
		return (T) this;
	}

	@SuppressWarnings("unchecked")
	public T helpText(XMultiPropertySet xMultiPropertySet, String text) {
		setProperty(xMultiPropertySet, GuiFactory.HELP_TEXT, text);
		return (T) this;
	}

	public final T setProperty(String key, Object newVal) {
		return setProperty(properties, key, newVal);
	}

	@SuppressWarnings("unchecked")
	public final T setProperty(XMultiPropertySet xMultiPropertySet, String key, Object newVal) {
		if (properties != null && key != null) {
			String[] name = new String[] { key };
			Object[] val = new Object[] { newVal };
			try {
				xMultiPropertySet.setPropertyValues(name, val);
			} catch (IllegalArgumentException | PropertyVetoException | WrappedTargetException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return (T) this;
	}

	protected void disposing() {
		hLayout = new HorizontalLayout();
		properties = null;
	}

	protected final String getImageUrlDir() {
		return imageUrlDir;
	}

}
