/*
 * Erstellung 2026 / Michael Massee
 */
package de.petanqueturniermanager.siegergeld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.sun.star.sheet.XSpreadsheet;

import de.petanqueturniermanager.comp.WorkingSpreadsheet;
import de.petanqueturniermanager.exception.GenerateException;
import de.petanqueturniermanager.helper.position.Position;
import de.petanqueturniermanager.helper.sheet.SheetHelper;
import de.petanqueturniermanager.helper.sheet.SheetMetadataHelper;

final class RanglistenSiegergeldQuelle implements SiegergeldQuelle {

	private static final int MAX_ZEILEN = 500;

	private final WorkingSpreadsheet workingSpreadsheet;
	private final String metadatenSchluessel;
	private final String gruppe;
	private final int ersteDatenZeile;
	private final int nrSpalte;
	private final int nameSpalte;
	private final int platzSpalte;
	private final String fallbackSheetName;
	private final TeilnehmerAnzahlQuelle teilnehmerAnzahlQuelle;

	RanglistenSiegergeldQuelle(WorkingSpreadsheet workingSpreadsheet, String metadatenSchluessel,
			String gruppe, int ersteDatenZeile, int nrSpalte, int nameSpalte, int platzSpalte,
			String fallbackSheetName, TeilnehmerAnzahlQuelle teilnehmerAnzahlQuelle) {
		this.workingSpreadsheet = workingSpreadsheet;
		this.metadatenSchluessel = metadatenSchluessel;
		this.gruppe = gruppe;
		this.ersteDatenZeile = ersteDatenZeile;
		this.nrSpalte = nrSpalte;
		this.nameSpalte = nameSpalte;
		this.platzSpalte = platzSpalte;
		this.fallbackSheetName = fallbackSheetName;
		this.teilnehmerAnzahlQuelle = teilnehmerAnzahlQuelle;
	}

	@Override
	public List<SiegergeldEintrag> leseTop3() throws GenerateException {
		XSpreadsheet sheet = findeSheet();
		if (sheet == null) {
			return List.of();
		}

		SheetHelper sheetHelper = new SheetHelper(workingSpreadsheet);
		List<SiegergeldEintrag> eintraege = new ArrayList<>();
		for (int zeile = ersteDatenZeile; zeile < ersteDatenZeile + MAX_ZEILEN; zeile++) {
			String name = sheetHelper.getTextFromCell(sheet, Position.from(nameSpalte, zeile));
			if (name == null || name.isBlank()) {
				continue;
			}
			int platz = sheetHelper.getIntFromCell(sheet, Position.from(platzSpalte, zeile));
			if (platz < 1 || platz > 3) {
				continue;
			}
			int nr = sheetHelper.getIntFromCell(sheet, Position.from(nrSpalte, zeile));
			eintraege.add(new SiegergeldEintrag(gruppe, platz, nr, name));
		}
		eintraege.sort(Comparator.comparing(SiegergeldEintrag::gruppe).thenComparingInt(SiegergeldEintrag::platz));
		return eintraege;
	}

	@Override
	public int teilnehmerAnzahl() throws GenerateException {
		if (teilnehmerAnzahlQuelle != null) {
			int anzahl = teilnehmerAnzahlQuelle.lese();
			if (anzahl > 0) {
				return anzahl;
			}
		}
		XSpreadsheet sheet = findeSheet();
		if (sheet == null) {
			return 0;
		}

		SheetHelper sheetHelper = new SheetHelper(workingSpreadsheet);
		int anzahl = 0;
		for (int zeile = ersteDatenZeile; zeile < ersteDatenZeile + MAX_ZEILEN; zeile++) {
			String name = sheetHelper.getTextFromCell(sheet, Position.from(nameSpalte, zeile));
			if (name != null && !name.isBlank()) {
				anzahl++;
			}
		}
		return anzahl;
	}

	private XSpreadsheet findeSheet() throws GenerateException {
		XSpreadsheet sheet = SheetMetadataHelper.findeSheetUndHeile(
				workingSpreadsheet.getWorkingSpreadsheetDocument(), metadatenSchluessel, fallbackSheetName);
		return sheet;
	}

	@FunctionalInterface
	interface TeilnehmerAnzahlQuelle {
		int lese() throws GenerateException;
	}
}
