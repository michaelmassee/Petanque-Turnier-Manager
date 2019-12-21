/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

/**
 * @author Michael Massee
 *
 */
public class IsEvenOrOdd {

	// https://stackoverflow.com/questions/7342237/check-whether-number-is-even-or-odd/7342253
	public static boolean IsEven(int nmbr) {
		return ((nmbr & 1) == 0);
	}

	public static boolean IsOdd(int nmbr) {
		return !IsEvenOrOdd.IsEven(nmbr);
	}
}
