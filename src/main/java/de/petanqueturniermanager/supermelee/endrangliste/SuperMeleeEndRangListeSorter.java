/**
 * Erstellung 19.02.2023 / Michael Massee
 */
package de.petanqueturniermanager.supermelee.endrangliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.rangliste.IRangliste;
import de.petanqueturniermanager.helper.rangliste.RangListeSorter;
import de.petanqueturniermanager.helper.sheet.SortHelper;

/**
 * @author Michael Massee
 *
 */
public class SuperMeleeEndRangListeSorter extends RangListeSorter {

	/**
	 * @param iRanglisteSheet
	 */
	public SuperMeleeEndRangListeSorter(IRangliste iRanglisteSheet) {
		super(iRanglisteSheet);
	}

	/**
	 * wir muessen 4 Spalten sortieren, max 3 ist möglich<br>
	 * deswegen in 2 schritten
	 */
	@Override
	public void sortHelper(RangePosition toSortRange, int[] sortSpalten) throws GenerateException {
		checkNotNull(toSortRange);
		checkArgument(sortSpalten.length != 0, "Anzahl Sortspalten == 0 ");

		// wenn einzeln sortieren dann von letzte zu erste spalte
		// Zuerst die letzte 2 Spalten, Punkte plus

		SortHelper.from(getIRangliste(), toSortRange).abSteigendSortieren()
				.spaltenToSort(new int[] { sortSpalten[3], sortSpalten[4] }).doSort();

		// dann die restliche 3
		SortHelper.from(getIRangliste(), toSortRange).abSteigendSortieren()
				.spaltenToSort(new int[] { sortSpalten[0], sortSpalten[1], sortSpalten[2] }).doSort();
	}

}
