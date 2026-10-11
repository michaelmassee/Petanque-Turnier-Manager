/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.schweizer.meldeliste;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Zuordnung sichtbarer Teamkennungen (Teamname bzw. zusammengesetzte Spielernamen) zur
 * Teamnummer. Wird einmal aus der Meldeliste aufgebaut, damit die Auswertung einer
 * Spielrunde nicht je Paarung die Meldeliste erneut lesen muss.
 * <p>
 * Kennungen, die mehreren Teams gehören, sind keine sichere Identität und werden als
 * mehrdeutig markiert statt einem der Teams zugeordnet.
 */
public final class TeamAnzeigeIndex {

	private final Map<String, Integer> nrNachAnzeige = new HashMap<>();
	private final Set<String> mehrdeutig = new HashSet<>();

	/** Ordnet eine sichtbare Kennung einem Team zu. Leere Kennungen werden ignoriert. */
	public void hinzufuegen(String anzeige, int teamNr) {
		if (anzeige == null || anzeige.isBlank() || teamNr <= 0 || mehrdeutig.contains(anzeige)) {
			return;
		}
		Integer bisher = nrNachAnzeige.putIfAbsent(anzeige, teamNr);
		if (bisher != null && bisher != teamNr) {
			nrNachAnzeige.remove(anzeige);
			mehrdeutig.add(anzeige);
		}
	}

	public boolean istMehrdeutig(String anzeige) {
		return mehrdeutig.contains(anzeige);
	}

	/** @return Teamnummer oder 0, wenn die Kennung unbekannt oder mehrdeutig ist */
	public int teamNr(String anzeige) {
		return nrNachAnzeige.getOrDefault(anzeige, 0);
	}
}
