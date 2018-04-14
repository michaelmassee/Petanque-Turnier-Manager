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

public class QuestionBox extends AbstractMessageBox {

	private static final Logger logger = LogManager.getLogger(QuestionBox.class);

	public QuestionBox(XComponentContext m_xContext) {
		super(m_xContext);
	}

	/**
	 * shows a question messagebox
	 *
	 * @return MessageBoxResults<br>
	 * const short CANCEL = 0<br>
	 * The user canceled the XMessageBox, by pressing "Cancel" or "Abort" button. More...<br>
	 * const short OK = 1<br>
	 * The user pressed the "Ok" button.<br>
	 * const short YES = 2<br>
	 * The user pressed the "Yes" button.<br>
	 * const short NO = 3<br>
	 * The user pressed the "No" button.<br>
	 * const short RETRY = 4<br>
	 * The user pressed the "Retry" button.<br>
	 * const short IGNORE = 5<br>
	 * The user pressed the "Ignore" button.<br>
	 */
	public short showYesNo(String title, String message) {
		short nResult = 0;
		XMessageBox xMessageBox = this.getXMessageBoxFactory().createMessageBox(getWindowPeer(),
				MessageBoxType.QUERYBOX, MessageBoxButtons.BUTTONS_YES_NO, title, message);

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
