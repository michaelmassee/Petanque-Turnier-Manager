package de.petanqueturniermanager.supermelee;

import static com.google.common.base.Preconditions.checkArgument;

import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/*
* Erstellung     : 11.09.2017 / Michael Massee
*/

public class SuperMeleeTeamRechner {

	private final int anzSpieler;

	private int anzTriplette;
	private int anzDoublette;
	private final SuperMeleeMode mode;

	public SuperMeleeTeamRechner(int anzSpieler) {
		this(anzSpieler, SuperMeleeMode.Triplette);
	}

	/**
	 * @param anzSpieler
	 * @param mode
	 */
	public SuperMeleeTeamRechner(int anzSpieler, SuperMeleeMode mode) {
		checkArgument(anzSpieler > 0);
		this.anzSpieler = anzSpieler;
		this.mode = mode;
		if (valideAnzahlSpieler()) {
			switch (mode) {
			case Triplette:
				calcTeamsTripletteAuffuellenMitDoublette();
				break;
			case Doublette:
				calcTeamsDoubletteAuffuellenMitTriplette();
				break;
			}
		}
	}

	private void calcTeamsDoubletteAuffuellenMitTriplette() {
		int rest = anzSpieler % 4;
		anzTriplette = rest;
		anzDoublette = (anzSpieler - rest * 3) / 2;
	}

	private void calcTeamsTripletteAuffuellenMitDoublette() {
		// Doublette-Teams auffüllen: rest-Spieler (0–5) bestimmen, wie viele Doubletten nötig sind
		// Formel: (6 - rest) % 6 ergibt die Anzahl Doublette-Teams (0,5,4,3,2,1 für rest 0–5)
		int rest = anzSpieler % 6;
		anzDoublette = (6 - rest) % 6;
		anzTriplette = (anzSpieler - anzDoublette * 2) / 3;
	}

	public int getAnzSpieler() {
		return anzSpieler;
	}

	public int getAnzTriplette() {
		return anzTriplette;
	}

	public int getAnzDoublette() {
		return anzDoublette;
	}

	public int getAnzPaarungen() {
		return anzDoublette + anzTriplette;
	}

	public int getAnzBahnen() {
		return getAnzPaarungen() / 2;
	}

	public boolean isNurTripletteMoeglich() {
		return anzSpieler % 6 == 0;
	}

	public boolean isNurDoubletteMoeglich() {
		return anzSpieler % 4 == 0;
	}

	public int isNurDoubletteMoeglichVal() {
		return (isNurDoubletteMoeglich() ? 1 : 0);
	}

	public boolean valideAnzahlSpieler() {
		return anzSpieler != 7;
	}

	public SuperMeleeMode getMode() {
		return mode;
	}

	public int getAnzahlTripletteWennNurTriplette() {
		if (isNurTripletteMoeglich()) {
			return anzSpieler / 3;
		}
		return 0;
	}

	public int getAnzahlDoubletteWennNurDoublette() {
		if (isNurDoubletteMoeglich()) {
			return anzSpieler / 2;
		}
		return 0;
	}

}
