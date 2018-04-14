/**
* Erstellung : 10.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.meldeliste;

import de.petanqueturniermanager.model.Meldungen;

public interface IMeldeliste {

	String formulaSverweisSpielernamen(String spielrNrAdresse);

	Meldungen getAktiveUndAusgesetztMeldungenAktuellenSpielTag();

	int getSpielerZeileNr(int spielerNr);

}
