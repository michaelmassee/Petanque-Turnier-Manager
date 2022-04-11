/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * @author Michael Massee
 *
 */
public interface ISchweizerPropertiesSpalte {

	String getKopfZeileLinks() throws GenerateException;

	String getKopfZeileMitte() throws GenerateException;

	String getKopfZeileRechts() throws GenerateException;

}
