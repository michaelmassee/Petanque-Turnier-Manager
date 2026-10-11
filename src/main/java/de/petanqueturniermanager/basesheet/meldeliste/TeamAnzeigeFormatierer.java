/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

/**
 * Formatiert eine Meldelistenzeile zur sichtbaren Teamkennung (Teamname oder
 * zusammengesetzte Spielernamen). Einzige Quelle dieser Darstellung: Die Calc-Funktion
 * {@code PTM.ALG.TEAMANZEIGE} nutzt sie für die Anzeige im Spielplan, die
 * Spielrunden-Auswertung für den Rückweg von der Anzeige zur Teamnummer.
 */
public final class TeamAnzeigeFormatierer {

	private static final int TEAMNAME_SPALTE = 1;
	private static final int ERSTER_SPIELER_OFFSET = 1;
	private static final String SPIELER_TRENNER = " / ";

	private TeamAnzeigeFormatierer() {
		// Utility-Klasse – keine Instanzen
	}

	/**
	 * @param teamnameAnzeigen    {@code true} = Teamname, {@code false} = Spielernamen
	 * @param anzSpieler          Anzahl Spieler je Team (Formation)
	 * @param vereinsnameAnzeigen ob je Spieler eine Vereinsspalte folgt
	 * @param meldelistenZeile    komplette Meldelistenzeile ab Spalte A
	 * @return sichtbare Kennung, leer wenn die Zeile keine Namen enthält
	 */
	public static String formatiere(boolean teamnameAnzeigen, int anzSpieler, boolean vereinsnameAnzeigen,
			String[] meldelistenZeile) {
		if (meldelistenZeile == null) {
			return "";
		}
		if (teamnameAnzeigen) {
			return wert(meldelistenZeile, TEAMNAME_SPALTE);
		}
		int spaltenProSpieler = vereinsnameAnzeigen ? 3 : 2;
		StringBuilder anzeige = new StringBuilder();
		for (int spieler = 0; spieler < Math.max(0, anzSpieler); spieler++) {
			int vornameSpalte = ERSTER_SPIELER_OFFSET + spieler * spaltenProSpieler;
			String name = spielerAnzeige(wert(meldelistenZeile, vornameSpalte),
					wert(meldelistenZeile, vornameSpalte + 1),
					vereinsnameAnzeigen ? wert(meldelistenZeile, vornameSpalte + 2) : "");
			if (!name.isEmpty()) {
				if (anzeige.length() > 0) {
					anzeige.append(SPIELER_TRENNER);
				}
				anzeige.append(name);
			}
		}
		return anzeige.toString();
	}

	private static String spielerAnzeige(String vorname, String nachname, String verein) {
		String name;
		if (vorname.isEmpty()) {
			name = nachname;
		} else if (nachname.isEmpty()) {
			name = vorname;
		} else {
			name = vorname + " " + nachname;
		}
		if (name.isEmpty() || verein.isEmpty()) {
			return name;
		}
		return name + " (" + verein + ")";
	}

	private static String wert(String[] zeile, int idx) {
		if (idx < 0 || idx >= zeile.length || zeile[idx] == null) {
			return "";
		}
		return zeile[idx].trim();
	}
}
