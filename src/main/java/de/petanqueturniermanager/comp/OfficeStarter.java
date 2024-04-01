package de.petanqueturniermanager.comp;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.connection.XConnector;
import com.sun.star.frame.XComponentLoader;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Erstellung 10.07.2022 / Michael Massee
 */

public class OfficeStarter {
	private static final Logger logger = LogManager.getLogger(OfficeStarter.class);

	// connect to locally running Office via port 8100
	private static final int SOCKET_PORT = 8100;
	// https://help.libreoffice.org/6.2/he/text/shared/guide/start_parameters.html
	private static final String SOFFICE_BIN = "soffice";

	private boolean usingPipes = false;
	private XComponentContext xComponentContext = null;

	private XComponent bridgeComponent = null; // this is only set if office is opened via a socket
	private XMultiComponentFactory mcFactory;
	private XDesktop xDesktop;
	private XComponentLoader xComponentLoader;

	private boolean headless = false;

	private static AtomicBoolean isOfficeTerminated = new AtomicBoolean();

	private OfficeStarter() {
	}

	public static OfficeStarter from() {
		return new OfficeStarter();
	}

	public OfficeStarter usingPipes(boolean usingPipes) {
		this.usingPipes = usingPipes;
		return this;
	}

	public OfficeStarter loadOffice() {
		logger.info("Loading Office...");
		isOfficeTerminated.set(false);
		if (usingPipes) {
			bootstrapContext(); // connects to office via pipes
		} else {
			socketContext(); // connects to office via a socket
		}

		if (xComponentContext == null) {
			String errorMsg = "Office context could not be created";
			logger.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}

		// get the remote office service manager
		mcFactory = xComponentContext.getServiceManager();
		if (mcFactory == null) {
			String errorMsg = "Office Service Manager is unavailable";
			logger.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}

		// desktop service handles application windows and documents
		xDesktop = Lo.createInstanceMCF(XDesktop.class, "com.sun.star.frame.Desktop", mcFactory, xComponentContext);
		if (xDesktop == null) {
			String errorMsg = "Could not create a desktop service";
			logger.error(errorMsg);
			throw new RuntimeException(errorMsg);
		}

		// XComponentLoader provides ability to load components
		xComponentLoader = Lo.qi(XComponentLoader.class, xDesktop);

		return this;
	}

	// connect pipes to office using the Bootstrap class
	// i.e. see code at http://svn.apache.org/repos/asf/openoffice/symphony/trunk/main/
	// javaunohelper/com/sun/star/comp/helper/Bootstrap.java
	private void bootstrapContext() {
		try {
			// Connect to office, if office is not running then it's started
			this.xComponentContext = Bootstrap.bootstrap(); // get remote office component context
		} catch (BootstrapException e) {
			logger.error(e.getMessage(), e);
		}
	}

	// use socket connection to Office
	// https://forum.openoffice.org/en/forum/viewtopic.php?f=44&t=1014

	private void socketContext() {
		try {
			ArrayList<String> cmd = new ArrayList<String>();
			// requires soffice to be in Linux PATH env var.
			cmd.add(SOFFICE_BIN);
			if (this.headless) {
				cmd.add("-headless");
			}
			cmd.add("-accept=socket,host=localhost,port=" + SOCKET_PORT + ";urp;");

			logger.info(String.join(",", cmd));

			Process p = Runtime.getRuntime().exec(cmd.stream().toArray(String[]::new));
			if (p != null) {
				logger.info("Office process created");
			}
			Thread.sleep(5000);
			// Wait 5 seconds, until office is in listening mode

			// Create a local Component Context
			XComponentContext localContext = Bootstrap.createInitialComponentContext(null);

			// Get the local service manager
			XMultiComponentFactory localFactory = localContext.getServiceManager();

			// connect to Office via its socket
			XConnector connector = Lo.qi(XConnector.class,
					localFactory.createInstanceWithContext("com.sun.star.connection.Connector", localContext));

			com.sun.star.connection.XConnection connection = connector
					.connect("socket,host=localhost,port=" + SOCKET_PORT);

			// create a bridge to Office via the socket
			XBridgeFactory bridgeFactory = Lo.qi(XBridgeFactory.class,
					localFactory.createInstanceWithContext("com.sun.star.bridge.BridgeFactory", localContext));

			// create a nameless bridge with no instance provider
			XBridge bridge = bridgeFactory.createBridge("socketBridgeAD", "urp", connection, null);
			this.bridgeComponent = Lo.qi(XComponent.class, bridge);

			// get the remote service manager
			XMultiComponentFactory serviceManager = Lo.qi(XMultiComponentFactory.class,
					bridge.getInstance("StarOffice.ServiceManager"));

			// retrieve Office's remote component context as a property
			XPropertySet props = Lo.qi(XPropertySet.class, serviceManager);
			// initObject);
			Object defaultContext = props.getPropertyValue("DefaultContext");

			// get the remote interface XComponentContext
			xComponentContext = Lo.qi(XComponentContext.class, defaultContext);
		} catch (java.lang.Exception e) {
			logger.error("Unable to socket connect to Office", e);
		}
	}

	// ================== office shutdown =========================
	public void closeOffice() {
		logger.info("Closing Office");
		if (xDesktop == null) {
			logger.error("No office connection found");
			return;
		}

		if (isOfficeTerminated.get()) {
			logger.warn("Office has already been requested to terminate");
			return;
		}

		int numTries = 1;
		try {
			while (!isOfficeTerminated.get() && (numTries < 4)) {
				Thread.sleep(200);
				isOfficeTerminated.set(tryToTerminate(numTries));
				numTries++;
			}
		} catch (InterruptedException e) {
		}
	}

	private boolean tryToTerminate(int numTries) {
		try {
			boolean isDead = xDesktop.terminate();
			if (isDead) {
				if (numTries > 1)
					logger.info(numTries + ". Office terminated");
				else
					logger.info("Office terminated");
			} else
				logger.error(numTries + ". Office failed to terminate");
			return isDead;
		} catch (com.sun.star.lang.DisposedException e) {
			logger.info("Office link disposed");
			return true;
		} catch (java.lang.Exception e) {
			logger.error("Termination exception: " + e.getMessage(), e);
			return false;
		}
	}

	public XComponentLoader getComponentLoader() {
		return xComponentLoader;
	}

	public XComponentContext getxComponentContext() {
		return xComponentContext;
	}

}
