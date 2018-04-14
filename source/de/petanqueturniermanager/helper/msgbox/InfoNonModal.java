/**
* Erstellung : 21.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.msgbox;

public class InfoNonModal {

	public void show(String sTitle, String sMessage) {
		// Couldnt be implemented really using the toolkit ...
		// Because we need a parent anytime.
		// And showing e.g. a java dialog can make some trouble
		// inside office ... but we have no chance here.
		final String sFinalTitle = sTitle;
		final String sFinalMessage = sMessage;

		// On Mac OS X, AWT/Swing must not be accessed from the AppKit thread, so call
		// SwingUtilities.invokeLater always on a fresh thread to avoid that problem
		// (also, the current thread must not wait for that fresh thread to terminate,
		// as that would cause a deadlock if this thread is the AppKit thread):
		final Runnable doRun = new Runnable() {
			@Override
			public void run() {
				javax.swing.JOptionPane.showMessageDialog(null, sFinalMessage, sFinalTitle,
						javax.swing.JOptionPane.INFORMATION_MESSAGE);
			}
		};

		new Thread(doRun) {
			@Override
			public void run() {
				javax.swing.SwingUtilities.invokeLater(doRun);
			}
		}.start();
	}

}
