/**
* Erstellung : 17.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.ergebnis;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;

import java.util.Comparator;

public class SpielerEndranglisteErgebnis extends AbstractErgebnis<SpielerEndranglisteErgebnis>
		implements Comparable<SpielerEndranglisteErgebnis> {

	public SpielerEndranglisteErgebnis(int spielerNr) {
		super(spielerNr);
	}

	public boolean isValid() {
		return this.getSpielerNr() > 0 && getSpielPlus() > -1 && getSpielMinus() > -1 && getPunktePlus() > -1
				&& getPunkteMinus() > -1;
	}

	private Comparator<SpielerEndranglisteErgebnis> getComparator() {
		return Comparator.comparingInt(SpielerEndranglisteErgebnis::getSpielPlus)
				.thenComparingInt(SpielerEndranglisteErgebnis::getSpielDiv)
				.thenComparingInt(SpielerEndranglisteErgebnis::getPunkteDiv)
				.thenComparingInt(SpielerEndranglisteErgebnis::getPunktePlus)
				.thenComparing(reverseOrder(comparingInt(SpielerEndranglisteErgebnis::getSpielerNr)));
	}

	public int reversedCompareTo(SpielerEndranglisteErgebnis o) {
		// schlechteste an erste stelle
		return getComparator().reversed().compare(o, this);
	}

	@Override
	public int compareTo(SpielerEndranglisteErgebnis o) {
		// beste an erste stelle
		return getComparator().compare(o, this);
	}

}
