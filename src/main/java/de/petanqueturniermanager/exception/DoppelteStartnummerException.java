/*
 * Erstellung : 28.07.2026 / Michael Massee
 **/

package de.petanqueturniermanager.exception;

/**
 * signalisiert eine doppelt vergebene Startnummer in der Meldeliste, damit der
 * Aufrufer den Anwender fragen kann, ob nur die betroffenen Meldungen automatisch
 * neu nummeriert werden sollen.
 */
@SuppressWarnings("serial")
public class DoppelteStartnummerException extends GenerateException {

	private final int startnummer;

	public DoppelteStartnummerException(int startnummer, String msg) {
		super(msg);
		this.startnummer = startnummer;
	}

	public int getStartnummer() {
		return startnummer;
	}
}
