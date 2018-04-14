/**
* Erstellung : 21.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindow;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.sheet.DocumentHelper;

public class InfoModal {

	private final XComponentContext m_xContext;

	public InfoModal(XComponentContext m_xContext) {
		this.m_xContext = m_xContext;
	}

	public short show(String sTitle, String sMessage) {
		short result = 0;
		try {
			XWindow xParent = DocumentHelper.getCurrentFrame(this.m_xContext).getContainerWindow();

			// // get access to the office toolkit environment
			XToolkit xKit = UnoRuntime.queryInterface(XToolkit.class, this.m_xContext.getServiceManager()
					.createInstanceWithContext("com.sun.star.awt.Toolkit", this.m_xContext));
			//
			// describe the info box ini its parameters
			WindowDescriptor aDescriptor = new com.sun.star.awt.WindowDescriptor();
			aDescriptor.WindowServiceName = "infobox";
			aDescriptor.Bounds = new Rectangle(0, 0, 300, 200);
			aDescriptor.WindowAttributes = WindowAttribute.BORDER | WindowAttribute.MOVEABLE
					| WindowAttribute.CLOSEABLE;
			aDescriptor.Type = WindowClass.MODALTOP;
			aDescriptor.ParentIndex = 1;
			aDescriptor.Parent = UnoRuntime.queryInterface(XWindowPeer.class, xParent);
			//
			// // create the info box window
			XWindowPeer xPeer = xKit.createWindow(aDescriptor);
			XMessageBox xInfoBox = UnoRuntime.queryInterface(XMessageBox.class, xPeer);
			if (xInfoBox == null) {
				return 0;
			}
			// // fill it with all given information and show it
			xInfoBox.setCaptionText(sTitle);
			xInfoBox.setMessageText(sMessage);

			result = xInfoBox.execute();
		} catch (java.lang.Throwable exIgnore) {
			// ignore any problem, which can occur here.
			// It's not really a bug for this example job, if
			// it's message could not be printed out!
		}
		return result;
	}

}
