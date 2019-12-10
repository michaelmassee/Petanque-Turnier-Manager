/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.model.IMeldungen;

public interface IMeldeliste<T> extends ISheet, MeldeListeKonstanten, IMitSpielerSpalte {

	String formulaSverweisSpielernamen(String spielrNrAdresse);

	IMeldungen<T> getAktiveUndAusgesetztMeldungen() throws GenerateException;

	IMeldungen<T> getAktiveMeldungen() throws GenerateException;

	IMeldungen<T> getInAktiveMeldungen() throws GenerateException;

	IMeldungen<T> getAlleMeldungen() throws GenerateException;

	MeldungenSpalte<T> getMeldungenSpalte();

	int letzteSpielTagSpalte() throws GenerateException;

	int getSpielerNameSpalte();

}
