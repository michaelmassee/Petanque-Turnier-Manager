/**
* Erstellung : 09.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;

public class SpielRundeNr {
	private final int nr;

	public SpielRundeNr(int newNr) {
		checkArgument(newNr > -1);
		nr = newNr;
	}

	public int getNr() {
		return nr;
	}

	public static SpielRundeNr from(int i) {
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
