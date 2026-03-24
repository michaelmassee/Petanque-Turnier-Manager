/**
 * Erstellung 03.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import de.petanqueturniermanager.helper.i18n.SheetNamen;

/**
 * Gemeinsame Konstanten für alle Meldelisten-Sheets.
 */
public interface MeldeListeKonstanten {

	String SHEET_COLOR = "2544dd";

	/** Gibt den lokalisierten Tabellennamen zurück. */
	static String sheetName() {
		return SheetNamen.meldeliste();
	}

	int CELL_MARGIN = 100;

	int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	int SPIELER_NR_SPALTE = 0; // Spalte A=0
	int ERSTE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 2; // Zeile 1
	int ZWEITE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 1; // Zeile 2

}
