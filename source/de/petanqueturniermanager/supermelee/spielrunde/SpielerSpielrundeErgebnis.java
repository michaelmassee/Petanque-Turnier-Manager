/**
* Erstellung : 31.03.2018 / Michael Massee
**/

package de.petanqueturniermanager.supermelee.spielrunde;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.helper.position.Position;

public class SpielerSpielrundeErgebnis {

	private int spielrunde;
	private int spielerNr;
	private Position positionPlusPunkte;
	private Position positionMinusPunkte;
	private Position positionSpielerNr;
	private SpielRundeTeam spielRundeTeam;

	public static SpielerSpielrundeErgebnis from(int spielrunde, int spielerNr, Position positionSpielerNr,
			int ersteSpalteErgebnisse, SpielRundeTeam spielRundeTeam) {
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

	public static SpielerSpielrundeErgebnis from(int spielrunde) {
		SpielerSpielrundeErgebnis erg = new SpielerSpielrundeErgebnis();
		erg.setSpielrunde(spielrunde);
		return erg;
	}

	public static SpielerSpielrundeErgebnis from(SpielerSpielrundeErgebnis erg) {
		SpielerSpielrundeErgebnis ergNew = new SpielerSpielrundeErgebnis();
		ergNew.setSpielerNr(erg.getSpielerNr());
		ergNew.setSpielrunde(erg.getSpielerNr());
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

	public int getSpielrunde() {
		return this.spielrunde;
	}

	public SpielerSpielrundeErgebnis setSpielrunde(int spielrunde) {
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
