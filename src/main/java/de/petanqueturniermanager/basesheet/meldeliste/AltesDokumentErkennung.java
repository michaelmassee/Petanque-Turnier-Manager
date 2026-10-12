/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.basesheet.meldeliste;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.AltesDokumentException;
import de.petanqueturniermanager.helper.i18n.I18n;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * Erkennt Turnier-Dokumente aus der Zeit, als die Teamnummer selbst die Identität war: Die Meldeliste hat
 * nummerierte Meldungen, aber keine einzige Nr-Zelle trägt eine UUID ({@code PTM.ALG.TNRUUID}). Solche Dokumente
 * werden nicht umgestellt (umbau-nr-uuid.md, „Alte Dokumente“).
 * <p>
 * Ein neues Dokument trägt nach der ersten Aktualisierung in jeder nummerierten Zeile eine UUID; eine einzelne von
 * Hand getippte Nummer daneben macht es nicht alt.
 */
public final class AltesDokumentErkennung {

	private AltesDokumentErkennung() {
	}

	/**
	 * @throws AltesDokumentException wenn die Meldeliste des Dokuments im alten Format vorliegt
	 */
	public static void pruefe(WorkingSpreadsheet ws) throws AltesDokumentException {
		SheetHelper helper = new SheetHelper(ws);
		XSpreadsheet meldeliste = helper.findByName(SheetNamen.meldeliste());
		if (meldeliste == null) {
			return;
		}
		MeldelisteUuidIndex.NrSpalte spalte = MeldelisteUuidIndex.leseNrSpalte(helper, meldeliste,
				ws.getWorkingSpreadsheetDocument());
		if (istAlt(spalte.formeln(), spalte.nummern())) {
			throw new AltesDokumentException(I18n.get("dokument.alt.urspruengliches.plugin"));
		}
	}

	/**
	 * UNO-frei.
	 *
	 * @param formeln Formeln der Nr-Spalte, Index = Zeile
	 * @param nummern angezeigte Nummern derselben Zeilen, 0 für leer oder Text
	 * @return {@code true}, wenn es nummerierte Zeilen gibt, aber keine Zeile eine UUID trägt
	 */
	static boolean istAlt(String[][] formeln, int[] nummern) {
		boolean nummeriert = false;
		for (int zeile = 0; zeile < formeln.length; zeile++) {
			if (formeln[zeile].length > 0 && TeamUuidFormel.findeUuid(formeln[zeile][0]).isPresent()) {
				return false;
			}
			nummeriert |= zeile < nummern.length && nummern[zeile] > 0;
		}
		return nummeriert;
	}
}
