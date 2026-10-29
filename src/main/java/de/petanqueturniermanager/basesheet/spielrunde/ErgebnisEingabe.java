/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.spielrunde;

import de.petanqueturniermanager.helper.sheet.rangedata.CellData;

/**
 * Prüft, ob das Ergebnis einer Paarung vollständig eingetragen ist – Voraussetzung, bevor die nächste Runde
 * ausgelost wird.
 */
public final class ErgebnisEingabe {

	private static final int KEINE_ZAHL = -1;

	private ErgebnisEingabe() {
	}

	/**
	 * Eingetragen ist ein Ergebnis erst, wenn in beiden Zellen eine Zahl steht. Eine leere Zelle zählt nicht als
	 * 0 – sonst ginge „13 : leer“ als 13:0 durch. 0:0 gilt weiterhin als nicht eingetragen.
	 *
	 * @param ergA aus {@code getDataArray} gelesene Zelle von Team A
	 * @param ergB aus {@code getDataArray} gelesene Zelle von Team B
	 */
	public static boolean istEingetragen(CellData ergA, CellData ergB) {
		int punkteA = ergA.getIntVal(KEINE_ZAHL);
		int punkteB = ergB.getIntVal(KEINE_ZAHL);
		return punkteA >= 0 && punkteB >= 0 && punkteA + punkteB > 0;
	}
}
