/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.maastrichter;

import de.petanqueturniermanager.helper.ColorHelper;

/**
 * Zyklische Schriftfarben-Palette für Gruppenbuchstaben (A=Grün, B=Rot, C=Blau, …),
 * gemeinsam genutzt von Maastrichter-Gruppen-Übersicht und Vorrunden-Rangliste, damit
 * ein Gruppenbuchstabe an beiden Stellen dieselbe Farbe hat.
 */
public final class MaastrichterGruppenFarbeHelper {

	private static final Integer[] GRUPPEN_FARBEN = {
			ColorHelper.CHAR_COLOR_GREEN, ColorHelper.CHAR_COLOR_RED, Integer.valueOf("0066cc", 16),
			ColorHelper.CHAR_COLOR_ORANGE, Integer.valueOf("7030a0", 16), Integer.valueOf("008080", 16),
			Integer.valueOf("8b4513", 16), Integer.valueOf("696969", 16)
	};

	private MaastrichterGruppenFarbeHelper() {
		// Utility-Klasse – keine Instanzen
	}

	/** Ermittelt die feste Palettenfarbe für einen Gruppenbuchstaben (A=Grün, B=Rot, C=Blau, …). */
	public static Integer gruppenBuchstabeFarbe(String gruppe) {
		int index = Character.toUpperCase(gruppe.charAt(0)) - 'A';
		return GRUPPEN_FARBEN[Math.floorMod(index, GRUPPEN_FARBEN.length)];
	}
}
