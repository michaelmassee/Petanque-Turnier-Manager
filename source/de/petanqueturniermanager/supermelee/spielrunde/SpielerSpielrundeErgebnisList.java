/**
* Erstellung : 28.04.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spielrunde;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.petanqueturniermanager.exception.GenerateException;

public class SpielerSpielrundeErgebnisList {

	private HashSet<Integer> spielerNr = new HashSet<>();
	private ArrayList<SpielerSpielrundeErgebnis> list = new ArrayList<>();

	public void add(SpielerSpielrundeErgebnis spielerSpielrundeErgebnis) throws GenerateException {
		if (this.spielerNr.contains(spielerSpielrundeErgebnis.getSpielerNr())) {
			throw new GenerateException(
					"Spieler mit der Nr. " + spielerSpielrundeErgebnis.getSpielerNr() + " ist doppelt");
		}
		this.spielerNr.add(spielerSpielrundeErgebnis.getSpielerNr());
		this.list.add(spielerSpielrundeErgebnis);
	}

	public List<SpielerSpielrundeErgebnis> getSpielerSpielrundeErgebnis() {
		return this.list;
	}
}
