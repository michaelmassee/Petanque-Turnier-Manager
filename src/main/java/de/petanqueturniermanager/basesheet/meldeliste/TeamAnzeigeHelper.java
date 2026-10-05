package de.petanqueturniermanager.basesheet.meldeliste;


/**
 * Gemeinsame Auflösung der sichtbaren Teamkennung. Die Teamnummer bleibt in den
 * Spielsystemen die stabile technische Identität; diese Klasse erzeugt ausschließlich
 * den sichtbaren Zellinhalt.
 */
public final class TeamAnzeigeHelper {

	private TeamAnzeigeHelper() {
	}

	/**
	 * Liefert für eine Teamnummer entweder die Nummer selbst oder eine dynamische
	 * Meldelisten-Formel für Team- beziehungsweise zusammengesetzte Spielernamen.
	 */
	public static String formel(String teamNrAdresse, TeamAnzeige anzeige,
			Formation formation, boolean vereinsnameAnzeigen) {
		return switch (anzeige) {
		case NR -> teamNrAdresse;
		case SPIELERNAMEN -> MeldeListeHelper.teamNameFormel(teamNrAdresse, false, formation,
				vereinsnameAnzeigen);
		case NAME -> MeldeListeHelper.teamNameFormel(teamNrAdresse, true, formation, vereinsnameAnzeigen);
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
