/**
* Erstellung : 17.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.ergebnis;

import static com.google.common.base.Preconditions.checkArgument;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.helper.position.Position;

public abstract class AbstractErgebnis<T extends AbstractErgebnis<?>> {

	private final int spielerNr;

	private int spielPlus = 0;
	private Position PosSpielPlus;
	private int spielMinus = 0;
	private Position PosSpielMinus;
	private int punktePlus = 0;
	private Position PosPunktePlus;
	private int punkteMinus = 0;
	private Position PosPunkteMinus;

	public AbstractErgebnis(int spielerNr) {
		checkArgument(spielerNr > 0);
		this.spielerNr = spielerNr;
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
	public T setPunkteMinus(int punkteMinus) {
		this.punkteMinus = punkteMinus;
		return (T) this;
	}

	public final int getSpielerNr() {
		return this.spielerNr;
	}

	public Position getPosSpielPlus() {
		return this.PosSpielPlus;
	}

	@SuppressWarnings("unchecked")
	public T setPosSpielPlus(Position posSpielPlus) {
		this.PosSpielPlus = Position.from(posSpielPlus);
		return (T) this;
	}

	public Position getPosSpielMinus() {
		return this.PosSpielMinus;
	}

	@SuppressWarnings("unchecked")
	public T setPosSpielMinus(Position posSpielMinus) {
		this.PosSpielMinus = Position.from(posSpielMinus);
		return (T) this;
	}

	public Position getPosPunktePlus() {
		return this.PosPunktePlus;
	}

	@SuppressWarnings("unchecked")
	public T setPosPunktePlus(Position posPunktePlus) {
		this.PosPunktePlus = Position.from(posPunktePlus);
		return (T) this;
	}

	public Position getPosPunkteMinus() {
		return this.PosPunkteMinus;
	}

	@SuppressWarnings("unchecked")
	public T setPosPunkteMinus(Position posPunkteMinus) {
		this.PosPunkteMinus = Position.from(posPunkteMinus);
		return (T) this;
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("SpielerNr",this.getSpielerNr())
				.add("\nSpielPlus", this.getSpielPlus())
				.add("PosSpielPlus", this.getPosSpielPlus())
				.add("\nSpielMinus",this.getSpielMinus())
				.add("PosSpielMinus", this.getPosSpielMinus())
				.add("\nPunktePlus", this.getPunktePlus())
				.add("PosPunktePlus", this.getPosPunktePlus())
				.add("\nPunkteMinus",this.getPunkteMinus())
				.add("PosPunkteMinus", this.getPosPunkteMinus())
				.toString();
		// @formatter:on
	}

}
