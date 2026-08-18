/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.algorithmen.turnierserie.ergebnis;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;

/**
 * Basisklasse für team-basierte Turnierserien-Ergebnisse (Schweizer System, FormuleX).
 * Analog zu {@code supermelee.ergebnis.AbstractErgebnis}, aber mit {@code teamNr} statt
 * {@code spielerNr} als stabiler Identität über die gesamte Serie.
 */
public abstract class AbstractTeamErgebnis<T extends AbstractTeamErgebnis<?>> {

	private final int teamNr;

	private int spielPlus = 0;
	private int spielMinus = 0;
	private int punktePlus = 0;
	private int punkteMinus = 0;

	protected AbstractTeamErgebnis(int teamNr) {
		checkArgument(teamNr > 0);
		this.teamNr = teamNr;
	}

	public final int getTeamNr() {
		return this.teamNr;
	}

	public final int getSpielDiv() {
		return this.spielPlus - this.spielMinus;
	}

	public final int getSpielPlus() {
		return this.spielPlus;
	}

	@SuppressWarnings("unchecked")
	public final T setSpielPlus(int spielPlus) {
		this.spielPlus = spielPlus;
		return (T) this;
	}

	public final int getSpielMinus() {
		return this.spielMinus;
	}

	@SuppressWarnings("unchecked")
	public final T setSpielMinus(int spielMinus) {
		this.spielMinus = spielMinus;
		return (T) this;
	}

	public final int getPunkteDiv() {
		return this.punktePlus - this.punkteMinus;
	}

	public final int getPunktePlus() {
		return this.punktePlus;
	}

	@SuppressWarnings("unchecked")
	public final T setPunktePlus(int punktePlus) {
		this.punktePlus = punktePlus;
		return (T) this;
	}

	public final int getPunkteMinus() {
		return this.punkteMinus;
	}

	@SuppressWarnings("unchecked")
	public final T setPunkteMinus(int punkteMinus) {
		this.punkteMinus = punkteMinus;
		return (T) this;
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("TeamNr", this.getTeamNr())
				.add("SpielPlus", this.getSpielPlus())
				.add("SpielMinus", this.getSpielMinus())
				.add("PunktePlus", this.getPunktePlus())
				.add("PunkteMinus", this.getPunkteMinus())
				.toString();
		// @formatter:on
	}

}
