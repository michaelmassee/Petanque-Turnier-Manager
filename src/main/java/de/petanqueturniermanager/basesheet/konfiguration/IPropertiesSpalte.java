/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.basesheet.SheetTabFarben;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

	// ---------------------------------------------------------------
	// Tab-Farben (konfigurierbar, Defaults aus SheetTabFarben)
	// ---------------------------------------------------------------

	default int getMeldelisteTabFarbe() {
		return SheetTabFarben.MELDELISTE;
	}

	default int getTeilnehmerTabFarbe() {
		return SheetTabFarben.TEILNEHMER;
	}

	default int getSpielrundeTabFarbe() {
		return SheetTabFarben.SPIELRUNDE;
	}

	default int getRanglisteTabFarbe() {
		return SheetTabFarben.RANGLISTE;
	}

	default int getDirektvergleichTabFarbe() {
		return SheetTabFarben.DIREKTVERGLEICH;
	}

	// ---------------------------------------------------------------
	// Zellhintergrundfarben
	// ---------------------------------------------------------------

	Integer getMeldeListeHintergrundFarbeGerade();

	MeldungenHintergrundFarbeGeradeStyle getMeldeListeHintergrundFarbeGeradeStyle();

	Integer getMeldeListeHintergrundFarbeUnGerade();

	MeldungenHintergrundFarbeUnGeradeStyle getMeldeListeHintergrundFarbeUnGeradeStyle();

	Integer getMeldeListeHeaderFarbe();

	Integer getRanglisteHintergrundFarbeGerade();

	Integer getRanglisteHintergrundFarbeUnGerade();

	Integer getRanglisteHeaderFarbe();

	String getFusszeileLinks();

	String getFusszeileMitte();

	Integer getMaxAnzTeilnehmerInSpalte();

	String getTurnierlogoUrl();

	default boolean isEditierbareFelder() {
		return true;
	}

}
