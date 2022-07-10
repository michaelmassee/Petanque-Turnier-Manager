package de.petanqueturniermanager.comp;

import java.util.ArrayList;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.beans.XPropertySet;
import com.sun.star.bridge.XBridge;
import com.sun.star.bridge.XBridgeFactory;
import com.sun.star.comp.helper.Bootstrap;
import com.sun.star.comp.helper.BootstrapException;
import com.sun.star.connection.XConnector;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;

import de.petanqueturniermanager.helper.Lo;

/**
 * Erstellung 10.07.2022 / Michael Massee
 */

public class OfficeStarter {
	// connect to locally running Office via port 8100
	private static final int SOCKET_PORT = 8100;
	private static final String SOFFICE_BIN = "soffice";

	private static final Logger logger = LogManager.getLogger(OfficeStarter.class);

	private boolean usingPipes = false;
	private XComponentContext xComponentContext = null;
	private XComponent bridgeComponent = null; // this is only set if office is opened via a socket
	private boolean headless = false;

	private OfficeStarter() {
	}

	public OfficeStarter from() {
		return new OfficeStarter();
	}

	public OfficeStarter usingPipes(boolean usingPipes) {
		this.usingPipes = usingPipes;
		return this;
	}

	public OfficeStarter loadOffice() {
		logger.info("Loading Office...");
		if (usingPipes) {
			bootstrapContext(); // connects to office via pipes
		} else {
			socketContext(); // connects to office via a socket
		}

		if (xComponentContext == null) {
			logger.info("Office context could not be created");
			System.exit(1);
		}
		return this;
	}

	//
	//	public static XComponentLoader loadOffice(boolean usingPipes)
	//
	//		// get the remote office service manager
	//		mcFactory = xcc.getServiceManager();
	//		if (mcFactory == null) {
	//			System.out.println("Office Service Manager is unavailable");
	//			System.exit(1);
	//		}
	//
	//		// desktop service handles application windows and documents
	//		xDesktop = createInstanceMCF(XDesktop.class, "com.sun.star.frame.Desktop");
	//		if (xDesktop == null) {
	//			System.out.println("Could not create a desktop service");
	//			System.exit(1);
	//		}
	//
	//		// XComponentLoader provides ability to load components
	//		return Lo.qi(XComponentLoader.class, xDesktop);
	//	} // end of loadOffice()
	//

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
			ArrayList<String> cmd = new ArrayList<>();
			// requires soffice to be in Windows PATH env var.
			cmd.add(SOFFICE_BIN);
			if (this.headless) {
				cmd.add("-headless");
			}
			cmd.add("-accept=socket,host=localhost,port=" + SOCKET_PORT + ";urp;");

			Process p = Runtime.getRuntime().exec((String[]) cmd.toArray());
			if (p != null) {
				System.out.println("Office process created");
			}
			Thread.sleep(5000);
			// Wait 5 seconds, until office is in listening mode

			// Create a local Component Context
			XComponentContext localContext = Bootstrap.createInitialComponentContext(null);

			// Get the local service manager
			XMultiComponentFactory localFactory = localContext.getServiceManager();

			// connect to Office via its socket
			/*
			 * Object urlResolver = localFactory.createInstanceWithContext( "com.sun.star.bridge.UnoUrlResolver", localContext); XUnoUrlResolver xUrlResolver = Lo.qi(XUnoUrlResolver.class, urlResolver); Object
			 * initObject = xUrlResolver.resolve( "uno:socket,host=localhost,port=" + SOCKET_PORT + ";urp;StarOffice.ServiceManager");
			 */
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
			System.out.println("Unable to socket connect to Office");
		}
	}
	//
	//	// ================== office shutdown =========================
	//
	//	public static void closeOffice()
	//	// tell office to terminate
	//	{
	//		System.out.println("Closing Office");
	//		if (xDesktop == null) {
	//			System.out.println("No office connection found");
	//			return;
	//		}
	//
	//		if (isOfficeTerminated) {
	//			System.out.println("Office has already been requested to terminate");
	//			return;
	//		}
	//
	//		int numTries = 1;
	//		while (!isOfficeTerminated && (numTries < 4)) {
	//			delay(200);
	//			isOfficeTerminated = tryToTerminate(numTries);
	//			numTries++;
	//		}
	//	} // end of closeOffice()
	//
	//	public static boolean tryToTerminate(int numTries) {
	//		try {
	//			boolean isDead = xDesktop.terminate();
	//			if (isDead) {
	//				if (numTries > 1)
	//					System.out.println(numTries + ". Office terminated");
	//				else
	//					System.out.println("Office terminated");
	//			} else
	//				System.out.println(numTries + ". Office failed to terminate");
	//			return isDead;
	//		} catch (com.sun.star.lang.DisposedException e) {
	//			System.out.println("Office link disposed");
	//			return true;
	//		} catch (java.lang.Exception e) {
	//			System.out.println("Termination exception: " + e);
	//			return false;
	//		}
	//	} // end of tryToTerminate()
	//
	//	public static void killOffice()
	//	// kill office processes using a batch file
	//	// or use JNAUtils.killOffice()
	//	{
	//		try {
	//			Runtime.getRuntime().exec("cmd /c lokill.bat");
	//			System.out.println("Killed Office");
	//		} catch (java.lang.Exception e) {
	//			System.out.println("Unable to kill Office: " + e);
	//		}
	//	} // end of killOffice()

}
