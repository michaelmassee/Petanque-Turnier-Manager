/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

record SiegergeldStartOptionen(SiegergeldModus modus, int gruppenAnzahl, int plaetzeProGruppe,
		int ranglistenPlaetze, SiegergeldVerteilungsVorlage vorlage) {

	static final int DEFAULT_GRUPPEN_ANZAHL = 1;
	static final int DEFAULT_PLAETZE = 3;
	static final int MAX_GRUPPEN = 26;
	static final int MAX_PLAETZE = 64;

	SiegergeldStartOptionen {
		if (modus == null) {
			modus = SiegergeldModus.GRUPPEN;
		}
		if (vorlage == null) {
			vorlage = SiegergeldVerteilungsVorlage.STANDARD_60_30_10;
		}
		gruppenAnzahl = kappe(gruppenAnzahl, 1, MAX_GRUPPEN, DEFAULT_GRUPPEN_ANZAHL);
		plaetzeProGruppe = kappe(plaetzeProGruppe, 1, MAX_PLAETZE, DEFAULT_PLAETZE);
		ranglistenPlaetze = kappe(ranglistenPlaetze, 1, MAX_PLAETZE, DEFAULT_PLAETZE);
	}

	static SiegergeldStartOptionen defaults() {
		return new SiegergeldStartOptionen(SiegergeldModus.GRUPPEN, DEFAULT_GRUPPEN_ANZAHL,
				DEFAULT_PLAETZE, DEFAULT_PLAETZE, SiegergeldVerteilungsVorlage.STANDARD_60_30_10);
	}

	int relevantePlaetze() {
		return modus == SiegergeldModus.RANGLISTE ? ranglistenPlaetze : plaetzeProGruppe;
	}

	private static int kappe(int wert, int min, int max, int fallback) {
		if (wert < min) {
			return fallback;
		}
		return Math.min(wert, max);
	}
}
