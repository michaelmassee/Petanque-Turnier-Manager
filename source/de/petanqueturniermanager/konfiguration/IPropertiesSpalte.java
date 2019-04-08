/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;
import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public interface IPropertiesSpalte {

	SpielTagNr getAktiveSpieltag() throws GenerateException;

	void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException;

	SpielRundeNr getAktiveSpielRunde() throws GenerateException;

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException;

	Integer getSpielRundeNeuAuslosenAb() throws GenerateException;

	Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException;

	Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielRundeHeaderFarbe() throws GenerateException;

	Integer getRanglisteHintergrundFarbeGerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException;

	Integer getRanglisteHeaderFarbe() throws GenerateException;

	Integer getNichtGespielteRundePlus() throws GenerateException;

	Integer getNichtGespielteRundeMinus() throws GenerateException;

	Formation getFormation() throws GenerateException;

	SpielSystem getSpielSystem() throws GenerateException;

	String getSpielrundeSpielbahn() throws GenerateException;

	Integer getAnzGespielteSpieltage() throws GenerateException;

	String getFusszeileLinks() throws GenerateException;

	String getFusszeileMitte() throws GenerateException;
}
