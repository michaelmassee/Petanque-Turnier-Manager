/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * UNO-freie Prüfung auf doppelte Spielernamen vor der Mêlée-Übernahme. Namen werden über
 * {@link MeleeNeuAuslosungPlan#namensSchluessel(String, String)} verglichen (ohne Leerzeichen am Rand, ohne
 * Groß-/Kleinschreibung).
 */
public final class MeleeDoppelteNamen {

	private MeleeDoppelteNamen() {
	}

	/**
	 * Ergebnis der Prüfung.
	 *
	 * @param doppelteMeleeZeilen     Zeilen des Mêlée-Blatts, deren Name dort mehrfach eingecheckt ist
	 * @param meldelisteKonflikte     Zeilen des Mêlée-Blatts mit einem neuen Spieler, der schon in der
	 *                                Meldeliste steht (dort nicht aus der Mêlée-Anmeldung)
	 * @param meldelisteSchluessel    Namensschlüssel dieser Konflikte, um sie in der Meldeliste zu markieren
	 */
	public record Ergebnis(Set<Integer> doppelteMeleeZeilen, Set<Integer> meldelisteKonflikte,
			Set<String> meldelisteSchluessel) {

		public boolean istFehlerfrei() {
			return doppelteMeleeZeilen.isEmpty() && meldelisteKonflikte.isEmpty();
		}
	}

	/**
	 * @param zeilen              alle Zeilen des Mêlée-Blatts
	 * @param namenInMeldeliste   Namensschlüssel aller Spieler der Meldeliste
	 * @return doppelte eingecheckte Namen im Mêlée-Blatt und offene eingecheckte Spieler, die bereits als
	 *         von Hand gemeldeter Spieler in der Meldeliste stehen
	 */
	public static Ergebnis pruefe(List<MeleeAnmeldungZeile> zeilen, Set<String> namenInMeldeliste) {
		checkNotNull(zeilen);
		checkNotNull(namenInMeldeliste);
		Map<String, Set<Integer>> zeilenJeName = new HashMap<>();
		zeilen.stream().filter(MeleeAnmeldungZeile::eingecheckt).filter(MeleeAnmeldungZeile::hatNamen)
				.forEach(z -> zeilenJeName.computeIfAbsent(schluessel(z), k -> new TreeSet<>()).add(z.zeile()));
		Set<Integer> doppelte = new TreeSet<>();
		zeilenJeName.values().stream().filter(gleicheZeilen -> gleicheZeilen.size() > 1).forEach(doppelte::addAll);

		// Übernommene Mêlée-Spieler stehen selbst in der Meldeliste – nur fremde Meldungen sind Konflikte.
		Set<String> uebernommene = new TreeSet<>();
		zeilen.stream().filter(MeleeAnmeldungZeile::uebernommen).map(MeleeDoppelteNamen::schluessel)
				.forEach(uebernommene::add);
		Set<Integer> konflikte = new TreeSet<>();
		Set<String> konfliktSchluessel = new TreeSet<>();
		zeilen.stream().filter(MeleeAnmeldungZeile::istOffen).filter(MeleeAnmeldungZeile::eingecheckt)
				.filter(MeleeAnmeldungZeile::hatNamen)
				.filter(z -> namenInMeldeliste.contains(schluessel(z)) && !uebernommene.contains(schluessel(z)))
				.forEach(z -> {
					konflikte.add(z.zeile());
					konfliktSchluessel.add(schluessel(z));
				});
		return new Ergebnis(doppelte, konflikte, konfliktSchluessel);
	}

	private static String schluessel(MeleeAnmeldungZeile zeile) {
		return MeleeNeuAuslosungPlan.namensSchluessel(zeile.vorname(), zeile.nachname());
	}
}
