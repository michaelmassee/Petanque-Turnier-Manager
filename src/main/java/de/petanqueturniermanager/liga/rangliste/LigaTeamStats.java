package de.petanqueturniermanager.liga.rangliste;

record LigaTeamStats(int teamNr, String teamName,
		int punktePlus, int punkteMinus,
		int spielePlus, int spieleMinus,
		int spPunktePlus, int spPunkteMinus) {

	int spieleDiff() {
		return spielePlus - spieleMinus;
	}

	int spPunkteDiff() {
		return spPunktePlus - spPunkteMinus;
	}

	int begegnungen() {
		return punktePlus + punkteMinus;
	}

	LigaTeamStats plus(int pktPlus, int pktMinus, int spPlus, int spMinus, int spPktPlus, int spPktMinus) {
		return new LigaTeamStats(teamNr, teamName,
				punktePlus + pktPlus, punkteMinus + pktMinus,
				spielePlus + spPlus, spieleMinus + spMinus,
				spPunktePlus + spPktPlus, spPunkteMinus + spPktMinus);
	}
}
