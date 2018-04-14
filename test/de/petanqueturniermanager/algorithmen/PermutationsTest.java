/**
* Erstellung : 09.09.2017 / Michael Massee
**/

package de.petanqueturniermanager.algorithmen;

import java.util.ArrayList;

import org.junit.Test;

import de.petanqueturniermanager.algorithmen.Permutations;
import de.petanqueturniermanager.model.Spieler;

public class PermutationsTest {

	@Test
	public void testPermutations() throws Exception {

		ArrayList<Spieler> spieler = new ArrayList<>();

		for (int i = 1; i < 10; i++) {
			spieler.add(new Spieler(i));
		}

		int anzahl = 1;

		for (Spieler[] spList : new Permutations(spieler)) {
			for (Spieler sp : spList) {
				System.out.print(sp.getNr());
			}
			System.out.println();
			anzahl++;
		}
		System.out.println("---" + anzahl);

	}
}
