/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

final class SiegergeldVerteilung {

	private SiegergeldVerteilung() {
	}

	static int platzAnteil(int platz) {
		return switch (platz) {
		case 1 -> 60;
		case 2 -> 30;
		case 3 -> 10;
		default -> 0;
		};
	}

	static int gruppenAnteil(String ersteGruppenName, String gruppenName) {
		return ersteGruppenName.equals(gruppenName) ? 100 : 0;
	}
}
