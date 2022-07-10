package de.petanqueturniermanager.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.sun.star.container.XChild;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/**
 * Erstellung 10.07.2022 / Michael Massee <br>
 * copy from Andrew Davison
 */

public final class Lo {
	private static final Logger logger = LogManager.getLogger(Lo.class);

	// the "Loki" function -- reduces typing
	public static <T> T qi(Class<T> aType, Object o) {
		return UnoRuntime.queryInterface(aType, o);
	}

	/*
	 * create an interface object of class aType from the named service; uses 'old' XMultiServiceFactory, so a document must have been already loaded/created
	 */
	public static <T> T createInstanceMSF(Class<T> aType, String serviceName, XMultiServiceFactory msf) {
		if (msf == null) {
			logger.error("No document found");
			return null;
		}

		T interfaceObj = null;
		try {
			Object o = msf.createInstance(serviceName); // create service component
			interfaceObj = Lo.qi(aType, o);
			// uses bridge to obtain proxy to remote interface inside service;
			// implements casting across process boundaries
		} catch (Exception e) {
			logger.error("Couldn't create interface for \"" + serviceName + "\"", e);
		}
		return interfaceObj;
	}

	/*
	 * create an interface object of class aType from the named service; uses XComponentContext and 'new' XMultiComponentFactory so only a bridge to office is needed
	 */

	public static <T> T createInstanceMCF(Class<T> aType, String serviceName, XMultiComponentFactory mcFactory,
			XComponentContext xComponentContext) {
		if ((xComponentContext == null) || (mcFactory == null)) {
			logger.error("No office connection found");
			return null;
		}

		T interfaceObj = null;
		try {
			Object o = mcFactory.createInstanceWithContext(serviceName, xComponentContext);
			// create service component using the specified component context
			interfaceObj = Lo.qi(aType, o);
			// uses bridge to obtain proxy to remote interface inside service;
			// implements casting across process boundaries
		} catch (Exception e) {
			logger.error("Couldn't create interface for \"" + serviceName + "\"", e);
		}
		return interfaceObj;
	}

	/*
	 * create an interface object of class aType from the named service and arguments; uses XComponentContext and 'new' XMultiComponentFactory so only a bridge to office is needed
	 */

	public static <T> T createInstanceMCF(Class<T> aType, String serviceName, Object[] args,
			XMultiComponentFactory mcFactory, XComponentContext xComponentContext) {
		if ((xComponentContext == null) || (mcFactory == null)) {
			logger.error("No office connection found");
			return null;
		}

		T interfaceObj = null;
		try {
			Object o = mcFactory.createInstanceWithArgumentsAndContext(serviceName, args, xComponentContext);
			// create service component using the specified args and component context
			interfaceObj = Lo.qi(aType, o);
			// uses bridge to obtain proxy to remote interface inside service;
			// implements casting across process boundaries
		} catch (Exception e) {
			logger.error("Couldn't create interface for \"" + serviceName + "\"", e);
		}
		return interfaceObj;
	} // end of createInstanceMCF()

	// retrieves the parent of the given object
	public static <T> T getParent(Object aComponent, Class<T> aType) {
		XChild xAsChild = Lo.qi(XChild.class, aComponent);
		return Lo.qi(aType, xAsChild.getParent());
	}

}
