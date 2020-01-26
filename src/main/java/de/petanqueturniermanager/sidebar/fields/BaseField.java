/**
 * Erstellung 22.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import static com.google.common.base.Preconditions.checkNotNull;

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

	private GuiFactoryCreateParam guiFactoryCreateParam;
	private Layout hLayout;
	private XMultiPropertySet properties;
	private final String imageUrlDir;

	// wird nur intial verwendet, dan bei jeden resize von layout neu berechnet
	private static final int lineHeight = 25;
	private static final int lineWidth = 10;
	protected static final Rectangle BASE_RECTANGLE = new Rectangle(0, 0, lineWidth, lineHeight);

	protected BaseField(GuiFactoryCreateParam guiFactoryCreateParam) {
		this.guiFactoryCreateParam = checkNotNull(guiFactoryCreateParam);
		hLayout = new HorizontalLayout();
		imageUrlDir = ExtensionsHelper.from(guiFactoryCreateParam.getContext()).getImageUrlDir();
		doCreate();
	}

	protected final void setProperties(XMultiPropertySet properties) {
		this.properties = properties;
	}

	protected abstract void doCreate();

	protected final XMultiComponentFactory getxMCF() {
		return guiFactoryCreateParam.getxMCF();
	}

	protected final XComponentContext getxContext() {
		return guiFactoryCreateParam.getContext();
	}

	protected final XToolkit getToolkit() {
		return guiFactoryCreateParam.getToolkit();
	}

	protected final XWindowPeer getWindowPeer() {
		return guiFactoryCreateParam.getWindowPeer();
	}

	/**
	 * @return the hLayout
	 */
	public final Layout getLayout() {
		return hLayout;
	}

	protected final void setGuiFactoryCreateParam(GuiFactoryCreateParam guiFactoryCreateParam) {
		this.guiFactoryCreateParam = guiFactoryCreateParam;
	}

	protected final GuiFactoryCreateParam getGuiFactoryCreateParam() {
		return guiFactoryCreateParam;
	}

	@SuppressWarnings("unchecked")
	public final T helpText(String text) {
		if (properties != null && text != null) {
			String[] name = new String[] { GuiFactory.HELP_TEXT };
			String[] val = new String[] { text };
			try {
				properties.setPropertyValues(name, val);
			} catch (IllegalArgumentException | PropertyVetoException | WrappedTargetException e) {
				logger.error(e.getMessage(), e);
			}
		}
		return (T) this;
	}

	protected void disposing() {
		guiFactoryCreateParam = null;
		hLayout = null;
		properties = null;
	}

	protected final String getImageUrlDir() {
		return imageUrlDir;
	}

}
