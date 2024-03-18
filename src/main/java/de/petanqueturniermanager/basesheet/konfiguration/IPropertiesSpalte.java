/**
 * Erstellung 10.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.konfiguration;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeGeradeStyle;
import de.petanqueturniermanager.helper.cellstyle.MeldungenHintergrundFarbeUnGeradeStyle;

/**
 * @author Michael Massee
 *
 */
public interface IPropertiesSpalte {

	Integer getMeldeListeHintergrundFarbeGerade() throws GenerateException;

	MeldungenHintergrundFarbeGeradeStyle getMeldeListeHintergrundFarbeGeradeStyle() throws GenerateException;

	Integer getMeldeListeHintergrundFarbeUnGerade() throws GenerateException;

	MeldungenHintergrundFarbeUnGeradeStyle getMeldeListeHintergrundFarbeUnGeradeStyle() throws GenerateException;

	Integer getMeldeListeHeaderFarbe() throws GenerateException;

	Integer getRanglisteHintergrundFarbeGerade() throws GenerateException;

	Integer getRanglisteHintergrundFarbeUnGerade() throws GenerateException;

	Integer getRanglisteHeaderFarbe() throws GenerateException;

	String getFusszeileLinks() throws GenerateException;

	String getFusszeileMitte() throws GenerateException;

	boolean zeigeArbeitsSpalten() throws GenerateException;

}
