/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.comp.kompatibilitaet;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.basesheet.meldeliste.MeldelisteUuidIndex;
import de.petanqueturniermanager.basesheet.meldeliste.TeamUuidFormel;
import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.helper.i18n.SheetNamen;
import de.petanqueturniermanager.helper.sheet.SheetHelper;

/**
 * 5.40.00: Teams werden über eine UUID in der Nr-Zelle identifiziert statt über die Teamnummer. Ältere Dokumente
 * werden bewusst nicht umgestellt – ihre Spielrunden verweisen über Nummern auf Teams, eine Umstellung könnte sie
 * still auf falsche Teams zeigen lassen.
 */
final class TeamUuidUmstellung implements DokumentMigration {

	@Override
	public String abVersion() {
		return "5.40.00";
	}

	@Override
	public boolean istMigrierbar() {
		return false;
	}

	/** Ohne Version: nummerierte Meldungen, aber keine UUID in der Nr-Spalte. */
	@Override
	public boolean istNoetigOhneVersion(WorkingSpreadsheet ws) {
		SheetHelper helper = new SheetHelper(ws);
		XSpreadsheet meldeliste = helper.findByName(SheetNamen.meldeliste());
		if (meldeliste == null) {
			return false;
		}
		MeldelisteUuidIndex.NrSpalte spalte = MeldelisteUuidIndex.leseNrSpalte(helper, meldeliste,
				ws.getWorkingSpreadsheetDocument());
		return istAltesInhaltsformat(spalte.formeln(), spalte.nummern());
	}

	/**
	 * UNO-frei.
	 *
	 * @param formeln Formeln der Nr-Spalte, Index = Zeile
	 * @param nummern angezeigte Nummern derselben Zeilen, 0 für leer oder Text
	 * @return {@code true}, wenn es nummerierte Zeilen gibt, aber keine Zeile eine UUID trägt
	 */
	static boolean istAltesInhaltsformat(String[][] formeln, int[] nummern) {
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
