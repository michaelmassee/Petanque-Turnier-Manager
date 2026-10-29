/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import static com.google.common.base.Preconditions.checkArgument;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

/**
 * Paart Teams in Ranglistenreihenfolge mit der kleinstmöglichen Zahl an Gegnerwiederholungen.
 * <p>
 * Die Teams werden von oben nach unten gepaart: Jedes noch freie Team bekommt den ersten Gegner laut
 * {@link GegnerWahl}, mit dem die kleinstmögliche Zahl an Wiederholungen für die ganze Runde erreichbar bleibt.
 * Ob ein Kandidat dazu führt, prüft ein maximales Matching über die noch nicht gespielten Paare (Edmonds-Blüten,
 * inkrementell augmentiert) exakt. Das Ergebnis entspricht einer vollständigen Rücksetz-Suche, ohne Suchgrenze.
 * <p>
 * Eine Paarung, die nur einen Tausch mit bereits gebildeten Paaren versucht, übersieht Lösungen, bei denen mehrere
 * Paare umgestellt werden müssen, oder bei denen das betroffene Paar ganz oben bzw. ganz unten steht – dort
 * entstanden vermeidbare Wiederholungen (Schweizer, Formule X, KO-Runde).
 */
public final class WiederholungsarmePaarung {

	/** In welcher Reihenfolge ein Team seine möglichen Gegner (die tiefer platzierten Teams) durchgeht. */
	public enum GegnerWahl {
		/** Nächstplatziertes Team zuerst (1–2, 3–4, …): Schweizer System, Formule X. */
		NAECHSTER_IN_RANGLISTE,
		/** Schlechtestplatziertes Team zuerst (1–n, 2–(n-1), …): KO-Runde „Erster gegen Letzten“. */
		LETZTER_IN_RANGLISTE
	}

	/**
	 * Eine Paarung als Index in die Rangliste.
	 *
	 * @param a      Index des besser platzierten Teams
	 * @param b      Index des Gegners
	 * @param wiederholung ob die beiden schon gegeneinander gespielt haben
	 */
	public record Paar(int a, int b, boolean wiederholung) {
	}

	private WiederholungsarmePaarung() {
	}

	/**
	 * @param anzahl   gerade Anzahl Teams, als Index 0..anzahl-1 in Ranglistenreihenfolge
	 * @param gespielt ob zwei Teams (Index) schon gegeneinander gespielt haben
	 * @param wahl     Reihenfolge der Gegnerkandidaten
	 * @return die Paare, nach dem besser platzierten Team aufsteigend
	 */
	public static List<Paar> paare(int anzahl, BiPredicate<Integer, Integer> gespielt, GegnerWahl wahl) {
		checkArgument(IsEvenOrOdd.isEven(anzahl), "Keine gerade Anzahl Teams: %s", anzahl);
		boolean[][] erlaubt = new boolean[anzahl][anzahl];
		for (int a = 0; a < anzahl; a++) {
			for (int b = 0; b < anzahl; b++) {
				erlaubt[a][b] = a != b && !gespielt.test(a, b) && !gespielt.test(b, a);
			}
		}
		boolean[] aktiv = new boolean[anzahl];
		Arrays.fill(aktiv, true);
		int[] partner = startMatching(erlaubt, aktiv);
		int groesse = (int) Arrays.stream(partner).filter(p -> p >= 0).count() / 2;

		List<Paar> paare = new ArrayList<>(anzahl / 2);
		for (int team = 0; team < anzahl; team++) {
			if (!aktiv[team]) {
				continue;
			}
			aktiv[team] = false;
			for (int gegner : kandidaten(team, anzahl, wahl)) {
				if (!aktiv[gegner]) {
					continue;
				}
				// Maximales Matching ohne team und gegner; mehr als groesse minus (team, gegner) ist nicht möglich.
				int[] neu = partner.clone();
				int neueGroesse = groesse;
				int ziel = groesse - (erlaubt[team][gegner] ? 1 : 0);
				aktiv[gegner] = false;
				for (int entfernt : new int[] { team, gegner }) {
					if (neu[entfernt] >= 0) {
						neu[neu[entfernt]] = -1;
						neu[entfernt] = -1;
						neueGroesse--;
					}
				}
				for (int frei = 0; frei < anzahl && neueGroesse < ziel; frei++) {
					if (aktiv[frei] && neu[frei] < 0 && augmentiere(frei, erlaubt, neu, aktiv)) {
						neueGroesse++;
					}
				}
				if (neueGroesse == ziel) {
					partner = neu;
					groesse = neueGroesse;
					paare.add(new Paar(team, gegner, !erlaubt[team][gegner]));
					break;
				}
				aktiv[gegner] = true;
			}
		}
		return paare;
	}

	private static int[] kandidaten(int team, int anzahl, GegnerWahl wahl) {
		return switch (wahl) {
			case NAECHSTER_IN_RANGLISTE -> IntStream.range(team + 1, anzahl).toArray();
			case LETZTER_IN_RANGLISTE -> IntStream.range(team + 1, anzahl).map(i -> anzahl + team - i).toArray();
		};
	}

	/**
	 * Maximales Matching als Ausgangspunkt. Gieriger Start in Ranglistenreihenfolge: meist ist der gesuchte Gegner dann
	 * schon Matching-Partner und die Prüfung kommt ohne Augmentierung aus.
	 */
	private static int[] startMatching(boolean[][] erlaubt, boolean[] aktiv) {
		int anzahl = erlaubt.length;
		int[] partner = new int[anzahl];
		Arrays.fill(partner, -1);
		for (int a = 0; a < anzahl; a++) {
			for (int b = a + 1; b < anzahl && partner[a] < 0; b++) {
				if (partner[b] < 0 && erlaubt[a][b]) {
					partner[a] = b;
					partner[b] = a;
				}
			}
		}
		for (int i = 0; i < anzahl; i++) {
			if (partner[i] < 0) {
				augmentiere(i, erlaubt, partner, aktiv);
			}
		}
		return partner;
	}

	/**
	 * Sucht vom freien Knoten {@code wurzel} einen augmentierenden Pfad (Edmonds-Blüten-Algorithmus) und erweitert
	 * das Matching entlang dieses Pfads.
	 *
	 * @param wurzel  freier, aktiver Knoten
	 * @param erlaubt Kanten (noch nicht gespielte Paare)
	 * @param partner Matching, {@code -1} = frei; wird bei Erfolg geändert
	 * @param aktiv   noch nicht gepaarte Knoten
	 * @return {@code true} wenn das Matching um eine Kante wuchs
	 */
	private static boolean augmentiere(int wurzel, boolean[][] erlaubt, int[] partner, boolean[] aktiv) {
		int n = partner.length;
		boolean[] besucht = new boolean[n];
		int[] vorgaenger = new int[n];
		Arrays.fill(vorgaenger, -1);
		int[] basis = IntStream.range(0, n).toArray();
		int[] warteschlange = new int[n];
		int kopf = 0;
		int ende = 0;
		besucht[wurzel] = true;
		warteschlange[ende++] = wurzel;
		while (kopf < ende) {
			int v = warteschlange[kopf++];
			for (int zu = 0; zu < n; zu++) {
				if (!aktiv[zu] || !erlaubt[v][zu] || basis[v] == basis[zu] || partner[v] == zu) {
					continue;
				}
				if (zu == wurzel || (partner[zu] >= 0 && vorgaenger[partner[zu]] >= 0)) {
					int bluetenBasis = gemeinsameBasis(v, zu, basis, partner, vorgaenger);
					boolean[] bluete = new boolean[n];
					markierePfad(v, bluetenBasis, zu, bluete, basis, partner, vorgaenger);
					markierePfad(zu, bluetenBasis, v, bluete, basis, partner, vorgaenger);
					for (int i = 0; i < n; i++) {
						if (aktiv[i] && bluete[basis[i]]) {
							basis[i] = bluetenBasis;
							if (!besucht[i]) {
								besucht[i] = true;
								warteschlange[ende++] = i;
							}
						}
					}
				} else if (vorgaenger[zu] < 0) {
					vorgaenger[zu] = v;
					if (partner[zu] < 0) {
						for (int u = zu; u >= 0;) {
							int pv = vorgaenger[u];
							int naechster = partner[pv];
							partner[u] = pv;
							partner[pv] = u;
							u = naechster;
						}
						return true;
					}
					besucht[partner[zu]] = true;
					warteschlange[ende++] = partner[zu];
				}
			}
		}
		return false;
	}

	private static int gemeinsameBasis(int a, int b, int[] basis, int[] partner, int[] vorgaenger) {
		boolean[] gesehen = new boolean[partner.length];
		while (true) {
			a = basis[a];
			gesehen[a] = true;
			if (partner[a] < 0) {
				break;
			}
			a = vorgaenger[partner[a]];
		}
		while (true) {
			b = basis[b];
			if (gesehen[b]) {
				return b;
			}
			b = vorgaenger[partner[b]];
		}
	}

	private static void markierePfad(int v, int bluetenBasis, int kind, boolean[] bluete, int[] basis, int[] partner,
			int[] vorgaenger) {
		while (basis[v] != bluetenBasis) {
			bluete[basis[v]] = true;
			bluete[basis[partner[v]]] = true;
			vorgaenger[v] = kind;
			kind = partner[v];
			v = vorgaenger[partner[v]];
		}
	}
}
