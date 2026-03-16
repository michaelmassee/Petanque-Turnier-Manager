/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import static com.google.common.base.Preconditions.checkNotNull;

import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

/**
 * @author Michael Massee
 *
 */
public class GuiFactoryCreateParam {

	private XMultiComponentFactory xMCF;
	private XComponentContext xContext;
	private XToolkit toolkit;
	private XWindowPeer windowPeer;

	public GuiFactoryCreateParam(XMultiComponentFactory xMCF, XComponentContext xContext, XToolkit toolkit, XWindowPeer windowPeer) {
		this.xMCF = checkNotNull(xMCF);
		this.xContext = checkNotNull(xContext);
		this.toolkit = checkNotNull(toolkit);
		this.windowPeer = checkNotNull(windowPeer);
	}

	public final XMultiComponentFactory getxMCF() {
		return xMCF;
	}

	public final XComponentContext getContext() {
		return xContext;
	}

	public final XToolkit getToolkit() {
		return toolkit;
	}

	public final XWindowPeer getWindowPeer() {
		return windowPeer;
	}

	public final void clear() {
		xMCF = null;
		xContext = null;
		toolkit = null;
		windowPeer = null;
	}

}
