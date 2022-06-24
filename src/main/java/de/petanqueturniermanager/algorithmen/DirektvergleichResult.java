package de.petanqueturniermanager.algorithmen;

/**
 * Erstellung 23.06.2022 / Michael Massee
 */

public enum DirektvergleichResult {
	VERLOREN(1), GEWONNEN(2), GLEICH(0), KEINERGEBNISS(3), FEHLER(-1);

	DirektvergleichResult(int i) {
		code = i;
	}

	int code;

	public int getCode() {
		return code;
	}
}
