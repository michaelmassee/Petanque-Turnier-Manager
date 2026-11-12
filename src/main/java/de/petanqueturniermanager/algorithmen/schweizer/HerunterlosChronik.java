/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Hält fest, in welchen Runden ein Team heruntergelost wurde (Downfloat).
 * <p>
 * Ein Team gilt in Runde r als heruntergelost, wenn es in Runde r gegen einen Gegner gepaart war, der vor Runde r
 * weniger Siege hatte. Maßgeblich ist die Paarung, nicht das Ergebnis.
 * <p>
 * Ein Freilos zählt wie eine Herunterlosung: Beides ist eine Runde ohne gleich starken Gegner. Diese Sonderrolle soll
 * reihum gehen, das Freilos-Team wird deshalb in der Folgerunde möglichst nicht auch noch heruntergelost. Die Chronik
 * wird beim Einlesen der Spielrunden aufgebaut und von {@link SchweizerSystem} genutzt, um wiederholtes Herunterlosen zu vermeiden.
 */
public final class HerunterlosChronik {

	/**
	 * Eine gespielte (oder bereits ausgeloste) Begegnung zweier Teams einer Runde.
	 *
	 * @param nrA Nummer von Team A
	 * @param nrB Nummer von Team B
	 */
	public record Begegnung(int nrA, int nrB) {
	}

	private final Map<Integer, List<Integer>> rundenProTeam = new HashMap<>();

	/**
	 * Vermerkt die Herunterlosungen einer Runde.
	 *
	 * @param runde          Rundennummer
	 * @param begegnungen    alle Begegnungen der Runde ohne Freilose
	 * @param freilosTeams   Nummern der Teams mit Freilos in dieser Runde
	 * @param siegeVorRunde  Siege je Team vor dieser Runde (fehlend = 0)
	 */
	public void vermerkeRunde(int runde, List<Begegnung> begegnungen, List<Integer> freilosTeams,
			Map<Integer, Integer> siegeVorRunde) {
		freilosTeams.forEach(teamNr -> vermerke(teamNr, runde));
		for (Begegnung begegnung : begegnungen) {
			int siegeA = siegeVorRunde.getOrDefault(begegnung.nrA(), 0);
			int siegeB = siegeVorRunde.getOrDefault(begegnung.nrB(), 0);
			if (siegeA > siegeB) {
				vermerke(begegnung.nrA(), runde);
			} else if (siegeB > siegeA) {
				vermerke(begegnung.nrB(), runde);
			}
		}
	}

	/**
	 * @param teamNr Nummer des Teams
	 * @return Runden, in denen das Team heruntergelost wurde, aufsteigend; leer wenn nie
	 */
	public List<Integer> rundenFuer(int teamNr) {
		return List.copyOf(rundenProTeam.getOrDefault(teamNr, List.of()));
	}

	private void vermerke(int teamNr, int runde) {
		List<Integer> runden = rundenProTeam.computeIfAbsent(teamNr, k -> new ArrayList<>());
		if (!runden.contains(runde)) {
			runden.add(runde);
		}
	}
}
