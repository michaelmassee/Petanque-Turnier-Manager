/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

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

	boolean zeigeArbeitsSpalten();

}
