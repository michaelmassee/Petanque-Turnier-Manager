/*
 * Erstellung : 03.10.2026 / Michael Massee
 **/
package de.petanqueturniermanager.jedergegenjeden.meldeliste;

import de.petanqueturniermanager.basesheet.meldeliste.MeldeListeKonstanten;
import de.petanqueturniermanager.jedergegenjeden.konfiguration.JGJKonfigurationSheet;

/**
 * Spalten-Layout der JGJ-Meldeliste: Nr, optional Teamname, Spieler
 * (Vorname/Nachname/[Verein]), SP, Aktiv.
 * <p>
 * Einzige Quelle für die Spaltenberechnung – genutzt vom Meldeliste-Aufbau
 * ({@link JGJMeldeListeDelegate}) und vom Blattschutz, damit die freigegebenen
 * Spalten nicht vom tatsächlich erzeugten Layout abweichen.
 */
public final class JGJMeldeListeSpalten {

	private JGJMeldeListeSpalten() {
	}

	public static int spaltenProSpieler(JGJKonfigurationSheet konfig) {
		return konfig.isMeldeListeVereinsnameAnzeigen() ? 3 : 2;
	}

	public static int ersterSpielerOffset(JGJKonfigurationSheet konfig) {
		return MeldeListeKonstanten.SPIELER_NR_SPALTE + (konfig.isMeldeListeTeamnameAnzeigen() ? 2 : 1);
	}

	public static int letzteDataSpalte(JGJKonfigurationSheet konfig) {
		return ersterSpielerOffset(konfig)
				+ konfig.getMeldeListeFormation().getAnzSpieler() * spaltenProSpieler(konfig) - 1;
	}

	public static int setzPositionSpalte(JGJKonfigurationSheet konfig) {
		return letzteDataSpalte(konfig) + 1;
	}

	public static int aktivSpalte(JGJKonfigurationSheet konfig) {
		return setzPositionSpalte(konfig) + 1;
	}
}
