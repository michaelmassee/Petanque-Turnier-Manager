/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.List;

import de.petanqueturniermanager.model.Spieler;
import de.petanqueturniermanager.model.Team;

/**
 * Aufstellung eines Teams für Runde 2 einer Trip-Tête-Begegnung: zwei Spieler bilden das Doublette,
 * der dritte spielt das Tête-à-tête. Das ursprüngliche Triplette-Team muss exakt drei Spieler enthalten.
 *
 * @param doublette1 erster Doublette-Spieler
 * @param doublette2 zweiter Doublette-Spieler
 * @param tete       Tête-à-tête-Spieler
 * @author Michael Massee
 */
public record TripTeteAufstellung(Spieler doublette1, Spieler doublette2, Spieler tete) {

	public TripTeteAufstellung {
		checkNotNull(doublette1, "doublette1 == null");
		checkNotNull(doublette2, "doublette2 == null");
		checkNotNull(tete, "tete == null");
		checkArgument(!doublette1.equals(doublette2), "Doublette-Spieler müssen verschieden sein");
		checkArgument(!doublette1.equals(tete), "Tête-Spieler darf nicht im Doublette stehen");
		checkArgument(!doublette2.equals(tete), "Tête-Spieler darf nicht im Doublette stehen");
	}

	/**
	 * Baut eine Aufstellung für das gegebene Team. Der Spieler mit der Nummer {@code teteSpielerNr}
	 * wird als Tête nominiert, die übrigen zwei Spieler bilden das Doublette.
	 */
	public static TripTeteAufstellung fuerTeam(Team team, int teteSpielerNr) {
		checkNotNull(team, "team == null");
		List<Spieler> spielerList = team.spieler();
		checkArgument(spielerList.size() == 3, "Team muss genau 3 Spieler haben, hat aber %s", spielerList.size());

		Spieler teteSpieler = spielerList.stream()
				.filter(s -> s.getNr() == teteSpielerNr)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"Spieler Nr " + teteSpielerNr + " ist nicht im Team " + team.getNr()));

		List<Spieler> doublette = spielerList.stream()
				.filter(s -> !s.equals(teteSpieler))
				.toList();
		return new TripTeteAufstellung(doublette.get(0), doublette.get(1), teteSpieler);
	}

	public List<Spieler> doublette() {
		return List.of(doublette1, doublette2);
	}
}
