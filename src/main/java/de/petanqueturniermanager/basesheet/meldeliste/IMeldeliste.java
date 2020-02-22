/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.model.IMeldungen;

public interface IMeldeliste<T, M> extends ISheet, MeldeListeKonstanten, IMitSpielerSpalte {

	String formulaSverweisSpielernamen(String spielrNrAdresse);

	IMeldungen<T, M> getAktiveUndAusgesetztMeldungen() throws GenerateException;

	IMeldungen<T, M> getAktiveMeldungen() throws GenerateException;

	IMeldungen<T, M> getInAktiveMeldungen() throws GenerateException;

	IMeldungen<T, M> getAlleMeldungen() throws GenerateException;

	MeldungenSpalte<T, M> getMeldungenSpalte();

	int letzteSpielTagSpalte() throws GenerateException;

	int getSpielerNameSpalte();

}
