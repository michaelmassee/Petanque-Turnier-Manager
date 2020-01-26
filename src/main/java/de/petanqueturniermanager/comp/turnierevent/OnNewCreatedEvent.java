/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.supermelee.meldeliste.TurnierSystem;

/**
 * @author Michael Massee
 *
 */
public class OnNewCreatedEvent implements ITurnierEvent {

	private final TurnierSystem turnierSystem;
	private final WorkingSpreadsheet workingSpreadsheet;

	public OnNewCreatedEvent(TurnierSystem turnierSystem, WorkingSpreadsheet workingSpreadsheet) {
		this.turnierSystem = turnierSystem;
		this.workingSpreadsheet = checkNotNull(workingSpreadsheet);
	}

	/**
	 * @return the turnierSystem
	 */
	public final TurnierSystem getTurnierSystem() {
		return turnierSystem;
	}

	@Override
	public WorkingSpreadsheet getWorkingSpreadsheet() {
		return workingSpreadsheet;
	}

}
