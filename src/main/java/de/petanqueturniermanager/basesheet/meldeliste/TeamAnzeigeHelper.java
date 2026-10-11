package de.petanqueturniermanager.basesheet.meldeliste;


/**
 * Gemeinsame Auflösung der sichtbaren Teamkennung. Identität eines Teams ist seine UUID ({@link TeamSchluessel});
 * diese Klasse erzeugt ausschließlich den sichtbaren Zellinhalt.
 */
public final class TeamAnzeigeHelper {

	private TeamAnzeigeHelper() {
	}

	/**
	 * Wie {@link #formel(TeamSchluessel, TeamAnzeige, boolean, Formation, boolean)} für eine Teamnummer – Übergang
	 * für Systeme, deren Spielrunden noch Nummern speichern.
	 */
	public static String formel(String teamNrAdresse, TeamAnzeige anzeige, boolean teamnameSpalteAktiv,
			Formation formation, boolean vereinsnameAnzeigen) {
		return formel(TeamSchluessel.nr(teamNrAdresse), anzeige, teamnameSpalteAktiv, formation, vereinsnameAnzeigen);
	}

	/**
	 * Liefert für ein Team entweder die aktuelle Nummer oder eine dynamische Meldelisten-Formel für Team-
	 * beziehungsweise zusammengesetzte Spielernamen.
	 *
	 * @param teamnameSpalteAktiv ob die Meldeliste eine Teamname-Spalte hat – bestimmt die Lage
	 *                            der Spielerspalten; ohne Teamname-Spalte wird {@link TeamAnzeige#NAME}
	 *                            zu Spielernamen (siehe {@link TeamAnzeige#effektiv(boolean)})
	 */
	public static String formel(TeamSchluessel team, TeamAnzeige anzeige, boolean teamnameSpalteAktiv,
			Formation formation, boolean vereinsnameAnzeigen) {
		return switch (anzeige.effektiv(teamnameSpalteAktiv)) {
		case NR -> team.nrAnzeige(MeldeListeHelper.nrBereichMeldeliste());
		case SPIELERNAMEN -> MeldeListeHelper.spielerNamenFormel(team, teamnameSpalteAktiv, formation,
				vereinsnameAnzeigen);
		case NAME -> MeldeListeHelper.teamNameFormel(team, true, formation, vereinsnameAnzeigen);
		};
	}

	/** Liefert die Kopfzeile für die gewählte sichtbare Kennung. */
	public static String headerI18nKey(TeamAnzeige anzeige) {
		return switch (anzeige) {
		case NR -> "column.header.nr";
		case SPIELERNAMEN -> "column.header.spieler";
		case NAME -> "column.header.teamname";
		};
	}
}
