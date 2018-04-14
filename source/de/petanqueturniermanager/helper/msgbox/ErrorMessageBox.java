/**
* Erstellung : 02.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.awt.MessageBoxButtons;
import com.sun.star.awt.MessageBoxType;
import com.sun.star.awt.XMessageBox;
import com.sun.star.uno.XComponentContext;

public class ErrorMessageBox extends AbstractMessageBox {

	private static final Logger logger = LogManager.getLogger(ErrorMessageBox.class);

	public ErrorMessageBox(XComponentContext m_xContext) {
		super(m_xContext);
	}

	/**
	 * shows an error messagebox
	 */
	public short showOk(String title, String message) {
		short nResult = 0;
		XMessageBox xMessageBox = this.getXMessageBoxFactory().createMessageBox(getWindowPeer(),
				MessageBoxType.ERRORBOX, MessageBoxButtons.BUTTONS_OK, title, message);

		if (xMessageBox != null) {
			nResult = xMessageBox.execute();
		}
		return nResult;
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

}
