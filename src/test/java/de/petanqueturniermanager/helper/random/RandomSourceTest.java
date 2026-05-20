package de.petanqueturniermanager.helper.random;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class RandomSourceTest {

	@AfterEach
	public void resetRandom() {
		RandomSource.reset();
	}

	@Test
	public void testSetSeed_macht_nextInt_reproduzierbar() {
		RandomSource.setSeed(42L);
		List<Integer> ersterLauf = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			ersterLauf.add(RandomSource.nextInt(1_000));
		}

		RandomSource.setSeed(42L);
		List<Integer> zweiterLauf = new ArrayList<>();
		for (int i = 0; i < 20; i++) {
			zweiterLauf.add(RandomSource.nextInt(1_000));
		}

		assertThat(zweiterLauf).containsExactlyElementsOf(ersterLauf);
	}

	@Test
	public void testSetSeed_nextInt_originBound_reproduzierbarUndImBereich() {
		RandomSource.setSeed(7L);
		List<Integer> werte = new ArrayList<>();
		for (int i = 0; i < 50; i++) {
			werte.add(RandomSource.nextInt(10, 20));
		}
		assertThat(werte).allSatisfy(v -> assertThat(v).isBetween(10, 19));

		RandomSource.setSeed(7L);
		for (Integer erwartet : werte) {
			assertThat(RandomSource.nextInt(10, 20)).isEqualTo(erwartet);
		}
	}

	@Test
	public void testNextIntBound1_immerNull() {
		RandomSource.setSeed(1L);
		for (int i = 0; i < 10; i++) {
			assertThat(RandomSource.nextInt(1)).isZero();
		}
	}

	@Test
	public void testNextIntOriginGleichBoundMinus1_immerOrigin() {
		RandomSource.setSeed(1L);
		for (int i = 0; i < 10; i++) {
			assertThat(RandomSource.nextInt(5, 6)).isEqualTo(5);
		}
	}

	@Test
	public void testReset_entferntSeed_undNextIntFunktioniertWeiter() {
		RandomSource.setSeed(123L);
		int seededWert = RandomSource.nextInt(100);
		RandomSource.reset();

		// Nach reset: kein Throw, Werte im erlaubten Bereich
		for (int i = 0; i < 20; i++) {
			assertThat(RandomSource.nextInt(100)).isBetween(0, 99);
		}
		// Sanity: seededWert war ebenfalls im erlaubten Bereich
		assertThat(seededWert).isBetween(0, 99);
	}

	@Test
	public void testAsJavaRandom_seeded_reproduzierbar() {
		RandomSource.setSeed(99L);
		Random r1 = RandomSource.asJavaRandom();
		List<Integer> lauf1 = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			lauf1.add(r1.nextInt(100));
		}

		RandomSource.setSeed(99L);
		Random r2 = RandomSource.asJavaRandom();
		List<Integer> lauf2 = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			lauf2.add(r2.nextInt(100));
		}

		assertThat(lauf2).containsExactlyElementsOf(lauf1);
	}

	@Test
	public void testCollectionsShuffleMitAsJavaRandom_reproduzierbar() {
		List<Integer> liste1 = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		RandomSource.setSeed(2026L);
		Collections.shuffle(liste1, RandomSource.asJavaRandom());

		List<Integer> liste2 = new ArrayList<>(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
		RandomSource.setSeed(2026L);
		Collections.shuffle(liste2, RandomSource.asJavaRandom());

		assertThat(liste2).containsExactlyElementsOf(liste1);
	}

	@Test
	public void testThreadIsolation_unterschiedlicheSeeds_beeinflussenSichNicht() throws Exception {
		int werteProThread = 30;
		CountDownLatch start = new CountDownLatch(1);
		AtomicReference<List<Integer>> threadA = new AtomicReference<>();
		AtomicReference<List<Integer>> threadB = new AtomicReference<>();

		ExecutorService exec = Executors.newFixedThreadPool(2);
		try {
			exec.submit(() -> {
				RandomSource.setSeed(11L);
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				List<Integer> werte = new ArrayList<>();
				for (int i = 0; i < werteProThread; i++) {
					werte.add(RandomSource.nextInt(10_000));
				}
				threadA.set(werte);
				RandomSource.reset();
			});

			exec.submit(() -> {
				RandomSource.setSeed(22L);
				try {
					start.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					return;
				}
				List<Integer> werte = new ArrayList<>();
				for (int i = 0; i < werteProThread; i++) {
					werte.add(RandomSource.nextInt(10_000));
				}
				threadB.set(werte);
				RandomSource.reset();
			});

			start.countDown();
			exec.shutdown();
			assertThat(exec.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
		} finally {
			if (!exec.isTerminated()) {
				exec.shutdownNow();
			}
		}

		// Referenz: einthreadiger Lauf mit den gleichen Seeds liefert dieselben Sequenzen
		RandomSource.setSeed(11L);
		List<Integer> referenzA = new ArrayList<>();
		for (int i = 0; i < werteProThread; i++) {
			referenzA.add(RandomSource.nextInt(10_000));
		}
		RandomSource.setSeed(22L);
		List<Integer> referenzB = new ArrayList<>();
		for (int i = 0; i < werteProThread; i++) {
			referenzB.add(RandomSource.nextInt(10_000));
		}

		assertThat(threadA.get()).containsExactlyElementsOf(referenzA);
		assertThat(threadB.get()).containsExactlyElementsOf(referenzB);
		assertThat(threadA.get()).isNotEqualTo(threadB.get());
	}

	@Test
	public void testAsJavaRandom_ohneSeed_lieferRandom() {
		Random r = RandomSource.asJavaRandom();
		assertThat(r).isNotNull();
		// Smoke: liefert Werte im erlaubten Bereich
		assertThat(r.nextInt(100)).isBetween(0, 99);
	}
}
