/**
 * Erstellung 06.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;

/**
 * @author Michael Massee
 *
 */
@Deprecated
public interface IKonfigurationSheet extends ISheet {
	void update() throws GenerateException;
}
