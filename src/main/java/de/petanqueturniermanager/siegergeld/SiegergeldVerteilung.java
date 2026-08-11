/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

final class SiegergeldVerteilung {

	private SiegergeldVerteilung() {
	}

	static int platzAnteil(int platz) {
		return platzAnteil(platz, 3, SiegergeldVerteilungsVorlage.STANDARD_60_30_10);
	}

	static int platzAnteil(int platz, int anzahlPlaetze, SiegergeldVerteilungsVorlage vorlage) {
		return vorlage.platzAnteil(platz, anzahlPlaetze);
	}

	static int gruppenAnteil(String ersteGruppenName, String gruppenName) {
		return ersteGruppenName.equals(gruppenName) ? 100 : 0;
	}

	static int gruppenAnteil(int gruppenIndex, int gruppenAnzahl) {
		if (gruppenIndex < 0 || gruppenAnzahl < 1 || gruppenIndex >= gruppenAnzahl) {
			return 0;
		}
		int basis = 100 / gruppenAnzahl;
		int rest = 100 % gruppenAnzahl;
		return basis + (gruppenIndex < rest ? 1 : 0);
	}
}
