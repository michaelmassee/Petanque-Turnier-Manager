package de.petanqueturniermanager.supermelee;

import static com.google.common.base.Preconditions.checkArgument;

/*
* TeamRechner.java
*
* Erstellung     : 11.09.2017 / Michael Massee
*
*/

public class TeamRechner {

	private final int anzSpieler;
	private int anzTriplette;

	private int anzDoublette;
	private boolean nurDoubletteMoeglich;

	public TeamRechner(int anzSpieler) {
		checkArgument(anzSpieler > 0);
		this.anzSpieler = anzSpieler;
		this.calcTeams();
	}

	private void calcTeams() {
		// triplette paarung = 6 Spieler
		int anzTriplettePaarungenGerundet = (anzSpieler / 6);
		int anzTripletteTeams = anzTriplettePaarungenGerundet * 2;
		// int anzSpielerIntriplette = anzTriplettePaarungenGerundet * 6;
		int restSpielerMinusAnzSpielerIntriplette = anzSpieler - (anzTripletteTeams * 3);

		// restSpielerMinusAnzSpielerIntriplette = 0 = nur Triplette, geht auf

		// der rest der Spieler muss auf doublette aufgeteilt werden.
		switch (restSpielerMinusAnzSpielerIntriplette) {
		case 1:
			// 1 restspieler, 4 Teams vom Triplette abziehen = 12 Spieler
			anzTripletteTeams = anzTripletteTeams - 4;
			break;
		case 2:
		case 3:
			// 2 Teams vom Triplette abziehen = 6 Spieler
			anzTripletteTeams = anzTripletteTeams - 2;
		}

		// neue rest spieler die auf doublette auf zu teilen sind
		restSpielerMinusAnzSpielerIntriplette = anzSpieler - (anzTripletteTeams * 3);

		int anzDoubletteTeams = 0;

		// rest = 0,4,5,8,9,13
		switch (restSpielerMinusAnzSpielerIntriplette) {
		case 4:
			anzDoubletteTeams = 2;
			break;
		case 5:
			anzTripletteTeams++;
			anzDoubletteTeams = 1;
			break;
		case 8:
			anzDoubletteTeams = 4;
			break;
		case 9:
			anzTripletteTeams++;
			anzDoubletteTeams = 3;
			break;
		case 13:
			anzTripletteTeams++;
			anzDoubletteTeams = 5;
			break;
		}

		this.anzTriplette = anzTripletteTeams;
		this.anzDoublette = anzDoubletteTeams; // max bis 5
		this.nurDoubletteMoeglich = ((this.anzSpieler / 4) * 4) == this.anzSpieler;
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

	public boolean isNurDoubletteMoeglich() {
		return nurDoubletteMoeglich;
	}

	public int isNurDoubletteMoeglichVal() {
		return (nurDoubletteMoeglich ? 1 : 0);
	}

	public boolean valideAnzahlSpieler() {
		return (anzSpieler == 7 ? false : true);
	}
}
