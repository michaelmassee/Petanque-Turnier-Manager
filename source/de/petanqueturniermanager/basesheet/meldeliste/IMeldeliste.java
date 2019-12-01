/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.model.SpielerMeldungen;

public interface IMeldeliste extends ISheet, MeldeListeKonstanten, IMitSpielerSpalte {

	String formulaSverweisSpielernamen(String spielrNrAdresse);

	SpielerMeldungen getAktiveUndAusgesetztMeldungen() throws GenerateException;

	SpielerMeldungen getAktiveMeldungen() throws GenerateException;

	SpielerMeldungen getInAktiveMeldungen() throws GenerateException;

	SpielerMeldungen getAlleMeldungen() throws GenerateException;

	MeldungenSpalte getMeldungenSpalte();

	int letzteSpielTagSpalte() throws GenerateException;

	int getSpielerNameSpalte();

}
