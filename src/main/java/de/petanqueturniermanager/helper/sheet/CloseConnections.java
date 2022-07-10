/**
* Erstellung : 09.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.helper.sheet;

import com.sun.star.uno.XComponentContext;

public class CloseConnections {
	// private static final Logger logger = LogManager.getLogger(CloseConnections.class);

	/**
	 * TODO Baustelle <br>
	 * Close alles ! Funktioniert so nicht ?!?!?
	 */
	public static void closeOfficeConnection(@SuppressWarnings("unused") XComponentContext xContext) {

		// try {
		// // XModel xModel = Lo.qi(XModel.class, DocumentHelper.getCurrentComponent(xContext));
		// // com.sun.star.lang.XComponent xDisposeable = Lo.qi(com.sun.star.lang.XComponent.class, xModel);
		//
		// // Schließt Document !!!
		// // xDisposeable.dispose();
		// } catch (Throwable e) {
		// logger.error("Exception disposing office process connection bridge:");
		// logger.error(e.getMessage(), e);
		// }
	}

}
