/**
* Erstellung : 17.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.ergebnis;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;

import java.util.Comparator;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.supermelee.SpielTagNr;

public class SpielerSpieltagErgebnis extends AbstractErgebnis<SpielerSpieltagErgebnis> implements Comparable<SpielerSpieltagErgebnis> {

	private final SpielTagNr spielTag;

	public SpielerSpieltagErgebnis(SpielTagNr spielTag, int spielerNr) {
		super(spielerNr);
		this.spielTag = checkNotNull(spielTag);
	}

	public int getSpielTagNr() {
		if (spielTag != null) {
			try {
				return spielTag.getNr();
			} catch (GenerateException e) {
				return 0;
			}
		}
		return 0;
	}

	public SpielTagNr getSpielTag() {
		return spielTag;
	}

	private Comparator<SpielerSpieltagErgebnis> getComparator() {
		return Comparator.comparingInt(SpielerSpieltagErgebnis::getSpielPlus).thenComparingInt(SpielerSpieltagErgebnis::getSpielDiv)
				.thenComparingInt(SpielerSpieltagErgebnis::getPunkteDiv).thenComparingInt(SpielerSpieltagErgebnis::getPunktePlus)
				.thenComparingInt(SpielerSpieltagErgebnis::getSpielTagNr).thenComparing(reverseOrder(comparingInt(SpielerSpieltagErgebnis::getSpielerNr)));
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

	public static SpielerSpieltagErgebnis from(SpielTagNr spielTag, int spielerNr) {
		return new SpielerSpieltagErgebnis(spielTag, spielerNr);
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("SpielTag", getSpielTag())
				.toString()
				+ super.toString();
		// @formatter:on
	}

}
