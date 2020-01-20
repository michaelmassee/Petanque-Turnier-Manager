/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class OnConfigChangedEvent implements ITurnierEvent {

	private final TurnierSystem turnierSystem;
	private final SpielTagNr spieltagnr;
	private final SpielRundeNr spielRundeNr;

	public OnConfigChangedEvent(TurnierSystem turnierSystem, SpielTagNr spieltag, SpielRundeNr spielRundeNr) {
		this.turnierSystem = checkNotNull(turnierSystem);
		spieltagnr = checkNotNull(spieltag);
		this.spielRundeNr = checkNotNull(spielRundeNr);
	}

	/**
	 * @return the turnierSystem
	 */
	public final TurnierSystem getTurnierSystem() {
		return turnierSystem;
	}

	/**
	 * @return the spieltagnr
	 */
	public SpielTagNr getSpieltagnr() {
		return spieltagnr;
	}

	/**
	 * @return the spielRundeNr
	 */
	public SpielRundeNr getSpielRundeNr() {
		return spielRundeNr;
	}

}
