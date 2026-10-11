/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;

import de.petanqueturniermanager.addins.GlobalImpl;

/**
 * Plant „Teamnummer ändern“ auf der Nr-Spalte der Meldeliste: Nur die angezeigte Zahl in
 * {@code PTM.ALG.TNRUUID(nr;"uuid")} ändert sich, die UUID bleibt. Spielrunden, die das Team über seine UUID
 * referenzieren, zeigen danach die neue Nummer. Ist die neue Nummer schon vergeben, tauschen beide Meldungen ihre
 * Nummern.
 * <p>
 * UNO-frei: arbeitet auf Formeln und angezeigten Nummern der Spalte, wie {@link MeldelisteUuidIndex#aus}. Der
 * Datenbereich beginnt mit der ersten Zeile, die eine UUID trägt; Zahlen in Kopfzeilen davor zählen nicht.
 */
public final class TeamnummerAenderung {

	/** Warum eine Änderung nicht möglich ist. */
	public enum Ablehnung {
		/** Neue Nummer nicht größer 0. */
		NEUE_NR_UNGUELTIG,
		/** Alte und neue Nummer sind gleich. */
		GLEICHE_NR,
		/** Keine Meldung mit der alten Nummer. */
		ALTE_NR_FEHLT,
		/** Eine beteiligte Meldung hat noch keine UUID (Meldeliste vorher aktualisieren). */
		OHNE_UUID
	}

	/**
	 * @param formelProZeile neue Formel je 0-basierter Zeile im gelesenen Bereich
	 * @param getauscht      {@code true}, wenn die neue Nummer vergeben war und beide Meldungen getauscht haben
	 */
	public record Plan(SortedMap<Integer, String> formelProZeile, boolean getauscht) {
	}

	/** Entweder ein {@link Plan} oder eine {@link Ablehnung}. */
	public record Ergebnis(Optional<Plan> plan, Optional<Ablehnung> ablehnung) {

		static Ergebnis von(Plan plan) {
			return new Ergebnis(Optional.of(plan), Optional.empty());
		}

		static Ergebnis abgelehnt(Ablehnung ablehnung) {
			return new Ergebnis(Optional.empty(), Optional.of(ablehnung));
		}
	}

	private TeamnummerAenderung() {
	}

	/**
	 * @param formeln Formel-Array der Nr-Spalte (eine Spalte)
	 * @param nummern angezeigte Nummern derselben Zeilen, 0 für leer
	 */
	public static Ergebnis plane(String[][] formeln, int[] nummern, int alteNr, int neueNr) {
		if (neueNr <= 0) {
			return Ergebnis.abgelehnt(Ablehnung.NEUE_NR_UNGUELTIG);
		}
		if (alteNr == neueNr) {
			return Ergebnis.abgelehnt(Ablehnung.GLEICHE_NR);
		}
		int ersteDatenZeile = ersteZeileMitUuid(formeln);
		int zeileAlt = zeileMitNr(nummern, alteNr, ersteDatenZeile);
		if (zeileAlt < 0) {
			return Ergebnis.abgelehnt(Ablehnung.ALTE_NR_FEHLT);
		}
		Optional<String> uuidAlt = uuid(formeln, zeileAlt);
		int zeileNeu = zeileMitNr(nummern, neueNr, ersteDatenZeile);
		Optional<String> uuidNeu = zeileNeu < 0 ? Optional.empty() : uuid(formeln, zeileNeu);
		if (uuidAlt.isEmpty() || zeileNeu >= 0 && uuidNeu.isEmpty()) {
			return Ergebnis.abgelehnt(Ablehnung.OHNE_UUID);
		}

		SortedMap<Integer, String> formelProZeile = new TreeMap<>();
		formelProZeile.put(zeileAlt, GlobalImpl.FORMAT_PTM_TNR_UUID(neueNr, uuidAlt.get()));
		uuidNeu.ifPresent(uuid -> formelProZeile.put(zeileNeu, GlobalImpl.FORMAT_PTM_TNR_UUID(alteNr, uuid)));
		return Ergebnis.von(new Plan(formelProZeile, uuidNeu.isPresent()));
	}

	private static int ersteZeileMitUuid(String[][] formeln) {
		for (int zeile = 0; zeile < formeln.length; zeile++) {
			if (uuid(formeln, zeile).isPresent()) {
				return zeile;
			}
		}
		return formeln.length;
	}

	private static int zeileMitNr(int[] nummern, int nr, int ersteDatenZeile) {
		for (int zeile = ersteDatenZeile; zeile < nummern.length; zeile++) {
			if (nummern[zeile] == nr) {
				return zeile;
			}
		}
		return -1;
	}

	private static Optional<String> uuid(String[][] formeln, int zeile) {
		return zeile < formeln.length && formeln[zeile].length > 0 ? TeamUuidFormel.findeUuid(formeln[zeile][0])
				: Optional.empty();
	}
}
