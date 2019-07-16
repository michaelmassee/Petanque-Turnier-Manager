package de.petanqueturniermanager.supermelee;

import static com.google.common.base.Preconditions.checkArgument;

/*
* TeamRechner.java
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

	public SuperMeleeTeamRechner(int anzSpieler, SuperMeleeMode mode) {
		checkArgument(anzSpieler > 0);
		this.anzSpieler = anzSpieler;
		this.mode = mode;
		if (valideAnzahlSpieler()) {
			switch (mode) {
			case Triplette:
				calcTeamsTripletteAufuelenMitDoublete();
				break;
			case Doublette:
				calcTeamsDoubletteAufuellenMitTriplette();
				break;
			}
		}
	}

	private void calcTeamsDoubletteAufuellenMitTriplette() {
		if (isNurDoubletteMoeglich()) {
			anzTriplette = 0;
			anzDoublette = anzSpieler / 2;
		} else {
			// anz triplettes kann nur 1,2,3 sein
			anzTriplette = anzSpieler - ((int) Math.floor(anzSpieler / 4) * 4);
			// anz doublettes ist der rest der Spieler
			anzDoublette = (anzSpieler - (anzTriplette * 3)) / 2;
		}
	}

	private void calcTeamsTripletteAufuelenMitDoublete() {
		// 1 triplette paarung sind 6 Spieler
		int anzTriplettePaarungenGerundet = (anzSpieler / 6);
		int anzTripletteTeams = anzTriplettePaarungenGerundet * 2;
		int restSpielerMinusAnzSpielerIntriplette = anzSpieler - (anzTripletteTeams * 3);

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

		// TODO das geht einfacher als mit switch :-)
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

		anzTriplette = anzTripletteTeams;
		anzDoublette = anzDoubletteTeams; // max bis 5
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
		return ((anzSpieler / 6) * 6) == anzSpieler;
	}

	public boolean isNurDoubletteMoeglich() {
		return ((anzSpieler / 4) * 4) == anzSpieler;
	}

	public int isNurDoubletteMoeglichVal() {
		return (isNurDoubletteMoeglich() ? 1 : 0);
	}

	public boolean valideAnzahlSpieler() {
		return (anzSpieler == 7 ? false : true);
	}

	public SuperMeleeMode getMode() {
		return mode;
	}
}
