/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.triptete;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Comparator;

import com.google.common.base.MoreObjects;

import de.petanqueturniermanager.model.Team;

/**
 * Aggregierter Turnier-Stand eines Teams für die Trip-Tête-Rangliste.
 * <p>
 * Sortierordnung (alle absteigend):
 * <ol>
 *   <li>Begegnungssiege (Hauptkriterium)</li>
 *   <li>Partiensiege (Tiebreak 1)</li>
 *   <li>Erzielte Spielpunkte (Σ+) (Tiebreak 2)</li>
 *   <li>Spielpunkte-Differenz Δ (Tiebreak 3)</li>
 * </ol>
 * <p>
 * Hinweis: {@code compareTo} definiert nur die Sortierung; {@code equals} bleibt absichtlich
 * identity-basiert (siehe {@code config/spotbugs/exclude.xml} → {@code EQ_COMPARETO_USE_OBJECT_EQUALS}).
 *
 * @author Michael Massee
 */
public class TripTeteTeamErgebnis implements Comparable<TripTeteTeamErgebnis> {

	private static final Comparator<TripTeteTeamErgebnis> SORTIERUNG = Comparator
			.comparingInt(TripTeteTeamErgebnis::getBegegnungenGewonnen).reversed()
			.thenComparing(Comparator.comparingInt(TripTeteTeamErgebnis::getPartienGewonnen).reversed())
			.thenComparing(Comparator.comparingInt(TripTeteTeamErgebnis::getSpielPunktePlus).reversed())
			.thenComparing(Comparator.comparingInt(TripTeteTeamErgebnis::getSpielPunkteDiff).reversed());

	private final Team team;
	private int begegnungenGespielt;
	private int begegnungenGewonnen;
	private int begegnungenVerloren;
	private int begegnungenUnentschieden;
	private int partienGewonnen;
	private int partienVerloren;
	private int spielpunktePlus;
	private int spielpunkteMinus;

	public TripTeteTeamErgebnis(Team team) {
		this.team = checkNotNull(team, "team == null");
	}

	/**
	 * Verbucht eine Begegnung aus Sicht dieses Teams.
	 *
	 * @param istTeamA               {@code true} wenn dieses Team in der Begegnung in der A-Rolle war
	 * @param ergebnis               das Begegnungs-Ergebnis (muss vollständig sein)
	 */
	public TripTeteTeamErgebnis verbucheBegegnung(boolean istTeamA, TripTeteBegegnungErgebnis ergebnis) {
		checkNotNull(ergebnis, "ergebnis == null");
		if (!ergebnis.istVollstaendig()) {
			throw new IllegalArgumentException("Begegnung ist nicht vollständig");
		}

		int eigenePartien = istTeamA ? ergebnis.begegnungPunkteA() : ergebnis.begegnungPunkteB();
		int gegnerPartien = istTeamA ? ergebnis.begegnungPunkteB() : ergebnis.begegnungPunkteA();
		int eigeneKugeln = istTeamA ? ergebnis.spielpunkteFuerA() : ergebnis.spielpunkteGegenA();
		int gegnerKugeln = istTeamA ? ergebnis.spielpunkteGegenA() : ergebnis.spielpunkteFuerA();

		begegnungenGespielt++;
		partienGewonnen += eigenePartien;
		partienVerloren += gegnerPartien;
		spielpunktePlus += eigeneKugeln;
		spielpunkteMinus += gegnerKugeln;

		if (eigenePartien > gegnerPartien) {
			begegnungenGewonnen++;
		} else if (eigenePartien < gegnerPartien) {
			begegnungenVerloren++;
		} else {
			begegnungenUnentschieden++;
		}
		return this;
	}

	@Override
	public int compareTo(TripTeteTeamErgebnis o) {
		return SORTIERUNG.compare(this, o);
	}

	public Team getTeam() {
		return team;
	}

	public int getBegegnungenGespielt() {
		return begegnungenGespielt;
	}

	public int getBegegnungenGewonnen() {
		return begegnungenGewonnen;
	}

	public int getBegegnungenVerloren() {
		return begegnungenVerloren;
	}

	public int getBegegnungenUnentschieden() {
		return begegnungenUnentschieden;
	}

	public int getPartienGewonnen() {
		return partienGewonnen;
	}

	public int getPartienVerloren() {
		return partienVerloren;
	}

	public int getSpielPunktePlus() {
		return spielpunktePlus;
	}

	public int getSpielPunkteMinus() {
		return spielpunkteMinus;
	}

	public int getSpielPunkteDiff() {
		return spielpunktePlus - spielpunkteMinus;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("team", team.getNr())
				.add("begSiege", begegnungenGewonnen)
				.add("partSiege", partienGewonnen)
				.add("spielpunkteDiff", getSpielPunkteDiff())
				.add("spielpunktePlus", spielpunktePlus)
				.toString();
	}
}
