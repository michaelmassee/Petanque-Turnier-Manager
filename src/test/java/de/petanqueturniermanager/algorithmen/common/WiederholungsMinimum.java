/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

/**
 * Test-Hilfe für Paarungsalgorithmen: berechnet per vollständiger Suche die kleinstmögliche Zahl an
 * Gegnerwiederholungen einer Runde und erzeugt zufällige, realistische Gegner-Historien.
 * <p>
 * Die Suche probiert alle vollständigen Paarungen durch – nur für kleine Teamzahlen (bis etwa 12) gedacht.
 */
public final class WiederholungsMinimum {

	private WiederholungsMinimum() {
	}

	/**
	 * @param anzahl   gerade Anzahl Teams, als Index 0..anzahl-1
	 * @param gespielt ob zwei Teams (Index) schon gegeneinander gespielt haben
	 * @return kleinstmögliche Zahl an Paarungen, deren Teams schon gegeneinander gespielt haben
	 */
	public static int berechne(int anzahl, BiPredicate<Integer, Integer> gespielt) {
		return suche(new boolean[anzahl], gespielt);
	}

	private static int suche(boolean[] vergeben, BiPredicate<Integer, Integer> gespielt) {
		int erstes = IntStream.range(0, vergeben.length).filter(i -> !vergeben[i]).findFirst().orElse(-1);
		if (erstes < 0) {
			return 0;
		}
		vergeben[erstes] = true;
		int minimum = Integer.MAX_VALUE;
		for (int gegner = erstes + 1; gegner < vergeben.length; gegner++) {
			if (!vergeben[gegner]) {
				vergeben[gegner] = true;
				int kosten = (gespielt.test(erstes, gegner) ? 1 : 0) + suche(vergeben, gespielt);
				minimum = Math.min(minimum, kosten);
				vergeben[gegner] = false;
			}
		}
		vergeben[erstes] = false;
		return minimum;
	}

	/**
	 * Erzeugt eine Gegner-Historie aus {@code runden} zufällig ausgelosten, vollständigen Runden (Wiederholungen
	 * innerhalb der Historie sind möglich, wie in einem echten Turnier mit erzwungener Wiederholung).
	 *
	 * @return {@code gespielt[a][b]} für Index a, b
	 */
	public static boolean[][] zufaelligeHistorie(int anzahl, int runden, Random random) {
		boolean[][] gespielt = new boolean[anzahl][anzahl];
		List<Integer> reihe = new ArrayList<>(IntStream.range(0, anzahl).boxed().toList());
		for (int runde = 0; runde < runden; runde++) {
			Collections.shuffle(reihe, random);
			for (int i = 0; i + 1 < anzahl; i += 2) {
				gespielt[reihe.get(i)][reihe.get(i + 1)] = true;
				gespielt[reihe.get(i + 1)][reihe.get(i)] = true;
			}
		}
		return gespielt;
	}
}
