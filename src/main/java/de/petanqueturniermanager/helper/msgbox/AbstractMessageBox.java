/**
* Erstellung : 02.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XMessageBoxFactory;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;

public abstract class AbstractMessageBox {

	final XComponentContext xContext;

	public AbstractMessageBox(XComponentContext m_xContext) {
		this.xContext = checkNotNull(m_xContext);
	}

	protected XWindowPeer getWindowPeer() {
		XWindow xParent = DocumentHelper.getCurrentFrame(this.xContext).getContainerWindow();
		return UnoRuntime.queryInterface(XWindowPeer.class, xParent);
	}

	protected XMessageBoxFactory getXMessageBoxFactory() {
		XMessageBoxFactory xMessageBoxFactory = null;

		XToolkit xKit;
		try {
			// get access to the office toolkit environment
			xKit = UnoRuntime.queryInterface(XToolkit.class, this.xContext.getServiceManager().createInstanceWithContext("com.sun.star.awt.Toolkit", this.xContext));
			xMessageBoxFactory = UnoRuntime.queryInterface(XMessageBoxFactory.class, xKit);
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		}
		return xMessageBoxFactory;
	}

	protected abstract Logger getLogger();

}