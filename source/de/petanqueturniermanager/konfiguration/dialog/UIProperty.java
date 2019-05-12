/**
 * Erstellung 12.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.konfiguration.dialog;

import com.sun.star.awt.XControlContainer;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;

/**
 * @author Michael Massee
 *
 */
public interface UIProperty {

	void save();

	void initDefault(WorkingSpreadsheet currentSpreadsheet);

	void doInsert(Object dialogModel, XControlContainer xControlCont);

}
