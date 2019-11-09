/**
* Erstellung : 09.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.GenerateException;

public class SpielRundeNr {
	private int nr;

	public SpielRundeNr(int newNr) throws GenerateException {
		setNr(newNr);
	}

	public int getNr() {
		return nr;
	}

	public SpielRundeNr setNr(int newNr) throws GenerateException {
		if (newNr < 1) {
			throw new GenerateException("Ungültige Spielrundenummer" + newNr);
		}
		nr = newNr;
		return this;
	}

	public SpielRundeNr minus(int anzahl) throws GenerateException {
		setNr(getNr() - anzahl);
		return this;
	}

	public static SpielRundeNr from(int i) throws GenerateException {
		return new SpielRundeNr(i);
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Nr", nr)
				.toString();
		// @formatter:on
	}

}
