/**
* Erstellung : 31.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spielrunde;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.supermelee.SpielRundeNr;

public class SpielerSpielrundeErgebnis {

	private SpielRundeNr spielrunde;
	private int spielerNr;
	private Position positionPlusPunkte;
	private Position positionMinusPunkte;
	private Position positionSpielerNr;
	private SpielRundeTeam spielRundeTeam;

	public static SpielerSpielrundeErgebnis from(SpielRundeNr spielrunde, int spielerNr, Position positionSpielerNr, int ersteSpalteErgebnisse, SpielRundeTeam spielRundeTeam) {
		checkNotNull(spielrunde, "spielrunde == null");
		checkNotNull(positionSpielerNr, "positionSpielerNr == null");
		checkNotNull(spielRundeTeam, "spielRundeTeam == null");
		checkArgument(spielerNr > 0, "spielerNr=%s <1", spielerNr);
		checkArgument(ersteSpalteErgebnisse > 0, "ersteSpalteErgebnisse=%s <1 ", ersteSpalteErgebnisse);

		SpielerSpielrundeErgebnis erg = new SpielerSpielrundeErgebnis();
		erg.setSpielrunde(spielrunde);
		erg.setSpielerNr(spielerNr);
		erg.setPositionSpielerNr(Position.from(positionSpielerNr));
		erg.setSpielRundeTeam(spielRundeTeam);

		if (spielRundeTeam == SpielRundeTeam.A) {
			erg.positionPlusPunkte = Position.from(positionSpielerNr).spalte(ersteSpalteErgebnisse);
			erg.positionMinusPunkte = Position.from(positionSpielerNr).spalte(ersteSpalteErgebnisse + 1);
		} else {
			erg.positionPlusPunkte = Position.from(positionSpielerNr).spalte(ersteSpalteErgebnisse + 1);
			erg.positionMinusPunkte = Position.from(positionSpielerNr).spalte(ersteSpalteErgebnisse);
		}

		return erg;
	}

	public static SpielerSpielrundeErgebnis from(SpielRundeNr spielrunde) {
		checkNotNull(spielrunde);
		SpielerSpielrundeErgebnis erg = new SpielerSpielrundeErgebnis();
		erg.setSpielrunde(spielrunde);
		return erg;
	}

	public static SpielerSpielrundeErgebnis from(SpielerSpielrundeErgebnis erg) {
		checkNotNull(erg);
		SpielerSpielrundeErgebnis ergNew = new SpielerSpielrundeErgebnis();
		ergNew.setSpielerNr(erg.getSpielerNr());
		ergNew.setSpielrunde(erg.getSpielrunde());
		ergNew.setPositionPlusPunkte(Position.from(erg.getPositionPlusPunkte()));
		ergNew.setPositionMinusPunkte(Position.from(erg.getPositionMinusPunkte()));
		ergNew.setPositionSpielerNr(Position.from(erg.getPositionSpielerNr()));
		ergNew.setSpielRundeTeam(erg.getSpielRundeTeam());
		return ergNew;
	}

	public int getSpielerNr() {
		return this.spielerNr;
	}

	public SpielerSpielrundeErgebnis setSpielerNr(int spielerNr) {
		this.spielerNr = spielerNr;
		return this;
	}

	public SpielRundeNr getSpielrunde() {
		return this.spielrunde;
	}

	public SpielerSpielrundeErgebnis setSpielrunde(SpielRundeNr spielrunde) {
		this.spielrunde = spielrunde;
		return this;
	}

	public Position getPositionPlusPunkte() {
		return this.positionPlusPunkte;
	}

	public SpielerSpielrundeErgebnis setPositionPlusPunkte(Position zellePlusPunkte) {
		this.positionPlusPunkte = zellePlusPunkte;
		return this;
	}

	public Position getPositionMinusPunkte() {
		return this.positionMinusPunkte;
	}

	public SpielerSpielrundeErgebnis setPositionMinusPunkte(Position zelleMinusPunkte) {
		this.positionMinusPunkte = zelleMinusPunkte;
		return this;
	}

	public SpielRundeTeam getSpielRundeTeam() {
		return this.spielRundeTeam;
	}

	public SpielerSpielrundeErgebnis setSpielRundeTeam(SpielRundeTeam spielRundeTeam) {
		this.spielRundeTeam = spielRundeTeam;
		return this;
	}

	public Position getPositionSpielerNr() {
		return this.positionSpielerNr;
	}

	public SpielerSpielrundeErgebnis setPositionSpielerNr(Position spielNrPos) {
		this.positionSpielerNr = spielNrPos;
		return this;
	}

	@Override
	public String toString() {
		// @formatter:off
		return MoreObjects.toStringHelper(this)
				.add("Spielrunde", this.getSpielrunde())
				.add("\nSpielerNr", this.getSpielerNr())
				.add("\nPlusPunkte", this.getPositionPlusPunkte())
				.add("\nMinusPunkte", this.getPositionMinusPunkte())
				.add("\nSpielrNrPos", this.getPositionSpielerNr())
				.add("\nTeam", this.getSpielRundeTeam())
				.toString();
		// @formatter:on
	}

}
