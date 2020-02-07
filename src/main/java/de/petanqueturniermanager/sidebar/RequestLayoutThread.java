/**
 * Erstellung 05.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import java.util.Hashtable;

import com.sun.star.ui.XSidebar;

/**
 * Katastrophe ... nur Coredumps
 *
 * @author Michael Massee
 *
 */
public class RequestLayoutThread {

	static final Hashtable<Integer, Boolean> isRunning = new Hashtable<>();

	private RequestLayoutThread() {
	}

	public static synchronized void start(XSidebar xSidebar) {

		if (isRunning.containsKey(xSidebar.hashCode()) && isRunning.get(xSidebar.hashCode())) {
			return;
		}
		isRunning.put(xSidebar.hashCode(), true);

		new Thread() {
			@Override
			public void run() {
				try {
					// Pause for 2 sekunden
					// Ist MEGA dirty, verzweifelte versuch LO CoreDumps :-( in griff zu kriegen
					Thread.sleep(2000);
					xSidebar.requestLayout();
				} catch (InterruptedException e) {
				} finally {
					isRunning.put(xSidebar.hashCode(), false);
				}
			}
		}.start();
	}

}
