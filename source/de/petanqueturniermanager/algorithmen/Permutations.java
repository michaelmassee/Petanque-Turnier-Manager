/**
* Erstellung : 03.09.2017 / Michael Massee
**/

package de.petanqueturniermanager.algorithmen;

import java.util.Collection;
import java.util.Iterator;

import com.google.common.collect.AbstractIterator;

import de.petanqueturniermanager.model.Spieler;

class Permutations extends AbstractIterator<Spieler[]> implements Iterable<Spieler[]> {
	private final static int NO_SUCH_INDEX_EXISTS = -1;

	private Spieler[] items;
	private Spieler[] currentPermutation;
	private boolean firstIteration;

	public Permutations(Collection<Spieler> items) {
		this.items = items.toArray(new Spieler[items.size()]);
		this.firstIteration = true;
	}

	@Override
	protected Spieler[] computeNext() {
		/*
		 * http://en.wikipedia.org/wiki/Permutation Systematic generation of all permutations
		 */
		// we simply return the items we just sorted as current permutation
		if (firstIteration) {
			currentPermutation = items.clone();
			firstIteration = false;
		} else {
			// such that items[k] < items[k + 1]
			int k = findLargestIndexK();

			switch (k) {

			case NO_SUCH_INDEX_EXISTS:
				currentPermutation = endOfData();
				break;

			default:
				// such that items[k] items[l]
				int l = findLargestIndexL(k);
				swap(k, l);
				reverseTailFrom(k);

			}
		}
		return currentPermutation;
	}

	private int findLargestIndexK() {
		int k = -1;
		for (int i = 0; i < currentPermutation.length - 1; i++) {
			if (firstIsSmaller(currentPermutation[i], currentPermutation[i + 1])) {
				k = i;
			}
		}
		return k;
	}

	private int findLargestIndexL(int k) {
		int l = -1;
		for (int i = k + 1; i < currentPermutation.length; i++) {
			if (firstIsSmaller(currentPermutation[k], currentPermutation[i])) {
				l = i;
			}
		}
		return l;
	}

	private void swap(int i, int j) {
		Spieler tmp = currentPermutation[i];
		currentPermutation[i] = currentPermutation[j];
		currentPermutation[j] = tmp;
	}

	private void reverseTailFrom(int k) {
		for (int i = k + 1, j = currentPermutation.length - 1; i < j; i++, j--) {
			swap(i, j);
		}
	}

	@SuppressWarnings("unchecked")
	private boolean firstIsSmaller(Object first, Object second) {
		return ((Comparable<Object>) first).compareTo(second) < 0;
	}

	@Override
	public Iterator<Spieler[]> iterator() {
		return this;
	}
}