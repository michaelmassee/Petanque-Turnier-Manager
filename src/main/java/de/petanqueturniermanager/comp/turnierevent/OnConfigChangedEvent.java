/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
public class OnConfigChangedEvent implements ITurnierEvent {

	private final SpielTagNr spieltagnr;
	private final SpielRundeNr spielRundeNr;
	private final WorkingSpreadsheet workingSpreadsheet;

	public OnConfigChangedEvent(SpielTagNr spieltag, SpielRundeNr spielRundeNr, WorkingSpreadsheet workingSpreadsheet) {
		spieltagnr = checkNotNull(spieltag);
		this.spielRundeNr = checkNotNull(spielRundeNr);
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
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

	@Override
	public WorkingSpreadsheet getWorkingSpreadsheet() {
		return workingSpreadsheet;
	}

}
