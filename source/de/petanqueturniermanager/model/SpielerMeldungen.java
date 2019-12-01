package de.petanqueturniermanager.model;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.AlgorithmenException;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/*
* Meldungen.java
*
* Erstellung     : 06.09.2017 / Michael Massee
*
*/

public class SpielerMeldungen implements IMeldungen<SpielerMeldungen> {

	private final ArrayList<Spieler> spielerList;

	public SpielerMeldungen() {
		spielerList = new ArrayList<>();
	}

	@Override
	public SpielerMeldungen addNewWennNichtVorhanden(RowData meldungZeile) {
		int spielerNr = meldungZeile.get(0).getIntVal(-1);
		if (spielerNr > 0) {
			Spieler spieler = Spieler.from(spielerNr);
			int nichtzusammen = meldungZeile.get(2).getIntVal(-1);
			if (nichtzusammen > 0) {
				spieler.setSetzPos(nichtzusammen);
			}
			this.addSpielerWennNichtVorhanden(spieler);
		}
		return this;
	}

	public SpielerMeldungen addSpielerWennNichtVorhanden(List<Spieler> spielerlist) {
		checkNotNull(spielerlist, "spielerlist == null");
		checkArgument(!spielerlist.isEmpty(), "spielerlist ist leer");

		for (Spieler spielerAusList : spielerlist) {
			addSpielerWennNichtVorhanden(spielerAusList);
		}
		return this;
	}

	public SpielerMeldungen addSpielerWennNichtVorhanden(Spieler spieler) {
		checkNotNull(spieler, "spieler == null");

		if (!spielerList.contains(spieler)) {
			spielerList.add(spieler);
		}
		return this;
	}

	/**
	 * Liste der Spieler nach links verschieben
	 */
	public SpielerMeldungen shifLeft() {
		Collections.rotate(spielerList, -1);
		return this;
	}

	/**
	 * Liste der Spieler mischen
	 */
	public SpielerMeldungen shuffle() {
		Collections.shuffle(spielerList);
		return this;
	}

	public final Iterable<Spieler> getSpielerList() {
		return spielerList;
	}

	public List<Spieler> spieler() {
		return new ArrayList<>(spielerList);
	}

	/**
	 * fuer alle Spieler in der Liste team auf null
	 *
	 * @throws AlgorithmenException
	 */
	public SpielerMeldungen resetTeam() throws AlgorithmenException {
		for (Spieler spielerausList : spielerList) {
			spielerausList.deleteTeam();
		}
		return this;
	}

	public List<Spieler> spielerOhneTeam() throws AlgorithmenException {
		List<Spieler> spielerOhneTeam = new ArrayList<>();
		for (Spieler spielerausList : spielerList) {
			if (!spielerausList.isIstInTeam()) {
				spielerOhneTeam.add(spielerausList);
			}
		}
		return spielerOhneTeam;
	}

	public SpielerMeldungen removeSpieler(Spieler spieler) {
		spielerList.remove(spieler);
		return this;
	}

	/**
	 * @param nr
	 * @return null when not found
	 */

	public Spieler findSpielerByNr(int nr) {
		Spieler spieler = null;
		for (Spieler spielerausList : spielerList) {
			if (spielerausList.getNr() == nr) {
				spieler = spielerausList;
				break;
			}
		}
		return spieler;
	}

	public int size() {
		return spielerList.size();
	}

	@Override
	public String toString() {

		String spielerNr = "[";
		for (Spieler spielerAusTeam : spielerList) {
			if (spielerNr.length() > 1) {
				spielerNr += ",";
			}
			spielerNr += spielerAusTeam.getNr();
		}
		spielerNr += "]";

		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Anzahl", size())
				.add("Spieler", spielerNr)
				.toString();
		// @formatter:on

	}

	public final void sortNachNummer() {
		spielerList.sort(new Comparator<Spieler>() {
			@Override
			public int compare(Spieler o1, Spieler o2) {
				return o1.compareTo(o2);
			}
		});
	}

}
