package de.petanqueturniermanager.helper.random;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Zentrale Random-Quelle für den gesamten Produktivcode.
 *
 * <p>Im Produktivbetrieb verhält sich diese Klasse wie ein direkter Aufruf von
 * {@link ThreadLocalRandom#current()}: jeder Lauf liefert nicht-reproduzierbare
 * Zufallswerte. In UI-Tests kann per {@link #setSeed(long)} ein fester Seed
 * gesetzt werden, um reproduzierbare Test-Daten gegen JSON-Referenzdateien zu
 * validieren. {@link #reset()} stellt das Default-Verhalten wieder her.
 *
 * <p>Die Seed-Konfiguration ist Thread-lokal, damit parallele Tests sich nicht
 * gegenseitig beeinflussen.
 */
public final class RandomSource {

	private static final ThreadLocal<Random> SEEDED = new ThreadLocal<>();

	private RandomSource() {
		// utility
	}

	/**
	 * Setzt einen festen Seed für den aktuellen Thread. Nur in Tests verwenden.
	 */
	public static void setSeed(long seed) {
		SEEDED.set(new Random(seed));
	}

	/**
	 * Entfernt den festgesetzten Seed des aktuellen Threads. Nach dem Aufruf
	 * verhält sich {@link RandomSource} wieder wie {@link ThreadLocalRandom}.
	 */
	public static void reset() {
		SEEDED.remove();
	}

	/**
	 * @return Zufalls-Int im Bereich {@code [0, bound)}.
	 */
	public static int nextInt(int bound) {
		Random seeded = SEEDED.get();
		return seeded != null ? seeded.nextInt(bound) : ThreadLocalRandom.current().nextInt(bound);
	}

	/**
	 * @return Zufalls-Int im Bereich {@code [origin, bound)}.
	 */
	public static int nextInt(int origin, int bound) {
		Random seeded = SEEDED.get();
		return seeded != null ? origin + seeded.nextInt(bound - origin)
				: ThreadLocalRandom.current().nextInt(origin, bound);
	}

	/**
	 * Liefert eine {@link Random}-Instanz für APIs wie
	 * {@link java.util.Collections#shuffle(java.util.List, Random)}, die eine
	 * konkrete {@link Random}-Referenz erwarten.
	 */
	public static Random asJavaRandom() {
		Random seeded = SEEDED.get();
		return seeded != null ? seeded : ThreadLocalRandom.current();
	}
}
