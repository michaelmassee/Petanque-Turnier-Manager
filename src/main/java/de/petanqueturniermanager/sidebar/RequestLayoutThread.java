/**
 * Erstellung 05.02.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.ui.XSidebar;

/**
 * requestLayout aufrufen mit eine verzögerung von 2 sec
 * 
 * @author Michael Massee
 *
 */
public class RequestLayoutThread {
	static Set<Integer> hashSet = ConcurrentHashMap.newKeySet();

	public void RequestLayout(XSidebar xSidebar) {
		if (xSidebar != null) {
			int hash = xSidebar.hashCode();
			if (!hashSet.contains(hash)) {
				hashSet.add(hash);
				new RequestLayoutThreadInt(xSidebar, hash).start();
			}
		}
	}
}

class RequestLayoutThreadInt extends Thread {
	private static final Logger logger = LogManager.getLogger(RequestLayoutThreadInt.class);
	private final XSidebar xSidebar;
	private final Integer hash;

	RequestLayoutThreadInt(XSidebar xSidebar, Integer hash) {
		this.xSidebar = xSidebar;
		this.hash = hash;
	}

	@Override
	public void run() {
		try {
			// Pause for 2 sekunden
			// Ist MEGA dirty, verzweifelte versuch LO CoreDumps :-( in griff zu kriegen
			Thread.sleep(2000);
			logger.debug("xSidebar.requestLayout");
			xSidebar.requestLayout();
		} catch (InterruptedException e) {
		} finally {
			RequestLayoutThread.hashSet.remove(hash);
		}
	}
}
