/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.ISheet;
import de.petanqueturniermanager.helper.sheet.IMitSpielerSpalte;
import de.petanqueturniermanager.model.Meldungen;

public interface IMeldeliste extends ISheet, MeldeListeKonstanten, IMitSpielerSpalte {

	String formulaSverweisSpielernamen(String spielrNrAdresse);

	Meldungen getAktiveUndAusgesetztMeldungen() throws GenerateException;

	Meldungen getAktiveMeldungen() throws GenerateException;

	Meldungen getInAktiveMeldungen() throws GenerateException;

	Meldungen getAlleMeldungen() throws GenerateException;

	MeldungenSpalte getMeldungenSpalte();

	int letzteSpielTagSpalte() throws GenerateException;

	int getSpielerNameSpalte();

}