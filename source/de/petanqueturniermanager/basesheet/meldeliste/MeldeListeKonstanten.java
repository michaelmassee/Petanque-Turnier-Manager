/**
 * Erstellung 03.11.2019 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

/**
 * @author Michael Massee
 *
 */
public interface MeldeListeKonstanten {

	String SHEETNAME = "Meldeliste";
	String SHEET_COLOR = "2544dd";

	int ERSTE_DATEN_ZEILE = 2; // Zeile 3
	int SPIELER_NR_SPALTE = 0; // Spalte A=0
	int ERSTE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 2; // Zeile 1
	int ZWEITE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 1; // Zeile 2

}
