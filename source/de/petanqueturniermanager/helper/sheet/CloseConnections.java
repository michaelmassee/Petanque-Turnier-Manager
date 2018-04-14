/**
* Erstellung : 09.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import static com.sun.star.uno.UnoRuntime.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.lang.XComponent;

public class CloseConnections {
	private static final Logger logger = LogManager.getLogger(CloseConnections.class);

	/**
	 * Close our connection to the office process.
	 */
	public static void closeOfficeConnection() {
		try {
			// get the bridge factory from the local service manager
			XBridgeFactory bridgeFactory = queryInterface(XBridgeFactory.class,
					Bootstrap.createSimpleServiceManager().createInstance("com.sun.star.bridge.BridgeFactory"));

			if (bridgeFactory != null) {
				for (XBridge bridge : bridgeFactory.getExistingBridges()) {
					// dispose of this bridge after closing its connection
					queryInterface(XComponent.class, bridge).dispose();
				}
			}
		} catch (Throwable e) {
			logger.error("Exception disposing office process connection bridge:");
			logger.error(e.getMessage(), e);
		}

	} // end closeOfficeConnection()

}
