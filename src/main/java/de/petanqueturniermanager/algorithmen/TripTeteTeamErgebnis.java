/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

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
 *   <li>Kugel-Differenz (Tiebreak 2)</li>
 *   <li>Erzielte Kugeln (Σ+) (Tiebreak 3)</li>
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
			.thenComparing(Comparator.comparingInt(TripTeteTeamErgebnis::getKugelDiff).reversed())
			.thenComparing(Comparator.comparingInt(TripTeteTeamErgebnis::getKugelnPlus).reversed());

	private final Team team;
	private int begegnungenGespielt;
	private int begegnungenGewonnen;
	private int begegnungenVerloren;
	private int begegnungenUnentschieden;
	private int partienGewonnen;
	private int partienVerloren;
	private int kugelnPlus;
	private int kugelnMinus;

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
		int eigeneKugeln = istTeamA ? ergebnis.kugelnFuerA() : ergebnis.kugelnGegenA();
		int gegnerKugeln = istTeamA ? ergebnis.kugelnGegenA() : ergebnis.kugelnFuerA();

		begegnungenGespielt++;
		partienGewonnen += eigenePartien;
		partienVerloren += gegnerPartien;
		kugelnPlus += eigeneKugeln;
		kugelnMinus += gegnerKugeln;

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

	public int getKugelnPlus() {
		return kugelnPlus;
	}

	public int getKugelnMinus() {
		return kugelnMinus;
	}

	public int getKugelDiff() {
		return kugelnPlus - kugelnMinus;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("team", team.getNr())
				.add("begSiege", begegnungenGewonnen)
				.add("partSiege", partienGewonnen)
				.add("kugelDiff", getKugelDiff())
				.add("kugelnPlus", kugelnPlus)
				.toString();
	}
}
