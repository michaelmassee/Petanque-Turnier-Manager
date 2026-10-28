/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Mischt eine Liste loser Mêlée-Spieler zu Teams.
 * <p>
 * Zwei Arten der Team-Bildung:
 * <ul>
 * <li>{@link #bildeTeams(List, int)}: feste Team-Größe aus der Meldeliste-Formation des Zielsystems
 * (Doublette oder Triplette). Reicht die Spielerzahl nicht für eine ganze Anzahl Teams dieser
 * Größe, bleiben die überzähligen Spieler unangetastet offen stehen, bis weitere Spieler
 * dazukommen.</li>
 * <li>{@link #bildeTeamsOhneFreilos(List, SuperMeleeMode)}: gemischte Doubletten und Triplettes
 * nach derselben Aufteilung wie im Supermêlée ({@link SuperMeleeTeamRechner}) – die Teamzahl ist
 * dadurch immer gerade, es entsteht kein Freilos.</li>
 * </ul>
 * <p>
 * <b>Setzpositionen (SP):</b> Spieler mit identischer, von 0 verschiedener SP dürfen nicht im
 * selben Team landen (typischer Einsatz: alle Spieler eines Vereins bekommen dieselbe SP). Das neu
 * gebildete Team erhält als eigene SP die <b>niedrigste</b> von 0 verschiedene SP seiner Mitglieder,
 * sodass die bereits vorhandene SP-Logik der Zielsysteme unverändert weiterwirkt.
 * <p>
 * Die Klasse ist bewusst frei von UNO-Abhängigkeiten und damit direkt unit-testbar. Zufall läuft
 * ausschließlich über {@link RandomSource} (im Test per Seed reproduzierbar).
 */
public final class MeleeAnmeldungTeamBildner {

	/** Kleinste sinnvolle Team-Größe – darunter kann kein Team gebildet werden. */
	private static final int MIN_TEAM_GROESSE = 2;

	private static final int DOUBLETTE = 2;
	private static final int TRIPLETTE = 3;

	/** Weniger Spieler ergeben keine gerade Anzahl gemischter Teams (mindestens zwei Doubletten). */
	private static final int MIN_SPIELER_OHNE_FREILOS = 4;

	/**
	 * Maximale Anzahl Misch-Versuche, um die SP-Bedingung zu erfüllen, bevor sie fallen gelassen
	 * wird. Ohne diese Obergrenze könnte eine fachlich unerfüllbare Konstellation (z.B. alle
	 * Spieler mit derselben SP) zu einer Endlosschleife führen.
	 */
	private static final int MAX_MISCH_VERSUCHE = 200;

	/**
	 * Ein Mêlée-Spieler als Eingabe der Team-Bildung.
	 *
	 * @param zeile        0-basierter Zeilenindex im Mêlée-Anmeldung-Sheet (Rückverweis für den Aufrufer)
	 * @param nr           laufende Nummer der Anmeldung (nur für Nachvollziehbarkeit)
	 * @param vorname      Vorname
	 * @param nachname     Nachname
	 * @param setzPosition Setzposition; 0 = kein Setzstatus
	 */
	public record MeleeSpieler(int zeile, int nr, String vorname, String nachname, int setzPosition) {
	}

	/**
	 * Ein gebildetes Team.
	 *
	 * @param spieler      Mitglieder des Teams
	 * @param setzPosition abgeleitete Team-Setzposition (niedrigste SP &gt; 0 der Mitglieder, sonst 0)
	 */
	public record MeleeTeam(List<MeleeSpieler> spieler, int setzPosition) {

		public MeleeTeam {
			spieler = List.copyOf(spieler);
		}
	}

	private MeleeAnmeldungTeamBildner() {
	}

	/**
	 * Bildet aus den übergebenen Spielern so viele vollständige Teams der vorgegebenen Größe wie
	 * möglich. Reicht die Spielerzahl nicht für eine ganze Anzahl Teams, bleiben die überzähligen
	 * Spieler unverteilt (sie tauchen in keinem der zurückgegebenen Teams auf).
	 *
	 * Reicht die Spielerzahl nicht für eine ganze Anzahl Teams, bleiben bewusst die
	 * <b>letzten</b> Spieler in der übergebenen Reihenfolge unverteilt – bei der Mêlée-Anmeldung
	 * also die zuletzt eingecheckten. Welche Spieler einem Team zugeteilt werden, ist damit
	 * deterministisch; nur die Zusammensetzung der Teams selbst wird zufällig gemischt.
	 *
	 * @param spieler     zu verteilende Spieler in fester Reihenfolge (z.B. Zeilenreihenfolge im
	 *                    Mêlée-Anmeldung-Sheet); nur die vorderen Einträge werden bei einem nicht
	 *                    restlos teilbaren Rest berücksichtigt
	 * @param teamGroesse feste Team-Größe (Meldeliste-Formation: Doublette = 2, Triplette = 3)
	 * @return gebildete Teams; leere Liste wenn weniger als {@code teamGroesse} Spieler übergeben
	 *         wurden
	 */
	public static List<MeleeTeam> bildeTeams(List<MeleeSpieler> spieler, int teamGroesse) {
		checkNotNull(spieler);
		checkArgument(teamGroesse >= MIN_TEAM_GROESSE, "teamGroesse muss mindestens %s sein", MIN_TEAM_GROESSE);
		int anzTeams = spieler.size() / teamGroesse;
		if (anzTeams == 0) {
			return List.of();
		}
		List<MeleeSpieler> zuVerteilen = spieler.subList(0, anzTeams * teamGroesse);
		return verteile(zuVerteilen, Collections.nCopies(anzTeams, teamGroesse));
	}

	/**
	 * Bildet gemischte Doubletten und Triplettes mit gerader Teamzahl (kein Freilos). Die Aufteilung
	 * übernimmt {@link SuperMeleeTeamRechner} – identisch zur Supermêlée-Spielrunde:
	 * {@link SuperMeleeMode#Doublette} bevorzugt Doubletten und füllt mit Triplettes auf,
	 * {@link SuperMeleeMode#Triplette} umgekehrt.
	 * <p>
	 * Nicht jede Spielerzahl lässt sich so aufteilen (unter 4 und genau 7 Spieler). Dann wird die
	 * größte aufteilbare Anzahl verwendet; wie bei {@link #bildeTeams(List, int)} bleiben die
	 * <b>letzten</b> Spieler in der übergebenen Reihenfolge unverteilt.
	 *
	 * @param spieler zu verteilende Spieler in fester Reihenfolge
	 * @param mode    bevorzugte Team-Größe
	 * @return gebildete Teams aufsteigend nach Größe (Doubletten zuerst); leere Liste bei weniger als 4 Spielern
	 */
	public static List<MeleeTeam> bildeTeamsOhneFreilos(List<MeleeSpieler> spieler, SuperMeleeMode mode) {
		checkNotNull(spieler);
		checkNotNull(mode);
		int anzVerwendbar = anzahlVerwendbarOhneFreilos(spieler.size());
		if (anzVerwendbar == 0) {
			return List.of();
		}
		SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(anzVerwendbar, mode);
		// Aufsteigend nach Teamgröße wie die Supermêlée-Spielrunde: erst Doubletten, dann Triplettes.
		List<Integer> teamGroessen = new ArrayList<>(rechner.getAnzPaarungen());
		teamGroessen.addAll(Collections.nCopies(rechner.getAnzDoublette(), DOUBLETTE));
		teamGroessen.addAll(Collections.nCopies(rechner.getAnzTriplette(), TRIPLETTE));
		return verteile(spieler.subList(0, anzVerwendbar), teamGroessen);
	}

	/**
	 * @param anzSpieler vorhandene Spieler
	 * @return größte Spielerzahl &le; {@code anzSpieler}, die sich ohne Freilos in Doubletten und
	 *         Triplettes aufteilen lässt; 0 wenn das nicht möglich ist
	 */
	public static int anzahlVerwendbarOhneFreilos(int anzSpieler) {
		if (anzSpieler < MIN_SPIELER_OHNE_FREILOS) {
			return 0;
		}
		return new SuperMeleeTeamRechner(anzSpieler).valideAnzahlSpieler() ? anzSpieler : anzSpieler - 1;
	}

	/**
	 * Verteilt die Spieler zufällig auf die vorgegebenen Team-Größen und beachtet dabei die
	 * SP-Bedingung. Überzählige Spieler (mehr Spieler als die Summe der Team-Größen) bleiben
	 * unverteilt. Ist die SP-Bedingung nach {@value #MAX_MISCH_VERSUCHE} Versuchen nicht erfüllbar,
	 * wird die letzte Verteilung verwendet – die Auslosung scheitert bewusst nie hart.
	 */
	private static List<MeleeTeam> verteile(List<MeleeSpieler> spieler, List<Integer> teamGroessen) {
		List<MeleeTeam> letzteVerteilung = null;
		for (int versuch = 0; versuch < MAX_MISCH_VERSUCHE; versuch++) {
			List<MeleeSpieler> gemischt = new ArrayList<>(spieler);
			Collections.shuffle(gemischt, RandomSource.asJavaRandom());
			letzteVerteilung = schneideInTeams(gemischt, teamGroessen);
			if (alleTeamsErfuellenSetzPositionen(letzteVerteilung)) {
				return letzteVerteilung;
			}
		}
		return letzteVerteilung;
	}

	private static List<MeleeTeam> schneideInTeams(List<MeleeSpieler> gemischt, List<Integer> teamGroessen) {
		List<MeleeTeam> teams = new ArrayList<>(teamGroessen.size());
		int idx = 0;
		for (int groesse : teamGroessen) {
			int bis = Math.min(idx + groesse, gemischt.size());
			if (idx >= bis) {
				break;
			}
			List<MeleeSpieler> mitglieder = List.copyOf(gemischt.subList(idx, bis));
			teams.add(new MeleeTeam(mitglieder, teamSetzPosition(mitglieder)));
			idx = bis;
		}
		return teams;
	}

	/**
	 * @return niedrigste von 0 verschiedene SP der Mitglieder, sonst 0
	 */
	private static int teamSetzPosition(List<MeleeSpieler> mitglieder) {
		return mitglieder.stream().mapToInt(MeleeSpieler::setzPosition).filter(sp -> sp > 0).min().orElse(0);
	}

	private static boolean alleTeamsErfuellenSetzPositionen(List<MeleeTeam> teams) {
		return teams.stream().allMatch(MeleeAnmeldungTeamBildner::erfuelltSetzPositionen);
	}

	/** Ein Team ist gültig, solange keine SP &gt; 0 darin doppelt vorkommt. */
	private static boolean erfuelltSetzPositionen(MeleeTeam team) {
		Set<Integer> gesehen = new HashSet<>();
		for (MeleeSpieler spieler : team.spieler()) {
			if (spieler.setzPosition() > 0 && !gesehen.add(spieler.setzPosition())) {
				return false;
			}
		}
		return true;
	}
}
