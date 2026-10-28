/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Einheitliche Breite der Mannschaftsspalten in Spielrunden, Spielplänen und Turnierbäumen. Zeigt eine Spalte die
 * Spielernamen eines Teams, bekommt jeder Spieler 4&nbsp;cm – wie die Spielerspalten der Supermêlée-Spielrunde
 * (Vorlage). So werden Triplettes nicht auf eine unlesbare Schrift eingeschrumpft. Nummern und Teamnamen behalten
 * die Breite des jeweiligen Blatts.
 */
public final class TeamSpaltenBreite {

	/** Breite je Spieler in 1/100 mm: 4 cm wie in der Supermêlée-Spielrunde. */
	public static final int BREITE_JE_SPIELER = 4000;

	private TeamSpaltenBreite() {
	}

	/**
	 * @param anzeige        gewählte Teamanzeige des Blatts
	 * @param teamnameAktiv  ob die Meldeliste eine Teamname-Spalte hat (ohne sie zeigt „Teamname" Spielernamen)
	 * @param formation      Meldeliste-Formation (Spieler je Team)
	 * @param standardBreite bisherige Breite des Blatts; Mindestbreite
	 * @return Spaltenbreite in 1/100 mm
	 */
	public static int fuer(TeamAnzeige anzeige, boolean teamnameAktiv, Formation formation, int standardBreite) {
		checkNotNull(anzeige);
		checkNotNull(formation);
		if (anzeige.effektiv(teamnameAktiv) != TeamAnzeige.SPIELERNAMEN) {
			return standardBreite;
		}
		return Math.max(standardBreite, formation.getAnzSpieler() * BREITE_JE_SPIELER);
	}
}
