/**
 * Erstellung 24.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.sidebar.config;

import de.petanqueturniermanager.comp.turnierevent.ITurnierEvent;
import de.petanqueturniermanager.sidebar.layout.Layout;

/**
 * @author Michael Massee
 *
 */
public interface ConfigSidebarElement {
	Layout getLayout();

	default void onPropertiesChanged(@SuppressWarnings("unused") ITurnierEvent eventObj) {
	}

}
