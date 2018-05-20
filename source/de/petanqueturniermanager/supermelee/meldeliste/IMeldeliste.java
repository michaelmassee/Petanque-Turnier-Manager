/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.meldeliste;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.model.Meldungen;

public interface IMeldeliste {

	String formulaSverweisSpielernamen(String spielrNrAdresse);

	Meldungen getAktiveUndAusgesetztMeldungen() throws GenerateException;

	int getSpielerZeileNr(int spielerNr) throws GenerateException;

	Meldungen getAktiveMeldungen() throws GenerateException;

	Meldungen getAlleMeldungen() throws GenerateException;

}
