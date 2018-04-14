/**
* Erstellung : 17.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.ergebnis;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;

import java.util.Comparator;

import com.google.common.base.MoreObjects;

public class SpielerSpieltagErgebnis extends AbstractErgebnis<SpielerSpieltagErgebnis>
		implements Comparable<SpielerSpieltagErgebnis> {

	private final int spielTag;

	public SpielerSpieltagErgebnis(int spielTag, int spielerNr) {
		super(spielerNr);
		checkArgument(spielTag > 0);
		this.spielTag = spielTag;
	}

	public int getSpielTag() {
		return this.spielTag;
	}

	private Comparator<SpielerSpieltagErgebnis> getComparator() {
		return Comparator.comparingInt(SpielerSpieltagErgebnis::getSpielPlus)
				.thenComparingInt(SpielerSpieltagErgebnis::getSpielDiv)
				.thenComparingInt(SpielerSpieltagErgebnis::getPunkteDiv)
				.thenComparingInt(SpielerSpieltagErgebnis::getPunktePlus)
				.thenComparingInt(SpielerSpieltagErgebnis::getSpielTag)
				.thenComparing(reverseOrder(comparingInt(SpielerSpieltagErgebnis::getSpielerNr)));
	}

	public int reversedCompareTo(SpielerSpieltagErgebnis o) {
		// schlechteste an erste stelle
		return getComparator().reversed().compare(o, this);
	}

	@Override
	public int compareTo(SpielerSpieltagErgebnis o) {
		// beste an erste stelle
		return getComparator().compare(o, this);
	}

	public static SpielerSpieltagErgebnis from(int spielTag, int spielerNr) {
		return new SpielerSpieltagErgebnis(spielTag, spielerNr);
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("SpielTag", this.getSpielTag())
				.toString()
				+ super.toString();
		// @formatter:on
	}

}
