/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

/**
 * @author Michael Massee
 *
 */
public interface ITurnierEventListener {

	default void onNewCreated(@SuppressWarnings("unused") ITurnierEvent eventObj) { // neues Turnier erstellt
	}

	default void onConfigChanged(@SuppressWarnings("unused") ITurnierEvent eventObj) { // neues Turnier erstellt
	}

}
