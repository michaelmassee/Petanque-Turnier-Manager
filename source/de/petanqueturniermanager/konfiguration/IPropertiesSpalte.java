/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public interface IPropertiesSpalte {

	int getAktuelleSpieltag();

	void setAktuelleSpieltag(int spieltag);

	int getAktuelleSpielRunde();

	void setAktuelleSpielRunde(int neueSpielrunde);

	Integer getSpielRundeNeuAuslosenAb();

	Integer getSpielRundeHintergrundFarbeGerade();

	Integer getSpielRundeHintergrundFarbeUnGerade();

	Integer getSpielRundeHeaderFarbe();

	Integer getRanglisteHintergrundFarbeGerade();

	Integer getRanglisteHintergrundFarbeUnGerade();

	Integer getRanglisteHeaderFarbe();

	Integer getNichtGespielteRundePlus();

	Integer getNichtGespielteRundeMinus();

	Formation getFormation();

	SpielSystem getSpielSystem();

}
