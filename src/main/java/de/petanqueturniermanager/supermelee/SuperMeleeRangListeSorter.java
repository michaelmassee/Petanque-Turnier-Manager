/**
 * Erstellung 25.12.2019 / Michael Massee
 */
package de.petanqueturniermanager.supermelee;

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
public class SuperMeleeRangListeSorter extends RangListeSorter {

	/**
	 * @param iRanglisteSheet
	 */
	public SuperMeleeRangListeSorter(IRangliste iRanglisteSheet) {
		super(iRanglisteSheet);
	}

	/**
	 * wir muessen 4 Spalten sortieren, max 3 ist möglich<br>
	 * deswegen in 2 schritten
	 */
	@Override
	public void sortHelper(RangePosition toSortRange, int[] sortSpalten) throws GenerateException {
		checkNotNull(toSortRange);
		checkArgument(sortSpalten.length == 4, "Anzahl Sortspalten ist nicht 4");

		// wenn einzaln sortieren dann von letzte zu erste spalte
		// Zuerst die letzte Spalte, Punkte plus
		SortHelper.from(getIRangliste(), toSortRange).abSteigendSortieren().spaltenToSort(new int[] { sortSpalten[3] }).doSort();
		// dann die restliche 3
		SortHelper.from(getIRangliste(), toSortRange).abSteigendSortieren().spaltenToSort(new int[] { sortSpalten[0], sortSpalten[1], sortSpalten[2] }).doSort();
	}

}
