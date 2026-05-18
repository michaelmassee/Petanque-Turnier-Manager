/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.model.SpielErgebnis;
import de.petanqueturniermanager.model.Team;

/**
 * Wertung einer kompletten Trip-Tête-Begegnung zwischen zwei Teams.
 * <p>
 * Eine Begegnung besteht aus drei Partien ({@link TripTetePartie#TRIPLETTE},
 * {@link TripTetePartie#DOUBLETTE}, {@link TripTetePartie#TETE}). Jede gewonnene Partie zählt
 * einen Begegnungspunkt; Sieger der Begegnung ist, wer ≥ 2 Partien gewinnt (Best-of-3).
 *
 * @author Michael Massee
 */
public class TripTeteBegegnungErgebnis {

	private final Team teamA;
	private final Team teamB;
	private final Map<TripTetePartie, SpielErgebnis> partien = new EnumMap<>(TripTetePartie.class);

	public TripTeteBegegnungErgebnis(Team teamA, Team teamB) {
		this.teamA = checkNotNull(teamA, "teamA == null");
		this.teamB = checkNotNull(teamB, "teamB == null");
	}

	public TripTeteBegegnungErgebnis setPartieErgebnis(TripTetePartie partie, SpielErgebnis ergebnis) {
		checkNotNull(partie, "partie == null");
		checkNotNull(ergebnis, "ergebnis == null");
		partien.put(partie, ergebnis);
		return this;
	}

	public Optional<SpielErgebnis> getPartieErgebnis(TripTetePartie partie) {
		return Optional.ofNullable(partien.get(checkNotNull(partie, "partie == null")));
	}

	public boolean istVollstaendig() {
		return partien.size() == TripTetePartie.values().length;
	}

	public int begegnungPunkteA() {
		return (int) partien.values().stream().filter(SpielErgebnis::siegA).count();
	}

	public int begegnungPunkteB() {
		return (int) partien.values().stream().filter(SpielErgebnis::siegB).count();
	}

	public int kugelnFuerA() {
		return partien.values().stream().mapToInt(SpielErgebnis::getSpielPunkteA).sum();
	}

	public int kugelnGegenA() {
		return partien.values().stream().mapToInt(SpielErgebnis::getSpielPunkteB).sum();
	}

	public int kugelDiffA() {
		return kugelnFuerA() - kugelnGegenA();
	}

	public boolean siegA() {
		return istVollstaendig() && begegnungPunkteA() > begegnungPunkteB();
	}

	public boolean siegB() {
		return istVollstaendig() && begegnungPunkteB() > begegnungPunkteA();
	}

	/**
	 * @return {@code true} wenn die Begegnung vollständig ist und keine Seite ≥ 2 Partien gewonnen hat
	 *         (in der Regel unmöglich bei 3 Partien ohne Unentschieden — kann nur entstehen, wenn
	 *         eine Partie selbst unentschieden endet).
	 */
	public boolean unentschieden() {
		return istVollstaendig() && begegnungPunkteA() == begegnungPunkteB();
	}

	public Optional<Team> sieger() {
		if (siegA()) {
			return Optional.of(teamA);
		}
		if (siegB()) {
			return Optional.of(teamB);
		}
		return Optional.empty();
	}

	public Team getTeamA() {
		return teamA;
	}

	public Team getTeamB() {
		return teamB;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("A", teamA.getNr())
				.add("B", teamB.getNr())
				.add("punkteA", begegnungPunkteA())
				.add("punkteB", begegnungPunkteB())
				.add("kugelDiffA", kugelDiffA())
				.toString();
	}
}
