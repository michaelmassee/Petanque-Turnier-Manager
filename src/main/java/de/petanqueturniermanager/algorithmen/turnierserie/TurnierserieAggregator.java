/*
* Erstellung : 18.08.2026 / Michael Massee
**/

package de.petanqueturniermanager.algorithmen.turnierserie;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamEndranglisteErgebnis;
import de.petanqueturniermanager.algorithmen.turnierserie.ergebnis.TeamSpieltagErgebnis;
import de.petanqueturniermanager.basesheet.meldeliste.SpielTagNr;

/**
 * UNO-freie Aggregationslogik für Turnierserien mit team-basierten Systemen (Schweizer System,
 * FormuleX). Team-Nummern bleiben über die gesamte Serie stabil; ein Team kann an einzelnen
 * Spieltagen fehlen (nicht teilgenommen). Die Berechnung ist analog zu
 * {@code supermelee.endrangliste.EndranglisteSheet}, aber team- statt spielerbasiert und mit
 * optionalem Ausschluss von Freilos-Spieltagen aus der Streichresultat-Ermittlung (FormuleX).
 * <p>
 * Das Einlesen der Spieltag-Ranglisten aus den Calc-Sheets erfolgt außerhalb dieser Klasse
 * (system-spezifisch, UNO-basiert); hier wird nur der bereits eingelesene Cache aggregiert.
 */
public final class TurnierserieAggregator {

	private TurnierserieAggregator() {
		// Utility-Klasse
	}

	/**
	 * Berechnet die Serien-Endrangliste aus den je Spieltag eingelesenen Team-Ergebnissen.
	 *
	 * @param cachePerSpieltag              Map: Spieltag-Nr -&gt; (Team-Nr -&gt; Ergebnis dieses Teams an diesem Spieltag)
	 * @param anzahlSpieltage               Anzahl der in der Serie enthaltenen Spieltage
	 * @param freilosVonStreichAusschliessen wenn true, werden als Freilos markierte Spieltage nie als Streichresultat gewählt
	 * @return Map: Team-Nr -&gt; aggregiertes Endranglisten-Ergebnis
	 */
	public static Map<Integer, TeamEndranglisteErgebnis> berechneEndrangliste(
			Map<Integer, Map<Integer, TeamSpieltagErgebnis>> cachePerSpieltag, int anzahlSpieltage,
			boolean freilosVonStreichAusschliessen) {

		TreeSet<Integer> alleTeamNrn = new TreeSet<>();
		for (Map<Integer, TeamSpieltagErgebnis> proSpieltag : cachePerSpieltag.values()) {
			alleTeamNrn.addAll(proSpieltag.keySet());
		}

		Map<Integer, TeamEndranglisteErgebnis> ergebnisProTeam = new LinkedHashMap<>();
		for (Integer teamNr : alleTeamNrn) {
			List<TeamSpieltagErgebnis> ergebnisseDesTeams = ergebnisseDesTeams(cachePerSpieltag, anzahlSpieltage, teamNr);
			SpielTagNr streichSpieltag = ermittleStreichSpieltag(ergebnisseDesTeams, freilosVonStreichAusschliessen);
			ergebnisProTeam.put(teamNr, aggregiere(teamNr, ergebnisseDesTeams, streichSpieltag));
		}
		return ergebnisProTeam;
	}

	private static TeamEndranglisteErgebnis aggregiere(int teamNr, List<TeamSpieltagErgebnis> ergebnisseDesTeams,
			SpielTagNr streichSpieltag) {
		int spielPlus = 0;
		int spielMinus = 0;
		int punktePlus = 0;
		int punkteMinus = 0;
		int anzGespielteSpieltage = 0;

		for (TeamSpieltagErgebnis ergebnis : ergebnisseDesTeams) {
			if (!ergebnis.isTeilgenommen()) {
				continue;
			}
			anzGespielteSpieltage++;
			if (streichSpieltag != null && streichSpieltag.getNr() == ergebnis.getSpielTagNr()) {
				continue;
			}
			spielPlus += ergebnis.getSpielPlus();
			spielMinus += ergebnis.getSpielMinus();
			punktePlus += ergebnis.getPunktePlus();
			punkteMinus += ergebnis.getPunkteMinus();
		}

		return new TeamEndranglisteErgebnis(teamNr).setSpielPlus(spielPlus).setSpielMinus(spielMinus)
				.setPunktePlus(punktePlus).setPunkteMinus(punkteMinus).setAnzGespielteSpieltage(anzGespielteSpieltage)
				.setStreichSpieltag(streichSpieltag);
	}

	private static List<TeamSpieltagErgebnis> ergebnisseDesTeams(
			Map<Integer, Map<Integer, TeamSpieltagErgebnis>> cachePerSpieltag, int anzahlSpieltage, int teamNr) {
		List<TeamSpieltagErgebnis> ergebnisse = new ArrayList<>(anzahlSpieltage);
		for (int spieltagCntr = 1; spieltagCntr <= anzahlSpieltage; spieltagCntr++) {
			Map<Integer, TeamSpieltagErgebnis> proTeam = cachePerSpieltag.get(spieltagCntr);
			TeamSpieltagErgebnis ergebnis = proTeam != null ? proTeam.get(teamNr) : null;
			if (ergebnis == null) {
				ergebnis = TeamSpieltagErgebnis.nichtTeilgenommen(SpielTagNr.from(spieltagCntr), teamNr);
			}
			ergebnisse.add(ergebnis);
		}
		return ergebnisse;
	}

	/**
	 * Ermittelt den schlechtesten Spieltag eines Teams, der bei der Endranglisten-Summe
	 * ausgeklammert wird. Gibt {@code null} zurück, wenn kein Streichresultat möglich ist
	 * (weniger als 2 Spieltage insgesamt, oder ausschließlich Freilos-Spieltage bei aktivem
	 * Ausschluss).
	 */
	static SpielTagNr ermittleStreichSpieltag(List<TeamSpieltagErgebnis> ergebnisseDesTeams,
			boolean freilosAusschliessen) {
		if (ergebnisseDesTeams.size() < 2) {
			return null;
		}
		List<TeamSpieltagErgebnis> kandidaten = new ArrayList<>(ergebnisseDesTeams);
		if (freilosAusschliessen) {
			kandidaten.removeIf(TeamSpieltagErgebnis::isFreilos);
		}
		if (kandidaten.isEmpty()) {
			return null;
		}
		kandidaten.sort(TeamSpieltagErgebnis::reversedCompareTo);
		return kandidaten.get(0).getSpielTag();
	}

}
