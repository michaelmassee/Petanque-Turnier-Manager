/**
 * Erstellung 17.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import com.sun.star.awt.XExtendedToolkit;
import com.sun.star.awt.XTopWindowListener;
import com.sun.star.lang.EventObject;

import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class XTopWindowAdapter implements XTopWindowListener {

	private static XExtendedToolkit extendedToolkit = null;

	/**
	 * einmal wenn nicht vorhanden diesen Windows Adapter registrieren
	 *
	 * @param workingSpreadsheet
	 */

	public static void addThisListenerOnce(WorkingSpreadsheet workingSpreadsheet) {
		if (extendedToolkit == null) {
			extendedToolkit = workingSpreadsheet.createInstanceMCF(XExtendedToolkit.class, "com.sun.star.awt.Toolkit");
			extendedToolkit.addTopWindowListener(new XTopWindowAdapter());
		}
	}

	@Override
	public void disposing(EventObject arg0) {
	}

	@Override
	public void windowActivated(EventObject arg0) {
	}

	@Override
	public void windowClosed(EventObject arg0) {
	}

	/**
	 * wird aufgerufen bevor speicher dialog<br>
	 * und für jeden Libreoffice Fenster
	 */

	@Override
	public void windowClosing(EventObject arg0) {
		// Clean Up Prozess Box, kann wieder aufgemacht werden
		ProcessBox.dispose();
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
