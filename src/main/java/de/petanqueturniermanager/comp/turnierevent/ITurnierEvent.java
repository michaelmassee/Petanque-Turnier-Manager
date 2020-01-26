/**
 * Erstellung 17.01.2020 / Michael Massee
 */
package de.petanqueturniermanager.comp.turnierevent;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public interface ITurnierEvent {

	WorkingSpreadsheet getWorkingSpreadsheet(); // von welchen Document wurde der Event ausgelöst

}
