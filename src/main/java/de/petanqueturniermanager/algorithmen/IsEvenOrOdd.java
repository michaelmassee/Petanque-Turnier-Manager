/**
 * Erstellung 24.05.2019 / Michael Massee
 */
package de.petanqueturniermanager.algorithmen;

/**
 * Hilfsmethoden zur Prüfung ob eine Ganzzahl gerade oder ungerade ist.<br>
 * <br>
 * Wird in der Turnierlogik an mehreren Stellen benötigt:<br>
 * <ul>
 *   <li><b>Ungerade Teamanzahl</b> (Schweizer System, Jeder-gegen-Jeden): ein Team bekommt
 *       ein Freilos und spielt in der betreffenden Runde nicht.</li>
 *   <li><b>Gerade Teamanzahl</b> (K.o.-Runde): Voraussetzung für symmetrische Paarungen;
 *       ungerade Anzahl wird als Fehler behandelt.</li>
 * </ul>
 * Die Prüfung erfolgt per Bit-Masking ({@code n & 1}), was semantisch klarer ist als
 * {@code n % 2} und von JIT-Compilern identisch optimiert wird.
 *
 * @author Michael Massee
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
