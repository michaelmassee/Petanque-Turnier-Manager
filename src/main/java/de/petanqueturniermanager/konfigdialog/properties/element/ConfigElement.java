/*
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.konfigdialog.properties.element;

import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public interface ConfigElement {
	Layout getLayout();

	default void onPropertiesChanged(@SuppressWarnings("unused") ITurnierEvent eventObj) {
	}

}
