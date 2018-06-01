/**
* Erstellung : 09.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

public class SpielTagNr {
	private int nr;

	public SpielTagNr(int newNr) throws GenerateException {
		setNr(newNr);
	}

	public int getNr() throws GenerateException {
		return this.nr;
	}

	public SpielTagNr setNr(int newNr) throws GenerateException {
		if (newNr < 1) {
			throw new GenerateException("Ungültige Spieltagnummer" + newNr);
		}
		this.nr = newNr;
		ProcessBox.from().spielTag(this);
		return this;
	}

	public static SpielTagNr from(int i) throws GenerateException {
		return new SpielTagNr(i);
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Nr", this.nr)
				.toString();
		// @formatter:on
	}

}
