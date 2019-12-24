/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

	SpielTagNr getAktiveSpieltag() throws GenerateException;

	void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException;

	SpielRundeNr getAktiveSpielRunde() throws GenerateException;

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException;

	Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException;

	Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException;

	Integer getMeldeListeHeaderFarbe() throws GenerateException;

	Integer getRanglisteHintergrundFarbeGerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException;

	Integer getRanglisteHeaderFarbe() throws GenerateException;

	void updateKonfigBlock() throws GenerateException;

	void doFormat() throws GenerateException;

	String getFusszeileLinks() throws GenerateException;

	String getFusszeileMitte() throws GenerateException;

	boolean zeigeArbeitsSpalten() throws GenerateException;

	String suchMatrixProperty();
}
