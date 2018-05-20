package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.AlgorithmenException;

/*
* Meldungen.java
*
* Erstellung     : 06.09.2017 / Michael Massee
*
*/

public class Meldungen {

	private final ArrayList<Spieler> spielerList;

	public Meldungen() {
		this.spielerList = new ArrayList<>();
	}

	public Meldungen addSpielerWennNichtVorhanden(List<Spieler> spielerlist) {
		checkNotNull(spielerlist, "spielerlist == null");
		checkArgument(!spielerlist.isEmpty(), "spielerlist ist leer");

		for (Spieler spielerAusList : spielerlist) {
			addSpielerWennNichtVorhanden(spielerAusList);
		}
		return this;
	}

	public Meldungen addSpielerWennNichtVorhanden(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");

		if (!this.spielerList.contains(spieler)) {
			this.spielerList.add(spieler);
		}
		return this;
	}

	/**
	 * Liste der Spieler nach links verschieben
	 */
	public Meldungen shifLeft() {
		Collections.rotate(this.spielerList, -1);
		return this;
	}

	/**
	 * Liste der Spieler mischen
	 */
	public Meldungen shuffle() {
		Collections.shuffle(this.spielerList);
		return this;
	}

	public List<Spieler> spieler() {
		return new ArrayList<>(this.spielerList);
	}

	/**
	 * fuer alle Spieler in der Liste team auf null
	 *
	 * @throws AlgorithmenException
	 */
	public Meldungen resetTeam() throws AlgorithmenException {
		for (Spieler spielerausList : this.spielerList) {
			spielerausList.deleteTeam();
		}
		return this;
	}

	public List<Spieler> spielerOhneTeam() throws AlgorithmenException {
		List<Spieler> spielerOhneTeam = new ArrayList<>();
		for (Spieler spielerausList : this.spielerList) {
			if (!spielerausList.isIstInTeam()) {
				spielerOhneTeam.add(spielerausList);
			}
		}
		return spielerOhneTeam;
	}

	public Meldungen removeSpieler(Spieler spieler) {
		this.spielerList.remove(spieler);
		return this;
	}

	/**
	 * @param nr
	 * @return null when not found
	 */

	public Spieler findSpielerByNr(int nr) {
		Spieler spieler = null;
		for (Spieler spielerausList : this.spielerList) {
			if (spielerausList.getNr() == nr) {
				spieler = spielerausList;
				break;
			}
		}
		return spieler;
	}

	public int size() {
		return this.spielerList.size();
	}

	@Override
	public String toString() {

		String spielerNr = "[";
		for (Spieler spielerAusTeam : this.spielerList) {
			if (spielerNr.length() > 1) {
				spielerNr += ",";
			}
			spielerNr += spielerAusTeam.getNr();
		}
		spielerNr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Anzahl", this.size())
				.add("Spieler", spielerNr)
				.toString();
		// @formatter:on

	}

	public final ArrayList<Spieler> getSpielerList() {
		return this.spielerList;
	}

}
