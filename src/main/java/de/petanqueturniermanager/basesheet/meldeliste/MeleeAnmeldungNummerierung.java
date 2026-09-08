/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.position.RangePosition;
import de.petanqueturniermanager.helper.sheet.RangeHelper;
import de.petanqueturniermanager.helper.sheet.rangedata.RangeData;

/**
 * Vergibt fortlaufende Nummern (Spalte {@code Nr}) für alle Zeilen mit Namen im
 * Mêlée-Anmeldung-Sheet.
 * <p>
 * Eigenständiger Helper statt Methode auf {@link AbstractMeleeAnmeldungSheet}, damit auch
 * {@link AbstractMeleeAnmeldungUebernehmenSheet} neu hinzugefügte, noch nicht durchnummerierte
 * Zeilen (z.B. per Copy-Paste ohne vorherigen Aufruf des Menüpunkts „Mêlée Anmeldung") vor dem
 * Übernehmen ergänzen kann.
 */
public final class MeleeAnmeldungNummerierung implements MeleeAnmeldungKonstanten {

	private MeleeAnmeldungNummerierung() {
	}

	/**
	 * Vergibt fortlaufende Nummern (1..n) für alle Zeilen mit Namen (blockweise geschrieben).
	 * Zeilen ohne Namen bleiben unberührt, damit die Nummerierung dem Anwender nicht in leere
	 * Zeilen „vorläuft". Bereits vergebene Nummern werden überschrieben, sodass Lücken (z.B. durch
	 * nachträglich per Copy-Paste eingefügte Zeilen ohne Nummer) geschlossen werden.
	 */
	public static void nummerieren(WorkingSpreadsheet ws, XSpreadsheet sheet) throws GenerateException {
		List<MeleeAnmeldungZeile> zeilen = MeleeAnmeldungLeser.lesen(ws, sheet);
		if (zeilen.isEmpty()) {
			return;
		}
		RangeData data = new RangeData();
		for (int idx = 0; idx < zeilen.size(); idx++) {
			data.addNewRow().newInt(idx + 1);
		}
		RangePosition bereich = data.getRangePosition(Position.from(SPALTE_NR, ERSTE_DATEN_ZEILE));
		RangeHelper.from(sheet, ws.getWorkingSpreadsheetDocument(), bereich).setDataInRange(data);
	}
}
