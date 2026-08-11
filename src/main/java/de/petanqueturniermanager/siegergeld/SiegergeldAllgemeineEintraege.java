/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.IntStream;

final class SiegergeldAllgemeineEintraege {

	private SiegergeldAllgemeineEintraege() {
	}

	static List<SiegergeldEintrag> einzelgruppe(int plaetze) {
		return gruppen(List.of("A"), plaetze);
	}

	static List<SiegergeldEintrag> gruppen(int gruppenAnzahl, int plaetze) {
		List<String> gruppen = IntStream.range(0, gruppenAnzahl)
				.mapToObj(SiegergeldAllgemeineEintraege::gruppenBuchstabe)
				.toList();
		return gruppen(gruppen, plaetze);
	}

	static List<SiegergeldEintrag> gruppen(Collection<String> gruppen, int plaetze) {
		List<SiegergeldEintrag> eintraege = new ArrayList<>();
		for (String gruppe : gruppen) {
			String gruppenName = gruppe == null || gruppe.isBlank() ? "A" : gruppe.trim();
			for (int platz = 1; platz <= plaetze; platz++) {
				eintraege.add(new SiegergeldEintrag(gruppenName, platz, 0, ""));
			}
		}
		return eintraege.isEmpty() ? einzelgruppe(plaetze) : eintraege;
	}

	static String gruppenBuchstabe(int index) {
		return String.valueOf((char) ('A' + index));
	}
}
