package de.petanqueturniermanager.supermelee.spielrunde;

import de.petanqueturniermanager.helper.i18n.SheetNamen;

/** Gemeinsame Konstanten für Supermelee-Spielrunden-Sheets. */
public interface SpielrundeSheetKonstanten {

	/**
	 * Gibt den lokalisierten Tabellennamen für eine Supermelee-Spielrunde zurück.
	 *
	 * @param spieltagNr Spieltagnummer
	 * @param rundeNr    Rundennummer
	 * @return lokalisierter Tabellenname
	 */
	static String sheetName(int spieltagNr, int rundeNr) {
		return SheetNamen.supermeleeSpielrunde(spieltagNr, rundeNr);
	}

	int ERSTE_DATEN_ZEILE = 2;
	int ERSTE_HEADER_ZEILE = ERSTE_DATEN_ZEILE - 2;
	int ZWEITE_HEADER_ZEILE = ERSTE_HEADER_ZEILE + 1;

	int ERSTE_SPALTE_RUNDESPIELPLAN = 1; // spalte B
	int NUMMER_SPALTE_RUNDESPIELPLAN = ERSTE_SPALTE_RUNDESPIELPLAN - 1; // spalte A
	int ERSTE_SPALTE_ERGEBNISSE = ERSTE_SPALTE_RUNDESPIELPLAN + 6;
	int EINGABE_VALIDIERUNG_SPALTE = ERSTE_SPALTE_ERGEBNISSE + 2;
	int ERSTE_SPIELERNR_SPALTE = 11; // spalte L + 5 Spalten
	int PAARUNG_CNTR_SPALTE = ERSTE_SPIELERNR_SPALTE - 1;
	int ERSTE_SPALTE_VERTIKALE_ERGEBNISSE = ERSTE_SPIELERNR_SPALTE + 7;
	int SPALTE_VERTIKALE_ERGEBNISSE_PLUS = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 1;
	int SPALTE_VERTIKALE_ERGEBNISSE_MINUS = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 2;
	int SPALTE_VERTIKALE_ERGEBNISSE_AB = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 3;
	int SPALTE_VERTIKALE_ERGEBNISSE_BA_NR = ERSTE_SPALTE_VERTIKALE_ERGEBNISSE + 4;

	int LETZTE_SPALTE = ERSTE_SPIELERNR_SPALTE + 5;
}
