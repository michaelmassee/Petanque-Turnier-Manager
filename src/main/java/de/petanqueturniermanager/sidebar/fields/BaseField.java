/**
 * Erstellung 22.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.fields;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.sidebar.GuiFactoryCreateParam;
import de.petanqueturniermanager.sidebar.layout.HorizontalLayout;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public abstract class BaseField {

	private GuiFactoryCreateParam guiFactoryCreateParam;
	private final Layout hLayout;

	protected BaseField(GuiFactoryCreateParam guiFactoryCreateParam) {
		this.guiFactoryCreateParam = checkNotNull(guiFactoryCreateParam);
		hLayout = new HorizontalLayout();
		doCreate();
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

}
