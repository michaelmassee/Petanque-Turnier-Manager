/**
 * Erstellung 17.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import com.sun.star.awt.XTopWindowListener;
import com.sun.star.lang.EventObject;

/**
 * @author Michael Massee
 *
 */
public class XTopWindowAdapter implements XTopWindowListener {

	// TODO
	// see 25. Monitoring Sheets.pdf chapter 10
	// XExtendedToolkit tk = Lo.createInstanceMCF(XExtendedToolkit.class,
	// "com.sun.star.awt.Toolkit");
	// if (tk != null)
	// tk.addTopWindowListener( new XTopWindowAdapter() {
	// public void windowClosing(EventObject eo)
	// { /* called whenever the appl. is closed */ }
	// }

	@Override
	public void disposing(EventObject arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowActivated(EventObject arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosed(EventObject arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowClosing(EventObject arg0) {
		// Clean Up Prozess Box
		// TODO Auto-generated method stub
	}

	@Override
	public void windowDeactivated(EventObject arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowMinimized(EventObject arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowNormalized(EventObject arg0) {
		// TODO Auto-generated method stub

	}

	@Override
	public void windowOpened(EventObject arg0) {
		// TODO Auto-generated method stub

	}

}
