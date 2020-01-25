/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class OnNewCreatedEvent implements ITurnierEvent {

	private final TurnierSystem turnierSystem;

	public OnNewCreatedEvent(TurnierSystem turnierSystem) {
		this.turnierSystem = turnierSystem;
	}

	/**
	 * @return the turnierSystem
	 */
	public final TurnierSystem getTurnierSystem() {
		return turnierSystem;
	}

}