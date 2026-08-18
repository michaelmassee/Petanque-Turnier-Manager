/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.algorithmen.turnierserie.ergebnis;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;

import java.util.Comparator;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * Ergebnis eines Teams an einem einzelnen Spieltag einer Turnierserie.
 * Ein Team, das an diesem Spieltag nicht teilgenommen hat, wird über ein
 * Sentinel-Ergebnis mit {@code spielPlus == -1} repräsentiert (analog Supermelees
 * "Nuller-Spieltag" für fehlende Spielerteilnahme).
 */
public class TeamSpieltagErgebnis extends AbstractTeamErgebnis<TeamSpieltagErgebnis>
		implements Comparable<TeamSpieltagErgebnis> {

	private final SpielTagNr spielTag;
	private boolean freilos = false;

	public TeamSpieltagErgebnis(SpielTagNr spielTag, int teamNr) {
		super(teamNr);
		this.spielTag = checkNotNull(spielTag);
	}

	public int getSpielTagNr() {
		return this.spielTag.getNr();
	}

	public SpielTagNr getSpielTag() {
		return this.spielTag;
	}

	public boolean isTeilgenommen() {
		return getSpielPlus() > -1;
	}

	public boolean isFreilos() {
		return this.freilos;
	}

	public TeamSpieltagErgebnis setFreilos(boolean freilos) {
		this.freilos = freilos;
		return this;
	}

	private Comparator<TeamSpieltagErgebnis> getComparator() {
		return Comparator.comparingInt(TeamSpieltagErgebnis::getSpielPlus).thenComparingInt(TeamSpieltagErgebnis::getSpielDiv)
				.thenComparingInt(TeamSpieltagErgebnis::getPunkteDiv).thenComparingInt(TeamSpieltagErgebnis::getPunktePlus)
				.thenComparingInt(TeamSpieltagErgebnis::getSpielTagNr)
				.thenComparing(reverseOrder(comparingInt(TeamSpieltagErgebnis::getTeamNr)));
	}

	public int reversedCompareTo(TeamSpieltagErgebnis o) {
		// schlechteste an erste stelle
		return getComparator().reversed().compare(o, this);
	}

	@Override
	public int compareTo(TeamSpieltagErgebnis o) {
		// beste an erste stelle
		return getComparator().compare(o, this);
	}

	public static TeamSpieltagErgebnis nichtTeilgenommen(SpielTagNr spielTag, int teamNr) {
		return new TeamSpieltagErgebnis(spielTag, teamNr).setSpielPlus(-1);
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("SpielTag", getSpielTag())
				.add("Freilos", this.freilos)
				.toString()
				+ super.toString();
		// @formatter:on
	}

}
