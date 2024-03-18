/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * @author Michael Massee
 *
 */
public interface ISchweizerPropertiesSpalte {

	String getKopfZeileLinks() throws GenerateException;

	String getKopfZeileMitte() throws GenerateException;

	String getKopfZeileRechts() throws GenerateException;

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException;

}
