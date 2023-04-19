/**
 * Erstellung 07.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp;

import com.sun.star.frame.TerminationVetoException;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XTerminateListener;
import com.sun.star.lang.EventObject;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.SheetRunner;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

/**
 * @author Michael Massee
 *
 */
public class TerminateListener implements XTerminateListener {

	private static TerminateListener terminateListener = null;

	public static synchronized void addThisListenerOnce(XComponentContext context) {
		if (terminateListener == null) {
			XDesktop currentDesktop = DocumentHelper.getCurrentDesktop(context);
			terminateListener = new TerminateListener();
			currentDesktop.addTerminateListener(terminateListener);
		}
	}

	/**
	 * Is called when the master environment (e.g., desktop) is about to terminate.<br>
	 * Termination can be intercepted by throwing TerminationVetoException.<br>
	 * Interceptor will be the new owner of desktop and should call XDesktop::terminate() after finishing his own operations.
	 */

	@Override
	public void queryTermination(EventObject arg0) throws TerminationVetoException {
		if (SheetRunner.isRunning()) {
			SheetRunner.cancelRunner();
		}
	}

	/**
	 * is called when the master environment is finally terminated.
	 */
	@Override
	public void notifyTermination(EventObject arg0) {
		ProcessBox.dispose();
		PetanqueTurnierMngrSingleton.dispose();
	}

	@Override
	public void disposing(EventObject arg0) {
		// System.out.println("disposing");
	}

}
