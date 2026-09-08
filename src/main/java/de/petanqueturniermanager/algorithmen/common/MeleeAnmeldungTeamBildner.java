/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.petanqueturniermanager.helper.random.RandomSource;
import de.petanqueturniermanager.supermelee.SuperMeleeTeamRechner;
import de.petanqueturniermanager.supermelee.konfiguration.SuperMeleeMode;

/**
 * Mischt eine Liste loser Melee-Spieler zu Teams (Doublette/Triplette-Mix).
 * <p>
 * Die Team-Größen ergeben sich aus {@link SuperMeleeTeamRechner} – identisch zur Supermêlée-Logik:
 * im Modus {@link SuperMeleeMode#Triplette} werden Tripletten bevorzugt und mit Doubletten
 * aufgefüllt, im Modus {@link SuperMeleeMode#Doublette} umgekehrt.
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
	private static final int MIN_ANZ_SPIELER = 2;

	/**
	 * Maximale Anzahl Misch-Versuche, um die SP-Bedingung zu erfüllen, bevor sie fallen gelassen
	 * wird. Ohne diese Obergrenze könnte eine fachlich unerfüllbare Konstellation (z.B. alle
	 * Spieler mit derselben SP) zu einer Endlosschleife führen.
	 */
	private static final int MAX_MISCH_VERSUCHE = 200;

	/**
	 * Kleinstfelder, für die die Formeln des {@link SuperMeleeTeamRechner} kein gültiges Ergebnis
	 * liefern, fachlich aber eindeutig aufteilbar sind.
	 */
	private static final Map<Integer, List<Integer>> KLEINE_FELDER = Map.of(
			2, List.of(2),
			3, List.of(3),
			7, List.of(3, 2, 2));

	/**
	 * Ein Melee-Spieler als Eingabe der Team-Bildung.
	 *
	 * @param nr           laufende Nummer der Anmeldung (nur für Nachvollziehbarkeit)
	 * @param vorname      Vorname
	 * @param nachname     Nachname
	 * @param setzPosition Setzposition; 0 = kein Setzstatus
	 */
	public record MeleeSpieler(int nr, String vorname, String nachname, int setzPosition) {
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
	 * Bildet aus den übergebenen Spielern Teams.
	 *
	 * @param spieler zu verteilende Spieler (Reihenfolge ist unerheblich)
	 * @param modus   bevorzugte Team-Größe
	 * @return gebildete Teams; leere Liste wenn weniger als {@value #MIN_ANZ_SPIELER} Spieler
	 *         übergeben wurden
	 */
	public static List<MeleeTeam> bildeTeams(List<MeleeSpieler> spieler, SuperMeleeMode modus) {
		checkNotNull(spieler);
		checkNotNull(modus);
		if (spieler.size() < MIN_ANZ_SPIELER) {
			return List.of();
		}
		List<Integer> teamGroessen = teamGroessen(spieler.size(), modus);
		return verteile(spieler, teamGroessen);
	}

	/**
	 * Ermittelt die Team-Größen für die gegebene Spielerzahl.
	 * <p>
	 * Für die üblichen Feldgrößen liefert der {@link SuperMeleeTeamRechner} die Aufteilung – damit
	 * verhält sich die Melee-Anmeldung identisch zur bekannten Supermêlée-Auslosung (inkl. dessen
	 * bevorzugt gerader Teamanzahl).
	 * <p>
	 * Seine Formeln setzen allerdings ein hinreichend großes Feld voraus und liefern für 2, 3 und 7
	 * Spieler eine negative Team-Anzahl ({@code valideAnzahlSpieler()} meldet nur die 7). Diese
	 * Kleinstfelder werden daher direkt aufgeteilt; zusätzlich prüft
	 * {@link #istPlausibel(SuperMeleeTeamRechner, int)} das Ergebnis defensiv gegen die
	 * Spielerzahl.
	 */
	private static List<Integer> teamGroessen(int anzSpieler, SuperMeleeMode modus) {
		List<Integer> kleinesFeld = KLEINE_FELDER.get(anzSpieler);
		if (kleinesFeld != null) {
			return kleinesFeld;
		}
		SuperMeleeTeamRechner rechner = new SuperMeleeTeamRechner(anzSpieler, modus);
		if (!istPlausibel(rechner, anzSpieler)) {
			// Defensive Absicherung: lieber ein einzelnes Team bilden als die Anmeldungen
			// kommentarlos zu verwerfen.
			return List.of(anzSpieler);
		}
		List<Integer> groessen = new ArrayList<>();
		for (int i = 0; i < rechner.getAnzTriplette(); i++) {
			groessen.add(3);
		}
		for (int i = 0; i < rechner.getAnzDoublette(); i++) {
			groessen.add(2);
		}
		return groessen;
	}

	private static boolean istPlausibel(SuperMeleeTeamRechner rechner, int anzSpieler) {
		return rechner.getAnzTriplette() >= 0 && rechner.getAnzDoublette() >= 0
				&& rechner.getAnzTriplette() * 3 + rechner.getAnzDoublette() * 2 == anzSpieler;
	}

	/**
	 * Verteilt die Spieler zufällig auf die vorgegebenen Team-Größen und beachtet dabei die
	 * SP-Bedingung. Ist sie nach {@value #MAX_MISCH_VERSUCHE} Versuchen nicht erfüllbar, wird die
	 * letzte Verteilung verwendet – die Auslosung scheitert bewusst nie hart.
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
		List<Integer> gesetzte = team.spieler().stream().map(MeleeSpieler::setzPosition).filter(sp -> sp > 0).toList();
		return gesetzte.size() == gesetzte.stream().distinct().count();
	}
}
