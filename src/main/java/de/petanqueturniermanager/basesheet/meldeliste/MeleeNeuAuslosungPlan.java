/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

/**
 * UNO-freie Planung für das Neu-Auslosen der Mêlée-Teams vor der ersten Spielrunde.
 * <p>
 * In den Modi „ohne Freilos" lassen sich einzelne Nachzügler nicht zu eigenen Teams formen, ohne
 * eine ungerade Teamzahl zu erzeugen. Dann werden die bisher übernommenen Mêlée-Teams zusammen mit
 * den Nachzüglern komplett neu ausgelost. Diese Klasse findet die Meldelisten-Zeilen der bisherigen
 * Mêlée-Teams und ordnet die neu ausgelosten Teams Zeilen zu.
 */
public final class MeleeNeuAuslosungPlan {

	private MeleeNeuAuslosungPlan() {
	}

	/**
	 * Eine Zielzeile der Meldeliste: entweder mit einem neuen Team belegt oder zu leeren.
	 *
	 * @param zeile     0-basierte Zeile der Meldeliste
	 * @param teamIndex Index des neuen Teams, {@link #LEEREN} wenn die Zeile geleert wird
	 */
	public record Belegung(int zeile, int teamIndex) {

		/** Die Zeile verliert ihr Team; die Meldeliste entfernt Zeilen ohne Namen beim Aktualisieren. */
		public static final int LEEREN = -1;

		public boolean istLeeren() {
			return teamIndex == LEEREN;
		}
	}

	/**
	 * Zusammenhängender Zeilenbereich, der am Stück geschrieben wird.
	 *
	 * @param von erste Zeile (inklusive)
	 * @param bis letzte Zeile (inklusive)
	 */
	public record Bereich(int von, int bis) {
	}

	/**
	 * @return Vergleichsschlüssel eines Spielers – getrimmt und ohne Unterscheidung der Groß-/Kleinschreibung,
	 *         so wie die Meldeliste die Namen beim Aktualisieren trimmt
	 */
	public static String namensSchluessel(String vorname, String nachname) {
		return StringUtils.trimToEmpty(vorname).toLowerCase(Locale.ROOT) + '\u0000'
				+ StringUtils.trimToEmpty(nachname).toLowerCase(Locale.ROOT);
	}

	/**
	 * @param spielerJeZeile Namensschlüssel der Spieler je Meldelisten-Zeile (leere Spielerplätze fehlen)
	 * @param uebernommene   Namensschlüssel aller als „Übernommen" markierten Mêlée-Anmeldungen
	 * @return Indizes der Zeilen, deren Spieler alle aus der Mêlée-Anmeldung stammen – also die bisherigen
	 *         Mêlée-Teams. Von Hand gemeldete Teams und Zeilen ohne Spieler zählen nicht.
	 */
	public static List<Integer> meleeTeamZeilen(List<Set<String>> spielerJeZeile, Set<String> uebernommene) {
		checkNotNull(spielerJeZeile);
		checkNotNull(uebernommene);
		List<Integer> zeilen = new ArrayList<>();
		for (int index = 0; index < spielerJeZeile.size(); index++) {
			Set<String> spieler = spielerJeZeile.get(index);
			if (!spieler.isEmpty() && uebernommene.containsAll(spieler)) {
				zeilen.add(index);
			}
		}
		return zeilen;
	}

	/**
	 * Ordnet die neu ausgelosten Teams Zeilen zu: zuerst die Zeilen der bisherigen Mêlée-Teams in ihrer
	 * Reihenfolge, weitere Teams ab {@code ersteFreieZeile}. Überzählige bisherige Zeilen werden geleert.
	 *
	 * @param bisherigeZeilen aufsteigend sortierte Zeilen der bisherigen Mêlée-Teams
	 * @param anzTeams        Anzahl neu ausgeloster Teams
	 * @param ersteFreieZeile erste freie Zeile hinter allen Meldungen
	 * @return Belegungen aufsteigend nach Zeile
	 */
	public static List<Belegung> zeilenZuordnung(List<Integer> bisherigeZeilen, int anzTeams, int ersteFreieZeile) {
		checkNotNull(bisherigeZeilen);
		checkArgument(anzTeams >= 0, "anzTeams darf nicht negativ sein");
		List<Belegung> belegungen = new ArrayList<>();
		for (int index = 0; index < bisherigeZeilen.size(); index++) {
			belegungen.add(new Belegung(bisherigeZeilen.get(index), index < anzTeams ? index : Belegung.LEEREN));
		}
		for (int teamIndex = bisherigeZeilen.size(); teamIndex < anzTeams; teamIndex++) {
			belegungen.add(new Belegung(ersteFreieZeile + teamIndex - bisherigeZeilen.size(), teamIndex));
		}
		return belegungen;
	}

	/**
	 * @param belegungen Belegungen aufsteigend nach Zeile
	 * @return zusammenhängende Zeilenbereiche, damit jeder Bereich als Block geschrieben werden kann
	 */
	public static List<Bereich> zusammenhaengendeBereiche(List<Belegung> belegungen) {
		checkNotNull(belegungen);
		List<Bereich> bereiche = new ArrayList<>();
		int index = 0;
		while (index < belegungen.size()) {
			int von = belegungen.get(index).zeile();
			int bis = von;
			while (index + 1 < belegungen.size() && belegungen.get(index + 1).zeile() == bis + 1) {
				index++;
				bis++;
			}
			bereiche.add(new Bereich(von, bis));
			index++;
		}
		return bereiche;
	}
}
