/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.algorithmen.turnierserie.ergebnis;

import static java.util.Collections.reverseOrder;
import static java.util.Comparator.comparingInt;

import java.util.Comparator;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * Über alle Spieltage einer Turnierserie aggregiertes Team-Ergebnis (ohne Streich-Spieltag).
 */
public class TeamEndranglisteErgebnis extends AbstractTeamErgebnis<TeamEndranglisteErgebnis>
		implements Comparable<TeamEndranglisteErgebnis> {

	private int anzGespielteSpieltage = 0;
	private SpielTagNr streichSpieltag;

	public TeamEndranglisteErgebnis(int teamNr) {
		super(teamNr);
	}

	public int getAnzGespielteSpieltage() {
		return this.anzGespielteSpieltage;
	}

	public TeamEndranglisteErgebnis setAnzGespielteSpieltage(int anzGespielteSpieltage) {
		this.anzGespielteSpieltage = anzGespielteSpieltage;
		return this;
	}

	public SpielTagNr getStreichSpieltag() {
		return this.streichSpieltag;
	}

	public TeamEndranglisteErgebnis setStreichSpieltag(SpielTagNr streichSpieltag) {
		this.streichSpieltag = streichSpieltag;
		return this;
	}

	public boolean isValid() {
		return getTeamNr() > 0 && getAnzGespielteSpieltage() > 0;
	}

	private Comparator<TeamEndranglisteErgebnis> getComparator() {
		return Comparator.comparingInt(TeamEndranglisteErgebnis::getSpielPlus)
				.thenComparingInt(TeamEndranglisteErgebnis::getSpielDiv)
				.thenComparingInt(TeamEndranglisteErgebnis::getPunkteDiv)
				.thenComparingInt(TeamEndranglisteErgebnis::getPunktePlus)
				.thenComparing(reverseOrder(comparingInt(TeamEndranglisteErgebnis::getTeamNr)));
	}

	public int reversedCompareTo(TeamEndranglisteErgebnis o) {
		// schlechteste an erste stelle
		return getComparator().reversed().compare(o, this);
	}

	@Override
	public int compareTo(TeamEndranglisteErgebnis o) {
		// beste an erste stelle
		return getComparator().compare(o, this);
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("AnzGespielteSpieltage", this.anzGespielteSpieltage)
				.add("StreichSpieltag", this.streichSpieltag)
				.toString()
				+ super.toString();
		// @formatter:on
	}

}
