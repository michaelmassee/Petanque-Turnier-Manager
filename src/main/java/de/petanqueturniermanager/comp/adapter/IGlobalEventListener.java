/**
 * Erstellung 16.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.adapter;

/**
 * @author Michael Massee
 *
 */
public interface IGlobalEventListener {

	// XModel xModel = Lo.qi(XModel.class, source);
	// XSpreadsheetDocument xSpreadsheetDocument = Lo.qi(XSpreadsheetDocument.class, xModel);
//	XSpreadsheetView xSpreadsheetView = Lo.qi(XSpreadsheetView.class,
//			xModel.getCurrentController());

	default void onNew(@SuppressWarnings("unused") Object source) {
	}

	default void onLoad(@SuppressWarnings("unused") Object source) {
	}

	default void onUnload(@SuppressWarnings("unused") Object source) {
	}

	default void onUnfocus(@SuppressWarnings("unused") Object source) {
	}

	default void onViewCreated(@SuppressWarnings("unused") Object source) {
	}

}
