/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

/**
 * @author Michael Massee
 *
 */
public interface ITurnierEventListener {

	default void onGenerateStart(@SuppressWarnings("unused") ITurnierEvent eventObj) { // neues Turnier erstellt
	}

	default void onGenerateReady(@SuppressWarnings("unused") ITurnierEvent eventObj) { // neues Turnier erstellt
	}

}
