/**
 * Erstellung : 05.04.2018 / Michael Massee
 **/

package de.petanqueturniermanager.supermelee.konfiguration;

import de.petanqueturniermanager.basesheet.konfiguration.IPropertiesSpalte;
import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.supermelee.SpielRundeNr;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public interface ISuperMeleePropertiesSpalte extends IPropertiesSpalte {

	SpielTagNr getAktiveSpieltag();

	void setAktiveSpieltag(SpielTagNr spieltag);

	SpielRundeNr getAktiveSpielRunde();

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde);

	Integer getSpielRundeNeuAuslosenAb();

	Integer getSpielRundeHintergrundFarbeGerade();

	SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle();

	Integer getSpielRundeHintergrundFarbeUnGerade();

	SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle();

	Integer getSpielRundeHeaderFarbe();

	Integer getRanglisteHintergrundFarbeStreichSpieltagGerade();

	Integer getRanglisteHintergrundFarbeStreichSpieltagUnGerade();

	Integer getNichtGespielteRundePlus();

	Integer getNichtGespielteRundeMinus();

	SpielrundeSpielbahn getSpielrundeSpielbahn();

	void setSpielrundeSpielbahn(SpielrundeSpielbahn option);

	Integer getMaxAnzGespielteSpieltage();

	Integer getMaxAnzSpielerInSpalte();

	@Override
	String getFusszeileLinks();

	@Override
	String getFusszeileMitte();

	boolean getSpielrunde1Header();

	SuperMeleeMode getSuperMeleeMode();

	boolean getSpielrundePlan();

	boolean getSetzPositionenAktiv();

	boolean getGleichePaarungenAktiv();

	SuprMleEndranglisteSortMode getSuprMleEndranglisteSortMode();

}
