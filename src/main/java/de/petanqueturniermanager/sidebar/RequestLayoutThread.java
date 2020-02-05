/**
 * Erstellung 05.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import com.sun.star.ui.XSidebar;

/**
 * @author Michael Massee
 *
 */
public class RequestLayoutThread {

	static boolean ISRUNNING;

	static synchronized void start(XSidebar xSidebar) {
		if (ISRUNNING || xSidebar == null) {
			return;
		}
		ISRUNNING = true;
		new Thread() {
			@Override
			public void run() {
				try {
					// Pause for 3 seconds
					Thread.sleep(3000);
					xSidebar.requestLayout();
				} catch (InterruptedException e) {
				} finally {
					ISRUNNING = false;
				}
			}
		}.start();
	}

}
