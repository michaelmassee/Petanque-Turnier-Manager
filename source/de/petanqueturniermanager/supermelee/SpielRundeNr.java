/**
* Erstellung : 09.05.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.msgbox.ProcessBox;

public class SpielRundeNr {
	private int nr;

	public SpielRundeNr(int newNr) throws GenerateException {
		setNr(newNr);
	}

	public int getNr() throws GenerateException {
		return this.nr;
	}

	public SpielRundeNr setNr(int newNr) throws GenerateException {
		if (newNr < 1) {
			throw new GenerateException("Ungültige Spielrundenummer" + newNr);
		}
		this.nr = newNr;
		ProcessBox.from().spielRunde(this);
		return this;
	}

	public static SpielRundeNr from(int i) throws GenerateException {
		return new SpielRundeNr(i);
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
