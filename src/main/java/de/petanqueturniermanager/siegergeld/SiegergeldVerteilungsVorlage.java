/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

enum SiegergeldVerteilungsVorlage {
	STANDARD_60_30_10("60 / 30 / 10"),
	STAFFEL_50_30_20("50 / 30 / 20"),
	GLEICHMAESSIG("Gleichmaessig");

	private final String anzeigeName;

	SiegergeldVerteilungsVorlage(String anzeigeName) {
		this.anzeigeName = anzeigeName;
	}

	String anzeigeName() {
		return anzeigeName;
	}

	int platzAnteil(int platz, int anzahlPlaetze) {
		if (platz < 1 || anzahlPlaetze < 1 || platz > anzahlPlaetze) {
			return 0;
		}
		if (this == GLEICHMAESSIG) {
			return gleichmaessigerAnteil(platz, anzahlPlaetze);
		}
		if (anzahlPlaetze == 1) {
			return 100;
		}
		if (this == STANDARD_60_30_10) {
			return switch (platz) {
			case 1 -> 60;
			case 2 -> anzahlPlaetze == 2 ? 40 : 30;
			case 3 -> 10;
			default -> 0;
			};
		}
		return switch (platz) {
		case 1 -> 50;
		case 2 -> anzahlPlaetze == 2 ? 50 : 30;
		case 3 -> 20;
		default -> 0;
		};
	}

	private static int gleichmaessigerAnteil(int platz, int anzahlPlaetze) {
		int basis = 100 / anzahlPlaetze;
		int rest = 100 % anzahlPlaetze;
		return basis + (platz <= rest ? 1 : 0);
	}
}
