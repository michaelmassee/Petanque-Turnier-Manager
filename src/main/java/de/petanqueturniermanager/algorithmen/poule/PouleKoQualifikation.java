/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.poule;

import static java.util.Objects.requireNonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Verteilt die Teams der sortierten Poule-Gruppen auf A- und B-Turnier.
 * <p>
 * Platz 1 und 2 jeder Gruppe spielen das A-Turnier, Platz 3 und 4 das B-Turnier. Gezählt werden nur Teams, die
 * noch teilnehmen: Ist ein Team ausgestiegen, rücken die Teams dahinter in seiner Gruppe nach (z. B. der
 * Gruppendritte ins A-Turnier, wenn ein Gruppensieger aussteigt). Die Vorrunden-Ergebnisse und damit die
 * Gruppenreihenfolge bleiben unverändert.
 * <p>
 * Setzung (Cross-Seeding über Gruppenpaare): erst die Gruppenersten in Gruppenreihenfolge, dann die Gruppenzweiten in
 * umgekehrter Reihenfolge – entsprechend für Dritte und Vierte im B-Turnier. So treffen benachbarte Teams im
 * Turnierbaum aus verschiedenen Gruppen aufeinander.
 */
public final class PouleKoQualifikation {

	/** Turnier, das ein Team nach der Vorrunde spielt. */
	public enum Turnier {
		A, B, KEINES
	}

	private static final int ERSTER_PLATZ_B_TURNIER = 2;
	private static final int ERSTER_PLATZ_OHNE_TURNIER = 4;

	private final List<List<Integer>> teilnehmendeGruppen;

	private PouleKoQualifikation(List<List<Integer>> teilnehmendeGruppen) {
		this.teilnehmendeGruppen = teilnehmendeGruppen;
	}

	/**
	 * @param sortierteGruppen    Gruppen in Gruppenreihenfolge, jede nach Vorrunden-Platz sortiert
	 * @param teilnehmendeTeamNrn Teams, die noch teilnehmen (Aktiv = 1)
	 */
	public static PouleKoQualifikation von(List<List<PouleTeamErgebnis>> sortierteGruppen,
			Set<Integer> teilnehmendeTeamNrn) {
		requireNonNull(sortierteGruppen, "sortierteGruppen");
		requireNonNull(teilnehmendeTeamNrn, "teilnehmendeTeamNrn");
		List<List<Integer>> gruppen = sortierteGruppen.stream()
				.map(gruppe -> gruppe.stream().map(PouleTeamErgebnis::teamNr).filter(teilnehmendeTeamNrn::contains)
						.toList())
				.toList();
		return new PouleKoQualifikation(gruppen);
	}

	/** @return Turnier des Teams; {@link Turnier#KEINES} für ausgestiegene und unbekannte Teams */
	public Turnier turnier(int teamNr) {
		for (List<Integer> gruppe : teilnehmendeGruppen) {
			int platzIndex = gruppe.indexOf(teamNr);
			if (platzIndex >= 0) {
				return turnierFuerPlatzIndex(platzIndex);
			}
		}
		return Turnier.KEINES;
	}

	/** @return Teamnummern des A-Turniers in Setzreihenfolge */
	public List<Integer> aTeams() {
		return gesetzt(0);
	}

	/** @return Teamnummern des B-Turniers in Setzreihenfolge */
	public List<Integer> bTeams() {
		return gesetzt(ERSTER_PLATZ_B_TURNIER);
	}

	private static Turnier turnierFuerPlatzIndex(int platzIndex) {
		if (platzIndex < ERSTER_PLATZ_B_TURNIER) {
			return Turnier.A;
		}
		return platzIndex < ERSTER_PLATZ_OHNE_TURNIER ? Turnier.B : Turnier.KEINES;
	}

	/**
	 * Erst Platz {@code ersterPlatzIndex} aller Gruppen in Gruppenreihenfolge, dann der folgende Platz in umgekehrter
	 * Gruppenreihenfolge.
	 */
	private List<Integer> gesetzt(int ersterPlatzIndex) {
		var teams = new ArrayList<Integer>();
		for (List<Integer> gruppe : teilnehmendeGruppen) {
			if (gruppe.size() > ersterPlatzIndex) {
				teams.add(gruppe.get(ersterPlatzIndex));
			}
		}
		int zweiterPlatzIndex = ersterPlatzIndex + 1;
		for (List<Integer> gruppe : teilnehmendeGruppen.reversed()) {
			if (gruppe.size() > zweiterPlatzIndex) {
				teams.add(gruppe.get(zweiterPlatzIndex));
			}
		}
		return List.copyOf(teams);
	}
}
