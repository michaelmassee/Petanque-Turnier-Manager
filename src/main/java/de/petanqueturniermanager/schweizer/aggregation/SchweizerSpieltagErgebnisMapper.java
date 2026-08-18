/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.schweizer.aggregation;

import de.petanqueturniermanager.algorithmen.schweizer.SchweizerTeamErgebnis;
import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamSpieltagErgebnis;
import de.petanqueturniermanager.supermelee.SpielTagNr;

/**
 * Übersetzt ein {@link SchweizerTeamErgebnis} (Siege/Buchholz/Feinbuchholz-basierte Wertung
 * eines einzelnen Spieltags) in ein generisches {@link TeamSpieltagErgebnis} für die
 * Turnierserien-Aggregation ({@code algorithmen.turnierserie}).
 * <p>
 * Buchholz und Feinbuchholz sind reine Tie-Break-Werte innerhalb eines Spieltags (abhängig von
 * den an diesem Tag getroffenen Gegnern) und fließen bewusst nicht in die Serien-Endrangliste
 * ein; dort entscheidet Siege → SpielDiff → PunkteDiff → PunktePlus (siehe
 * {@link de.petanqueturniermanager.algorithmen.turnierserie.TurnierserieAggregator}).
 */
public final class SchweizerSpieltagErgebnisMapper {

	private SchweizerSpieltagErgebnisMapper() {
		// Utility-Klasse
	}

	/**
	 * @param spielTag              Spieltag, dem das Ergebnis zugeordnet wird
	 * @param ergebnis              Schweizer-Ranglisten-Ergebnis dieses Spieltags
	 * @param anzahlGespielteRunden Anzahl der an diesem Spieltag tatsächlich gespielten Runden
	 *                              (Petanque kennt kein Unentschieden: Niederlagen = Runden − Siege)
	 */
	public static TeamSpieltagErgebnis von(SpielTagNr spielTag, SchweizerTeamErgebnis ergebnis,
			int anzahlGespielteRunden) {
		int spielMinus = Math.max(0, anzahlGespielteRunden - ergebnis.siege());
		int punktePlus = ergebnis.erzieltePunkte();
		int punkteMinus = ergebnis.erzieltePunkte() - ergebnis.punktedifferenz();

		return new TeamSpieltagErgebnis(spielTag, ergebnis.teamNr()).setSpielPlus(ergebnis.siege())
				.setSpielMinus(spielMinus).setPunktePlus(punktePlus).setPunkteMinus(punkteMinus);
	}

}
