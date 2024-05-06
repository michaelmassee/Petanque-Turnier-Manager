/**
 * Erstellung 12.04.2020 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.konfiguration;

import de.petanqueturniermanager.basesheet.spielrunde.SpielrundeSpielbahn;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.SpielrundeHintergrundFarbeUnGeradeStyle;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

/**
 * @author Michael Massee
 *
 */
public interface ISchweizerPropertiesSpalte {

	String getKopfZeileLinks();

	String getKopfZeileMitte();

	String getKopfZeileRechts();

	void setAktiveSpielRunde(SpielRundeNr neueSpielrunde);

	SpielRundeNr getAktiveSpielRunde();

	Integer getSpielRundeHintergrundFarbeGerade();

	SpielrundeHintergrundFarbeGeradeStyle getSpielRundeHintergrundFarbeGeradeStyle();

	Integer getSpielRundeHintergrundFarbeUnGerade();

	SpielrundeHintergrundFarbeUnGeradeStyle getSpielRundeHintergrundFarbeUnGeradeStyle();

	Integer getSpielRundeHeaderFarbe();

	SpielrundeSpielbahn getSpielrundeSpielbahn();

	void setSpielrundeSpielbahn(SpielrundeSpielbahn option);

}
