/**
* Erstellung : 21.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

import com.sun.star.awt.XWindow;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.sheet.DocumentHelper;

public class LogWindow {

	private final XComponentContext m_xContext;

	public LogWindow(XComponentContext m_xContext) {
		this.m_xContext = m_xContext;
	}

	public void show(String sTitle, String sMessage) {
		// BAUSTELLE !!

		try {
			XWindow xParent = DocumentHelper.getCurrentFrame(m_xContext).getContainerWindow();

			// // get access to the office toolkit environment
			com.sun.star.awt.XToolkit xKit = UnoRuntime.queryInterface(com.sun.star.awt.XToolkit.class,
					m_xContext.getServiceManager().createInstanceWithContext("com.sun.star.awt.Toolkit", m_xContext));
			//
			// describe the info box ini its parameters
			com.sun.star.awt.WindowDescriptor aDescriptor = new com.sun.star.awt.WindowDescriptor();
			aDescriptor.WindowServiceName = "infobox";
			aDescriptor.Bounds = new com.sun.star.awt.Rectangle(0, 0, 300, 200);
			aDescriptor.WindowAttributes = com.sun.star.awt.WindowAttribute.BORDER
					| com.sun.star.awt.WindowAttribute.MOVEABLE | com.sun.star.awt.WindowAttribute.CLOSEABLE;
			aDescriptor.Type = com.sun.star.awt.WindowClass.MODALTOP;
			aDescriptor.ParentIndex = 1;
			aDescriptor.Parent = UnoRuntime.queryInterface(com.sun.star.awt.XWindowPeer.class, xParent);
			//
			// // create the info box window
			com.sun.star.awt.XWindowPeer xPeer = xKit.createWindow(aDescriptor);
			com.sun.star.awt.XMessageBox xInfoBox = UnoRuntime.queryInterface(com.sun.star.awt.XMessageBox.class,
					xPeer);
			if (xInfoBox == null) {
				return;
			}
			// // fill it with all given information and show it
			xInfoBox.setCaptionText(sTitle);
			xInfoBox.setMessageText(sMessage);

			xInfoBox.execute();
		} catch (java.lang.Throwable exIgnore) {
			// ignore any problem, which can occur here.
			// It's not really a bug for this example job, if
			// it's message could not be printed out!
		}
	}

}
