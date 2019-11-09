/**
* Erstellung : 09.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.GenerateException;

public class SpielTagNr {
	private int nr;

	public SpielTagNr(int newNr) throws GenerateException {
		setNr(newNr);
	}

	public int getNr() {
		return nr;
	}

	public SpielTagNr setNr(int newNr) throws GenerateException {
		if (newNr < 1) {
			throw new GenerateException("Ungültige Spieltagnummer" + newNr);
		}
		nr = newNr;
		return this;
	}

	public static SpielTagNr from(int i) throws GenerateException {
		return new SpielTagNr(i);
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
