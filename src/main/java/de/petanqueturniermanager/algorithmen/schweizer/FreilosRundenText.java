package de.petanqueturniermanager.algorithmen.schweizer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Formatiert die Freilos-Runden eines Teams für die Freilos-Spalte der Rangliste.
 * <p>
 * Beispiele: keine Runde → leerer Text, eine Runde → „2“, mehrere Runden → „1, 3, 6“.
 */
public final class FreilosRundenText {

	private static final String TRENNER = ", ";

	private FreilosRundenText() {
		// Utility-Klasse – keine Instanzen
	}

	/**
	 * @param freilosInRunden Rundennummern in der gewünschten Anzeigereihenfolge
	 * @return die mit „, “ verbundenen Rundennummern, leerer Text wenn keine Runde vorhanden ist
	 */
	public static String alsText(List<Integer> freilosInRunden) {
		return freilosInRunden.stream().map(String::valueOf).collect(Collectors.joining(TRENNER));
	}
}
