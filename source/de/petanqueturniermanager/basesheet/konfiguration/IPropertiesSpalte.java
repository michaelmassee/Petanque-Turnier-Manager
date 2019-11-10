/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

	SpielSystem getSpielSystem() throws GenerateException;

	Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException;

	Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException;

	Integer getMeldeListeHeaderFarbe() throws GenerateException;

}
