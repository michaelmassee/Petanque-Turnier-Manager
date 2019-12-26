/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.liga.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * @author Michael Massee
 *
 */
public interface ILigaPropertiesSpalte {

	Integer getSpielPlanHeaderFarbe() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielPlanHintergrundFarbeGerade() throws GenerateException;

}
