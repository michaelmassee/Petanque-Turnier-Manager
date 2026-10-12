/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.helper.sheet;

import java.util.List;
import java.util.UUID;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.NrZellFormel;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.position.RangePosition;

/**
 * Schreibt Meldelisten-Nummern in Tests so, wie PTM sie vergibt: als Nr-Zelle mit UUID. Reine Zahlen in der
 * Nr-Spalte gelten als altes Dokument, jeder Lauf bricht dann ab ({@code AltesDokumentErkennung}).
 */
public final class NrZellenTestHilfe {

	private NrZellenTestHilfe() {
	}

	/** Schreibt {@code nummern} untereinander ab {@code ersteZeile} (0-basiert) in {@code nrSpalte}. */
	public static void schreibeNummern(WorkingSpreadsheet ws, XSpreadsheet sheet, int nrSpalte, int ersteZeile,
			List<Integer> nummern) {
		String[][] formeln = nummern.stream()
				.map(nr -> new String[] { NrZellFormel.team(UUID.randomUUID().toString()).formel(nr) })
				.toArray(String[][]::new);
		new SheetHelper(ws).setFormulaArrayInRange(sheet,
				RangePosition.from(nrSpalte, ersteZeile, nrSpalte, ersteZeile + formeln.length - 1), formeln);
	}
}
