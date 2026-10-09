/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.star.sheet.XSpreadsheet;
import com.sun.star.sheet.XSpreadsheetDocument;

import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.rangedata.RowData;

/**
 * Baut die erwartete Spielernamen-Anzeige je Teamnummer direkt aus den Meldelisten-Zellen –
 * bewusst unabhängig von {@code TeamAnzeigeFormatierer}, damit UITests eine verschobene
 * Spaltenlage in den Anzeige-Formeln erkennen.
 * <p>
 * Erwartetes Meldelisten-Layout mit aktiver Teamname-Spalte: Spalte A Nr, Spalte B Teamname,
 * danach je Spieler Vorname, Nachname und optional Verein.
 */
public final class ErwarteteSpielernamen {

	private static final int NR_SPALTE = 0;
	private static final int ERSTE_SPIELER_SPALTE = 2;

	private ErwarteteSpielernamen() {
	}

	/**
	 * @return Teamnummer → „Vorname Nachname (Verein) / …“; Zeilen ohne Nummer fehlen
	 */
	public static Map<Integer, String> ausMeldeliste(XSpreadsheet meldeliste, XSpreadsheetDocument doc,
			int ersteDatenZeile, int anzZeilen, int anzSpieler, boolean mitVerein) throws GenerateException {
		int spaltenProSpieler = mitVerein ? 3 : 2;
		int letzteSpalte = ERSTE_SPIELER_SPALTE + anzSpieler * spaltenProSpieler - 1;
		var zeilen = RangeHelper.from(meldeliste, doc,
				RangePosition.from(NR_SPALTE, ersteDatenZeile, letzteSpalte, ersteDatenZeile + anzZeilen - 1))
				.getDataFromRange();

		Map<Integer, String> anzeigeJeNr = new HashMap<>();
		for (RowData zeile : zeilen) {
			int nr = zeile.get(NR_SPALTE).getIntVal(0);
			if (nr > 0) {
				anzeigeJeNr.put(nr, spielernamen(zeile, anzSpieler, spaltenProSpieler, mitVerein));
			}
		}
		return anzeigeJeNr;
	}

	private static String spielernamen(RowData zeile, int anzSpieler, int spaltenProSpieler, boolean mitVerein) {
		List<String> spieler = new ArrayList<>();
		for (int idx = 0; idx < anzSpieler; idx++) {
			int spalte = ERSTE_SPIELER_SPALTE + idx * spaltenProSpieler;
			String name = zeile.get(spalte).getStringVal() + " " + zeile.get(spalte + 1).getStringVal();
			spieler.add(mitVerein ? name + " (" + zeile.get(spalte + 2).getStringVal() + ")" : name);
		}
		return String.join(" / ", spieler);
	}
}
