/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import org.apache.commons.lang3.StringUtils;

/**
 * Eine Zeile des Mêlée-Anmeldung-Sheets.
 *
 * @param zeile         0-basierter Zeilenindex im Sheet (für das Zurückschreiben der Markierung)
 * @param nr            laufende Nummer der Anmeldung
 * @param vorname       Vorname des Spielers
 * @param nachname      Nachname des Spielers
 * @param setzPosition  Setzposition (SP); 0 = kein Setzstatus
 * @param eingecheckt   Spieler ist vor Ort erschienen
 * @param uebernommen   Spieler wurde bereits in ein Team der Meldeliste übernommen
 */
public record MeleeAnmeldungZeile(int zeile, int nr, String vorname, String nachname, int setzPosition,
		boolean eingecheckt, boolean uebernommen) {

	/**
	 * @return {@code true} wenn die Zeile überhaupt einen Namen enthält
	 */
	public boolean hatNamen() {
		return StringUtils.isNotBlank(vorname) || StringUtils.isNotBlank(nachname);
	}

	/**
	 * @return {@code true} wenn die Anmeldung noch nicht übernommen wurde
	 */
	public boolean istOffen() {
		return !uebernommen;
	}

	/**
	 * @return Anzeigename „Vorname Nachname" (ohne führende/folgende Leerzeichen)
	 */
	public String anzeigeName() {
		return StringUtils.normalizeSpace(StringUtils.trimToEmpty(vorname) + " " + StringUtils.trimToEmpty(nachname));
	}

	/**
	 * @return Sortierschlüssel: Nachname, ersatzweise Vorname
	 */
	public String sortName() {
		return StringUtils.isNotBlank(nachname) ? nachname.trim() : StringUtils.trimToEmpty(vorname);
	}
}
