/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.konfiguration;

public interface IPropertiesSpalte {

	int getSpieltag();

	int getSpielRunde();

	void setSpielRunde(int neueSpielrunde);

	Integer getSpielRundeNeuAuslosenAb();

	Integer getSpielRundeHintergrundFarbeGerade();

	Integer getSpielRundeHintergrundFarbeUnGerade();

	Integer getSpielRundeHeaderFarbe();

}
