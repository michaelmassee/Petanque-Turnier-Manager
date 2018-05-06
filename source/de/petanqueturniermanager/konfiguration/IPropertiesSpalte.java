/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

import de.petanqueturniermanager.supermelee.meldeliste.Formation;
import de.petanqueturniermanager.supermelee.meldeliste.SpielSystem;

public interface IPropertiesSpalte {

	int getSpieltag();

	int getSpielRunde();

	void setSpielRunde(int neueSpielrunde);

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
