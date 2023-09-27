/**
 * Erstellung : 05.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public interface ISuperMeleePropertiesSpalte extends IPropertiesSpalte {

	SpielTagNr getAktiveSpieltag() throws GenerateException;

	void setAktiveSpieltag(SpielTagNr spieltag) throws GenerateException;

	SpielRundeNr getAktiveSpielRunde() throws GenerateException;

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde) throws GenerateException;

	Integer getSpielRundeNeuAuslosenAb() throws GenerateException;

	Integer getSpielRundeHintergrundFarbeGerade() throws GenerateException;

	Integer getSpielRundeHintergrundFarbeUnGerade() throws GenerateException;

	Integer getSpielRundeHeaderFarbe() throws GenerateException;

	Integer getRanglisteHintergrundFarbe_StreichSpieltag_Gerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbe_StreichSpieltag_UnGerade() throws GenerateException;

	Integer getNichtGespielteRundePlus() throws GenerateException;

	Integer getNichtGespielteRundeMinus() throws GenerateException;

	String getSpielrundeSpielbahn() throws GenerateException;

	Integer getMaxAnzGespielteSpieltage() throws GenerateException;

	Integer getMaxAnzSpielerInSpalte() throws GenerateException;

	@Override
	String getFusszeileLinks() throws GenerateException;

	@Override
	String getFusszeileMitte() throws GenerateException;

	boolean getSpielrunde1Header() throws GenerateException;

	SuperMeleeMode getSuperMeleeMode() throws GenerateException;

	boolean getSpielrundePlan() throws GenerateException;

	boolean getSetzPositionenAktiv() throws GenerateException;

	boolean getGleichePaarungenAktiv() throws GenerateException;

	SuprMleEndranglisteSortMode getSuprMleEndranglisteSortMode();

}
