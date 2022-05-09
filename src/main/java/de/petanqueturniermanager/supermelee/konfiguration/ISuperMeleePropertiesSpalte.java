/**
* Erstellung : 05.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;

public interface ISuperMeleePropertiesSpalte extends IPropertiesSpalte {

	Integer getSpielRundeNeuAuslosenAb() throws GenerateException;

	Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException;

	Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielRundeHeaderFarbe() throws GenerateException;

	Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException;

	Integer getNichtGespielteRundePlus() throws GenerateException;

	Integer getNichtGespielteRundeMinus() throws GenerateException;

	String getSpielrundeSpielbahn() throws GenerateException;

	Integer getAnzGespielteSpieltage() throws GenerateException;

	@Override
	String getFusszeileLinks() throws GenerateException;

	@Override
	String getFusszeileMitte() throws GenerateException;

	boolean getSpielrunde1Header() throws GenerateException;

	SuperMeleeMode getSuperMeleeMode() throws GenerateException;

	boolean getSpielrundePlan() throws GenerateException;

	boolean getSetzPositionenAktiv() throws GenerateException;

}
