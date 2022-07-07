/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

	Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException;

	Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException;

	Integer getMeldeListeHeaderFarbe() throws GenerateException;

	Integer getRanglisteHintergrundFarbeGerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException;

	Integer getRanglisteHeaderFarbe() throws GenerateException;

	String getFusszeileLinks() throws GenerateException;

	String getFusszeileMitte() throws GenerateException;

	boolean zeigeArbeitsSpalten() throws GenerateException;

}
