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
import com.sun.star.frame.XFrame;
import com.sun.star.uno.Exception;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.comp.DocumentHelper;
import de.petanqueturniermanager.helper.Lo;

public abstract class AbstractMessageBox {

	final XComponentContext xContext;

	public AbstractMessageBox(XComponentContext m_xContext) {
		xContext = checkNotNull(m_xContext);
	}

	protected XWindowPeer getWindowPeer() {
		XFrame currentFrame = DocumentHelper.getCurrentFrame(xContext);
		if (currentFrame != null) {
			XWindow xParent = DocumentHelper.getCurrentFrame(xContext).getContainerWindow();
			return Lo.qi(XWindowPeer.class, xParent);
		}
		return null;
	}

	protected XMessageBoxFactory getXMessageBoxFactory() {
		XMessageBoxFactory xMessageBoxFactory = null;

		XToolkit xKit;
		try {
			// get access to the office toolkit environment
			xKit = Lo.qi(XToolkit.class,
					xContext.getServiceManager().createInstanceWithContext("com.sun.star.awt.Toolkit", xContext));
			xMessageBoxFactory = Lo.qi(XMessageBoxFactory.class, xKit);
		} catch (Exception e) {
			getLogger().error(e.getMessage(), e);
		}
		return xMessageBoxFactory;
	}

	protected abstract Logger getLogger();

}