/**
* Erstellung : 02.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

import org.apache.logging.log4j.Logger;

import com.sun.star.awt.XMessageBoxFactory;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.sheet.DocumentHelper;

public abstract class AbstractMessageBox {

	final XComponentContext m_xContext;

	public AbstractMessageBox(XComponentContext m_xContext) {
		this.m_xContext = m_xContext;
	}

	protected XWindowPeer getWindowPeer() {
		XWindow xParent = DocumentHelper.getCurrentFrame(this.m_xContext).getContainerWindow();
		return UnoRuntime.queryInterface(XWindowPeer.class, xParent);
	}

	protected XMessageBoxFactory getXMessageBoxFactory() {
		XMessageBoxFactory xMessageBoxFactory = null;

		XToolkit xKit;
		try {
			// get access to the office toolkit environment
			xKit = UnoRuntime.queryInterface(XToolkit.class, this.m_xContext.getServiceManager()
					.createInstanceWithContext("com.sun.star.awt.Toolkit", this.m_xContext));
			xMessageBoxFactory = UnoRuntime.queryInterface(XMessageBoxFactory.class, xKit);
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		}

		// finally {
		// // make sure always to dispose the component and free the memory!
		// if (xComponent != null) {
		// xComponent.dispose();
		// }
		// }

		return xMessageBoxFactory;
	}

	protected abstract Logger getLogger();

}